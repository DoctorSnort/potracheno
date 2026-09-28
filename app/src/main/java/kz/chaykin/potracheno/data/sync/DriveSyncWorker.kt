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
        val notifier = container.syncNotifier

        // Ошибка пишется в настройки всегда, а будит человека — только когда пора (см. SyncAlertPolicy).
        suspend fun fail(reason: String, kind: SyncFailureKind): Result {
            settingsStore.setSyncFailed(reason)
            if (SyncAlertPolicy.shouldNotify(kind, runAttemptCount)) notifier.showFailure(reason)
            return if (kind == SyncFailureKind.NEEDS_USER) Result.failure() else Result.retry()
        }

        return when (val access = container.driveAuth.request()) {
            is DriveAccess.Granted -> runCatching { container.driveSync.upload(access.token) }
                .fold(
                    onSuccess = { at ->
                        settingsStore.setSyncSucceeded(at)
                        notifier.cancel()
                        Result.success()
                    },
                    onFailure = { error ->
                        fail(
                            error.readableMessage(),
                            if (error is DriveAuthExpired) SyncFailureKind.NEEDS_USER else SyncFailureKind.TRANSIENT,
                        )
                    },
                )

            is DriveAccess.NeedsConsent ->
                fail("Google просит подтвердить доступ — зайдите в настройки", SyncFailureKind.NEEDS_USER)

            is DriveAccess.Failed -> fail(access.message, SyncFailureKind.TRANSIENT)
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
