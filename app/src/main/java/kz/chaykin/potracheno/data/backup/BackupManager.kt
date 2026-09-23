package kz.chaykin.potracheno.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kz.chaykin.potracheno.data.db.Converters
import kz.chaykin.potracheno.data.db.PotrachenoDatabase
import kz.chaykin.potracheno.data.db.entity.ChecklistItemEntity
import kz.chaykin.potracheno.data.db.entity.DebtEntryEntity
import kz.chaykin.potracheno.data.db.entity.OperationEntity
import kz.chaykin.potracheno.data.db.entity.PersonEntity
import kz.chaykin.potracheno.data.db.entity.TripEntity
import kz.chaykin.potracheno.data.photo.PhotoStore
import kz.chaykin.potracheno.model.Category
import kz.chaykin.potracheno.model.CoverColor
import kz.chaykin.potracheno.model.OperationType
import java.io.File
import java.io.OutputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Резервная копия — обычный ZIP: `data.json` со всеми записями и папка `photos/` с фото людей.
 * Обычный, потому что данные должны быть доставаемы и без этого приложения.
 */
class BackupManager(
    private val context: Context,
    private val database: PotrachenoDatabase,
    private val photoStore: PhotoStore,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun export(target: Uri): Unit = withContext(Dispatchers.IO) {
        val output = requireNotNull(context.contentResolver.openOutputStream(target)) {
            "Не удалось открыть файл для записи"
        }
        output.use { writeArchive(it) }
    }

    /** Тот же архив, но в обычный файл: так его забирает выгрузка на Google Диск. */
    suspend fun exportTo(target: File): Unit = withContext(Dispatchers.IO) {
        target.outputStream().use { writeArchive(it) }
    }

    private suspend fun writeArchive(output: OutputStream) {
        // Все таблицы читаются одним снимком: ежесуточная выгрузка идёт в фоне, и без транзакции
        // трата, удалённая между чтением операций и долгов, уехала бы на Диск без своих долгов.
        val backup = database.withTransaction {
            BackupFile(
                exportedAt = System.currentTimeMillis(),
                trips = database.tripDao().getAll().map { it.toBackup() },
                operations = database.operationDao().getAll().map { it.toBackup() },
                persons = database.personDao().getAll().map { it.toBackup() },
                debts = database.debtDao().getAll().map { it.toBackup() },
                checklist = database.checklistDao().getAll().map { it.toBackup() },
            )
        }

        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(BackupFile.DATA_ENTRY))
            zip.write(json.encodeToString(backup).toByteArray())
            zip.closeEntry()

            backup.persons.mapNotNull { it.photo }.toSet().forEach { fileName ->
                val file = photoStore.file(fileName)
                if (!file.exists()) return@forEach
                zip.putNextEntry(ZipEntry(BackupFile.PHOTOS_PREFIX + fileName))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /**
     * Полностью заменяет содержимое приложения. Сначала архив копируется во временный файл:
     * ZIP нужно читать вразнобой, а поток из системного выбора файлов этого не умеет.
     *
     * Без отмены: если уйти с экрана посреди восстановления, база окажется новой,
     * а фотографии — наполовину старыми. Раз начали — доделываем.
     */
    suspend fun import(source: Uri): ImportResult = withContext(Dispatchers.IO + NonCancellable) {
        val temp = File.createTempFile("import", ".zip", context.cacheDir)
        try {
            requireNotNull(context.contentResolver.openInputStream(source)) {
                "Не удалось открыть файл"
            }.use { input -> temp.outputStream().use { input.copyTo(it) } }
            readArchive(temp)
        } finally {
            temp.delete()
        }
    }

    /** Восстановление из уже скачанного файла — например, из копии на Google Диске. */
    suspend fun importFrom(archive: File): ImportResult = withContext(Dispatchers.IO + NonCancellable) {
        readArchive(archive)
    }

    private suspend fun readArchive(archive: File): ImportResult {
        return ZipFile(archive).use { zip ->
            val dataEntry = requireNotNull(zip.getEntry(BackupFile.DATA_ENTRY)) {
                "В файле нет ${BackupFile.DATA_ENTRY} — это не копия «Потрачено»"
            }
            val backup = zip.getInputStream(dataEntry).use {
                json.decodeFromString<BackupFile>(it.readBytes().decodeToString())
            }
            require(backup.schemaVersion <= BackupFile.CURRENT_SCHEMA_VERSION) {
                "Копия сделана более новой версией приложения"
            }

            // Разбираем всё до транзакции: кривая строка должна уронить импорт
            // раньше, чем мы сотрём то, что сейчас в приложении.
            val trips = backup.trips.map { it.toEntity() }
            val tripIds = trips.map { it.id }.toSet()
            val persons = backup.persons.map { it.toEntity() }
            val personIds = persons.map { it.id }.toSet()
            val operations = backup.operations.filter { it.tripId in tripIds }.map { it.toEntity() }
            val operationIds = operations.map { it.id }.toSet()
            val debts = backup.debts
                .filter { it.tripId in tripIds && it.personId in personIds }
                .filter { it.expenseId == null || it.expenseId in operationIds }
                .map { it.toEntity() }
            val checklist = backup.checklist
                .filter { it.tripId == null || it.tripId in tripIds }
                .map { it.toEntity() }

            database.withTransaction {
                // Каскад от поездок и людей уносит операции и долги.
                database.tripDao().deleteAll()
                database.personDao().deleteAll()
                database.checklistDao().deleteAll()

                trips.forEach { database.tripDao().insert(it) }
                persons.forEach { database.personDao().insert(it) }
                operations.forEach { database.operationDao().insert(it) }
                database.debtDao().insertAll(debts)
                checklist.forEach { database.checklistDao().insert(it) }
            }

            photoStore.deleteAll()
            zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.startsWith(BackupFile.PHOTOS_PREFIX) }
                .forEach { entry ->
                    val fileName = entry.name.removePrefix(BackupFile.PHOTOS_PREFIX)
                    // Имена из архива не должны уводить запись за пределы папки с фото.
                    if (fileName.isEmpty() || fileName.contains('/') || fileName.contains('\\')) {
                        return@forEach
                    }
                    val bytes = zip.getInputStream(entry).use { it.readBytes() }
                    photoStore.writeRaw(fileName, bytes)
                }

            ImportResult(
                tripCount = trips.size,
                operationCount = operations.size,
                personCount = persons.size,
            )
        }
    }

    private fun TripEntity.toBackup() = BackupTrip(
        id = id,
        name = name,
        emoji = emoji,
        cover = cover.name,
        currencyCode = currencyCode,
        currencySymbol = currencySymbol,
        rate = rate.toPlainString(),
        startDate = startDate.toString(),
        endDate = endDate.toString(),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun BackupTrip.toEntity() = TripEntity(
        id = id,
        name = name,
        emoji = emoji,
        cover = CoverColor.fromNameOrDefault(cover),
        currencyCode = currencyCode,
        currencySymbol = currencySymbol,
        rate = BigDecimal(rate),
        startDate = LocalDate.parse(startDate),
        endDate = LocalDate.parse(endDate),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun OperationEntity.toBackup() = BackupOperation(
        id = id,
        tripId = tripId,
        type = type.name,
        amountMinor = amountMinor,
        paidMinor = paidMinor,
        category = category?.name,
        comment = comment,
        occurredAt = Converters.formatDateTime(occurredAt),
        excludeFromDaily = excludeFromDaily,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun BackupOperation.toEntity(): OperationEntity {
        val operationType = runCatching { OperationType.valueOf(type) }.getOrDefault(OperationType.EXPENSE)
        return OperationEntity(
            id = id,
            tripId = tripId,
            type = operationType,
            amountMinor = amountMinor,
            paidMinor = paidMinor,
            category = if (operationType == OperationType.EXPENSE) Category.fromNameOrDefault(category) else null,
            comment = comment,
            occurredAt = Converters.parseDateTime(occurredAt),
            excludeFromDaily = excludeFromDaily,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private fun PersonEntity.toBackup() = BackupPerson(
        id = id,
        name = name,
        photo = photoFileName,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun BackupPerson.toEntity() = PersonEntity(
        id = id,
        name = name,
        photoFileName = photo,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun DebtEntryEntity.toBackup() = BackupDebt(
        id = id,
        tripId = tripId,
        personId = personId,
        amountMinor = amountMinor,
        comment = comment,
        occurredAt = Converters.formatDateTime(occurredAt),
        expenseId = expenseId,
        createdAt = createdAt,
    )

    private fun BackupDebt.toEntity() = DebtEntryEntity(
        id = id,
        tripId = tripId,
        personId = personId,
        amountMinor = amountMinor,
        comment = comment,
        occurredAt = Converters.parseDateTime(occurredAt),
        expenseId = expenseId,
        createdAt = createdAt,
    )

    private fun ChecklistItemEntity.toBackup() = BackupChecklistItem(
        id = id,
        tripId = tripId,
        text = text,
        done = done,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun BackupChecklistItem.toEntity() = ChecklistItemEntity(
        id = id,
        tripId = tripId,
        text = text,
        done = done,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
