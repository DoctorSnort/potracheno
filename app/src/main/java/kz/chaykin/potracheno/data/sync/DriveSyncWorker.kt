package kz.chaykin.potracheno.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kz.chaykin.potracheno.PotrachenoApp
import java.util.concurrent.TimeUnit

/**
 * Ежесуточная выгрузка копии на Диск. Здесь нельзя ничего спрашивать у пользователя:
 * если Google решит переспросить разрешение, работа честно останавливается и пишет
 * об этом в настройках, а не крутится впустую.
 */
class DriveSyncWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as PotrachenoApp).container
        val settingsStore = container.settingsStore

        return when (val access = container.driveAuth.request()) {
            is DriveAccess.Granted -> runCatching { container.driveSync.upload(access.token) }
                .fold(
                    onSuccess = { at ->
                        settingsStore.setSyncSucceeded(at)
                        Result.success()
                    },
                    onFailure = { error ->
                        settingsStore.setSyncFailed(error.readableMessage())
                        if (error is DriveAuthExpired) Result.failure() else Result.retry()
                    },
                )

            is DriveAccess.NeedsConsent -> {
                settingsStore.setSyncFailed("Google просит подтвердить доступ — зайдите в настройки")
                Result.failure()
            }

            is DriveAccess.Failed -> {
                settingsStore.setSyncFailed(access.message)
                Result.retry()
            }
        }
    }

    private fun Throwable.readableMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: this::class.simpleName.orEmpty()

    companion object {
        private const val WORK_NAME = "drive-daily-backup"

        fun setDailyEnabled(context: Context, enabled: Boolean) {
            val manager = WorkManager.getInstance(context.applicationContext)
            if (!enabled) {
                manager.cancelUniqueWork(WORK_NAME)
                return
            }

            val request = PeriodicWorkRequestBuilder<DriveSyncWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()

            manager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
