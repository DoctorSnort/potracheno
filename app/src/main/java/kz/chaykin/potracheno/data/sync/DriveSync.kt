package kz.chaykin.potracheno.data.sync

import android.content.Context
import kz.chaykin.potracheno.data.backup.BackupManager
import kz.chaykin.potracheno.data.backup.ImportResult
import java.io.File
import java.io.IOException

/**
 * Выгрузка и восстановление через Google Диск. Формат — тот же ZIP, что и у ручного экспорта:
 * копия с телефона и копия с Диска взаимозаменяемы, и ни одна из них не привязана
 * к приложению намертво.
 */
class DriveSync(
    context: Context,
    private val backupManager: BackupManager,
    private val api: DriveApi,
) {
    private val appContext = context.applicationContext
    private val cacheDir = appContext.cacheDir

    /** Ежесуточная выгрузка: включается и выключается одним переключателем в настройках. */
    fun setDailyUpload(enabled: Boolean) = DriveSyncWorker.setDailyEnabled(appContext, enabled)

    suspend fun accountEmail(token: String): String? = api.accountEmail(token)

    /** Возвращает момент удачной выгрузки. */
    suspend fun upload(token: String): Long {
        val temp = File.createTempFile("drive-out", ".zip", cacheDir)
        try {
            backupManager.exportTo(temp)
            api.upload(token, api.findBackup(token)?.id, temp)
            return System.currentTimeMillis()
        } finally {
            temp.delete()
        }
    }

    /** Скачивает копию и полностью заменяет ею содержимое приложения. */
    suspend fun restore(token: String): ImportResult {
        val remote = api.findBackup(token)
            ?: throw IOException("На Google Диске нет копии «Потрачено»")
        val temp = File.createTempFile("drive-in", ".zip", cacheDir)
        try {
            api.download(token, remote.id, temp)
            return backupManager.importFrom(temp)
        } finally {
            temp.delete()
        }
    }
}
