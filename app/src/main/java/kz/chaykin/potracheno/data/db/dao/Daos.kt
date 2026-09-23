package kz.chaykin.potracheno.data.db.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kz.chaykin.potracheno.data.db.entity.ChecklistItemEntity
import kz.chaykin.potracheno.data.db.entity.DebtEntryEntity
import kz.chaykin.potracheno.data.db.entity.OperationEntity
import kz.chaykin.potracheno.data.db.entity.PersonEntity
import kz.chaykin.potracheno.data.db.entity.TripEntity
import kz.chaykin.potracheno.model.Category

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY startDate DESC, id DESC")
    fun observeAll(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id")
    fun observe(id: Long): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun get(id: Long): TripEntity?

    @Query("SELECT * FROM trips ORDER BY id")
    suspend fun getAll(): List<TripEntity>

    @Insert
    suspend fun insert(trip: TripEntity): Long

    @Update
    suspend fun update(trip: TripEntity)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM trips")
    suspend fun deleteAll()
}

@Dao
interface OperationDao {
    /**
     * Все операции поездки разом: их сотни, а статистике и переключателю рублей
     * удобнее считать в памяти, чем гонять по запросу на каждую цифру.
     */
    @Query("SELECT * FROM operations WHERE tripId = :tripId ORDER BY occurredAt DESC, id DESC")
    fun observeByTrip(tripId: Long): Flow<List<OperationEntity>>

    @Query("SELECT * FROM operations WHERE id = :id")
    suspend fun get(id: Long): OperationEntity?

    @Query("SELECT * FROM operations ORDER BY id")
    suspend fun getAll(): List<OperationEntity>

    @Insert
    suspend fun insert(operation: OperationEntity): Long

    @Update
    suspend fun update(operation: OperationEntity)

    @Query("UPDATE operations SET paidMinor = :paidMinor WHERE id = :id")
    suspend fun setPaid(id: Long, paidMinor: Long?)

    @Query("DELETE FROM operations WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface PersonDao {
    @Query("SELECT * FROM persons")
    fun observeAll(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons WHERE id = :id")
    fun observe(id: Long): Flow<PersonEntity?>

    @Query("SELECT * FROM persons ORDER BY id")
    suspend fun getAll(): List<PersonEntity>

    @Query("SELECT photoFileName FROM persons WHERE photoFileName IS NOT NULL")
    suspend fun allPhotoFileNames(): List<String>

    @Insert
    suspend fun insert(person: PersonEntity): Long

    @Update
    suspend fun update(person: PersonEntity)

    @Query("DELETE FROM persons WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM persons")
    suspend fun deleteAll()
}

data class PersonBalanceRow(
    @Embedded val person: PersonEntity,
    val balanceMinor: Long,
    val entryCount: Int,
)

data class DebtEntryRow(
    @Embedded val entry: DebtEntryEntity,
    val expenseCategory: Category?,
    val expenseComment: String?,
)

@Dao
interface DebtDao {
    /** Все люди, даже те, с кем в этой поездке в расчёте: иначе новичка не найти на экране долгов. */
    @Query(
        "SELECT p.*, COALESCE(SUM(d.amountMinor), 0) AS balanceMinor, COUNT(d.id) AS entryCount " +
            "FROM persons p LEFT JOIN debt_entries d ON d.personId = p.id AND d.tripId = :tripId " +
            "GROUP BY p.id",
    )
    fun observeBalances(tripId: Long): Flow<List<PersonBalanceRow>>

    @Query(
        "SELECT d.*, o.category AS expenseCategory, o.comment AS expenseComment " +
            "FROM debt_entries d LEFT JOIN operations o ON o.id = d.expenseId " +
            "WHERE d.tripId = :tripId AND d.personId = :personId " +
            "ORDER BY d.occurredAt DESC, d.id DESC",
    )
    fun observeEntries(tripId: Long, personId: Long): Flow<List<DebtEntryRow>>

    @Query("SELECT * FROM debt_entries WHERE expenseId = :expenseId ORDER BY id")
    suspend fun byExpense(expenseId: Long): List<DebtEntryEntity>

    @Query("DELETE FROM debt_entries WHERE expenseId = :expenseId")
    suspend fun deleteByExpense(expenseId: Long)

    @Query("SELECT DISTINCT expenseId FROM debt_entries WHERE personId = :personId AND expenseId IS NOT NULL")
    suspend fun expenseIdsByPerson(personId: Long): List<Long>

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM debt_entries WHERE expenseId = :expenseId")
    suspend fun sumByExpense(expenseId: Long): Long

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM debt_entries WHERE tripId = :tripId AND personId = :personId")
    suspend fun balance(tripId: Long, personId: Long): Long

    @Query("SELECT * FROM debt_entries WHERE id = :id")
    suspend fun get(id: Long): DebtEntryEntity?

    @Query("SELECT * FROM debt_entries ORDER BY id")
    suspend fun getAll(): List<DebtEntryEntity>

    @Insert
    suspend fun insert(entry: DebtEntryEntity): Long

    @Insert
    suspend fun insertAll(entries: List<DebtEntryEntity>)

    @Query("DELETE FROM debt_entries WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ChecklistDao {
    /** Пункты на все поездки плюс пункты текущей. Невыполненные сверху. */
    @Query(
        "SELECT * FROM checklist_items WHERE tripId IS NULL OR tripId = :tripId " +
            "ORDER BY done, createdAt, id",
    )
    fun observeVisible(tripId: Long?): Flow<List<ChecklistItemEntity>>

    @Query("SELECT * FROM checklist_items WHERE id = :id")
    suspend fun get(id: Long): ChecklistItemEntity?

    @Query("SELECT * FROM checklist_items ORDER BY id")
    suspend fun getAll(): List<ChecklistItemEntity>

    @Insert
    suspend fun insert(item: ChecklistItemEntity): Long

    @Update
    suspend fun update(item: ChecklistItemEntity)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun delete(id: Long)

    /**
     * «Убрать выполненные»: пункты этой поездки удаляются, а у пунктов «всегда» только
     * снимается отметка — паспорт понадобится и в следующей поездке.
     */
    @Transaction
    suspend fun clearDone(tripId: Long?) {
        if (tripId != null) deleteDoneOfTrip(tripId)
        resetDoneUniversal()
    }

    @Query("DELETE FROM checklist_items WHERE done = 1 AND tripId = :tripId")
    suspend fun deleteDoneOfTrip(tripId: Long)

    @Query("UPDATE checklist_items SET done = 0 WHERE done = 1 AND tripId IS NULL")
    suspend fun resetDoneUniversal()

    @Query("DELETE FROM checklist_items")
    suspend fun deleteAll()
}
