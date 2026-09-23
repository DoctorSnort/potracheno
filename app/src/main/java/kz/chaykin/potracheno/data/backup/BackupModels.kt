package kz.chaykin.potracheno.data.backup

import kotlinx.serialization.Serializable

/**
 * Формат резервной копии. Плоские списки с исходными id: здесь сплошные перекрёстные ссылки
 * (поездка ← трата ← долг → человек), и вложенностью их не выразить. Перечисления, даты
 * и курс пишутся строками — копия должна читаться человеком без приложения.
 */
@Serializable
data class BackupFile(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAt: Long,
    val trips: List<BackupTrip> = emptyList(),
    val operations: List<BackupOperation> = emptyList(),
    val persons: List<BackupPerson> = emptyList(),
    val debts: List<BackupDebt> = emptyList(),
    val checklist: List<BackupChecklistItem> = emptyList(),
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        const val DATA_ENTRY = "data.json"
        const val PHOTOS_PREFIX = "photos/"
    }
}

@Serializable
data class BackupTrip(
    val id: Long,
    val name: String,
    val emoji: String,
    val cover: String,
    val currencyCode: String,
    val currencySymbol: String,
    val rate: String,
    val startDate: String,
    val endDate: String,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

@Serializable
data class BackupOperation(
    val id: Long,
    val tripId: Long,
    val type: String,
    val amountMinor: Long,
    val paidMinor: Long? = null,
    val category: String? = null,
    val comment: String? = null,
    val occurredAt: String,
    val excludeFromDaily: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

@Serializable
data class BackupPerson(
    val id: Long,
    val name: String,
    val photo: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

@Serializable
data class BackupDebt(
    val id: Long,
    val tripId: Long,
    val personId: Long,
    val amountMinor: Long,
    val comment: String? = null,
    val occurredAt: String,
    val expenseId: Long? = null,
    val createdAt: Long = 0,
)

@Serializable
data class BackupChecklistItem(
    val id: Long,
    val tripId: Long? = null,
    val text: String,
    val done: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

data class ImportResult(val tripCount: Int, val operationCount: Int, val personCount: Int)
