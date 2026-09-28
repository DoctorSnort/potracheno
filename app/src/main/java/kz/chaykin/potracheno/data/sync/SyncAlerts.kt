package kz.chaykin.potracheno.data.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kz.chaykin.potracheno.MainActivity
import kz.chaykin.potracheno.R

/** Почему не удалась фоновая выгрузка — от этого зависит, стоит ли будить человека. */
enum class SyncFailureKind {
    /** Без человека не починить: Google просит согласие или отозвал доступ. */
    NEEDS_USER,

    /** Может пройти само: сеть, временный сбой Google. */
    TRANSIENT,
}

/**
 * Когда показывать уведомление о неудачной выгрузке. Держим отдельно от Android,
 * чтобы правило было видно глазами и проверялось тестом.
 */
object SyncAlertPolicy {

    /**
     * Сколько неудачных попыток подряд терпим молча при временной ошибке.
     * Бэкофф экспоненциальный от 30 минут, так что третья попытка — это уже часы без копии.
     */
    const val SILENT_ATTEMPTS = 2

    /** [attempt] — `runAttemptCount` воркера: 0 у первой попытки. */
    fun shouldNotify(kind: SyncFailureKind, attempt: Int): Boolean = when (kind) {
        SyncFailureKind.NEEDS_USER -> true
        SyncFailureKind.TRANSIENT -> attempt >= SILENT_ATTEMPTS
    }
}

/**
 * Уведомление «копия не уехала на Диск». Одно на всё приложение: новое заменяет
 * старое, а удачная выгрузка его убирает. Без разрешения на уведомления молча
 * ничего не делает — автовыгрузка от этого не зависит.
 */
class SyncNotifier(context: Context) {

    private val appContext = context.applicationContext

    fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            appContext.getString(R.string.notification_channel_backup),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = appContext.getString(R.string.notification_channel_backup_hint) }
        appContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canNotify(): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(appContext).areNotificationsEnabled()
    }

    fun showFailure(reason: String) {
        if (!canNotify()) return
        val openSettings = PendingIntent.getActivity(
            appContext,
            0,
            MainActivity.openSettingsIntent(appContext),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_mono)
            .setContentTitle(appContext.getString(R.string.notification_backup_failed_title))
            .setContentText(reason)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reason))
            .setContentIntent(openSettings)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Разрешение отозвали между проверкой и показом — не повод ронять фоновую работу.
        }
    }

    fun cancel() = NotificationManagerCompat.from(appContext).cancel(NOTIFICATION_ID)

    private companion object {
        const val CHANNEL_ID = "backup"
        const val NOTIFICATION_ID = 1001
    }
}
