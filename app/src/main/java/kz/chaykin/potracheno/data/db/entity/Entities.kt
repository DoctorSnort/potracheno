package kz.chaykin.potracheno.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kz.chaykin.potracheno.model.Category
import kz.chaykin.potracheno.model.CoverColor
import kz.chaykin.potracheno.model.OperationType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val cover: CoverColor,
    val currencyCode: String,
    val currencySymbol: String,
    /** ₽ за 1 единицу валюты. Строкой: курс бывает 0,0035, и Double его испортит. */
    val rate: BigDecimal,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "operations",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    // Строка времени фиксированного формата сортируется как время — индекс работает и для ORDER BY.
    indices = [Index(value = ["tripId", "occurredAt"])],
)
data class OperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val type: OperationType,
    /** У траты — только своя доля. */
    val amountMinor: Long,
    /** Полный чек, если часть заплачена за других. */
    val paidMinor: Long?,
    val category: Category?,
    val comment: String?,
    val occurredAt: LocalDateTime,
    val excludeFromDaily: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val photoFileName: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "debt_entries",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE,
        ),
        // Удалил трату — ушли и долги, которые она породила.
        ForeignKey(
            entity = OperationEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["tripId", "personId"]),
        Index(value = ["personId"]),
        Index(value = ["expenseId"]),
    ],
)
data class DebtEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val personId: Long,
    /** Плюс — мне должны, минус — я должен. */
    val amountMinor: Long,
    val comment: String?,
    val occurredAt: LocalDateTime,
    val expenseId: Long?,
    val createdAt: Long,
)

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["tripId"])],
)
data class ChecklistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** null — пункт на все поездки. */
    val tripId: Long?,
    val text: String,
    val done: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
