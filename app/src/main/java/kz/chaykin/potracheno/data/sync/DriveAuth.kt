package kz.chaykin.potracheno.data.sync

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Чем закончилась попытка получить доступ к Диску. */
sealed interface DriveAccess {
    data class Granted(val token: String) : DriveAccess

    /** Пользователя нужно спросить, а экран согласия показывается только из активити. */
    data class NeedsConsent(val intent: PendingIntent) : DriveAccess

    data class Failed(val message: String) : DriveAccess
}

/**
 * Доступ к Диску через Play-сервисы. Запрашиваем единственную область `drive.file`:
 * она даёт приложению только его собственные файлы и не требует платной проверки
 * приложения в Google — остальные области Диска дают доступ ко всему чужому добру.
 */
class DriveAuth(context: Context) {

    private val appContext = context.applicationContext

    private val request: AuthorizationRequest = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
        .build()

    /**
     * Первый раз показывает согласие (через [DriveAccess.NeedsConsent]), дальше молча
     * отдаёт свежий токен: они живут около часа, поэтому запрашиваются перед каждой выгрузкой.
     */
    suspend fun request(): DriveAccess = runCatching {
        Identity.getAuthorizationClient(appContext).authorize(request).await()
    }.fold(onSuccess = { it.toAccess() }, onFailure = { DriveAccess.Failed(describe(it)) })

    /** Разбор ответа с экрана согласия. */
    fun fromConsent(data: Intent?): DriveAccess = runCatching {
        Identity.getAuthorizationClient(appContext).getAuthorizationResultFromIntent(data)
    }.fold(onSuccess = { it.toAccess() }, onFailure = { DriveAccess.Failed(describe(it)) })

    /** Выбрасывает выданный токен из кэша Play-сервисов, чтобы «Отключить» что-то значило. */
    suspend fun forget(token: String?) {
        if (token.isNullOrBlank()) return
        runCatching {
            Identity.getAuthorizationClient(appContext)
                .clearToken(ClearTokenRequest.builder().setToken(token).build())
                .await()
        }
    }

    private fun AuthorizationResult.toAccess(): DriveAccess {
        if (hasResolution()) {
            val intent = pendingIntent
            return if (intent != null) {
                DriveAccess.NeedsConsent(intent)
            } else {
                DriveAccess.Failed("Google просит подтверждение, но не сказал какое")
            }
        }
        val token = accessToken
        return if (token.isNullOrBlank()) {
            DriveAccess.Failed("Google не выдал доступ к Диску")
        } else {
            DriveAccess.Granted(token)
        }
    }

    private fun describe(error: Throwable): String {
        val api = error as? ApiException ?: return error.message ?: "Не получилось обратиться к Google"
        return when (api.statusCode) {
            CommonStatusCodes.DEVELOPER_ERROR ->
                "Приложение не зарегистрировано в Google Cloud: проверьте OAuth-клиент, " +
                    "имя пакета и отпечаток SHA-1"
            CommonStatusCodes.NETWORK_ERROR -> "Нет связи с Google"
            CommonStatusCodes.CANCELED -> "Вход отменён"
            CommonStatusCodes.SIGN_IN_REQUIRED -> "Нужно войти в Google-аккаунт на телефоне"
            else -> api.message ?: "Google ответил ошибкой ${api.statusCode}"
        }
    }

    private companion object {
        /** Своя песочница в Диске: приложение видит только то, что само туда положило. */
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
