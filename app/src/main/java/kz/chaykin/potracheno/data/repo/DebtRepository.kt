package kz.chaykin.potracheno.data.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kz.chaykin.potracheno.data.db.dao.DebtDao
import kz.chaykin.potracheno.data.db.entity.DebtEntryEntity
import kz.chaykin.potracheno.model.DebtEntry
import kz.chaykin.potracheno.model.PersonBalance
import kz.chaykin.potracheno.util.NameOrder
import java.time.LocalDateTime
import kotlin.math.absoluteValue

/**
 * Долги в разрезе поездки и в её валюте. Баланс поездки они не трогают:
 * «Осталось» считается так, будто все долги уже вернули.
 */
class DebtRepository(
    private val debtDao: DebtDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /** Сначала те, с кем не в расчёте, — по размеру долга; потом остальные по имени. */
    fun observeBalances(tripId: Long): Flow<List<PersonBalance>> =
        debtDao.observeBalances(tripId).map { rows ->
            rows.map { it.toDomain() }.sortedWith(
                compareBy<PersonBalance> { it.balanceMinor == 0L }
                    .thenByDescending { it.balanceMinor.absoluteValue }
                    .thenBy(NameOrder.comparator) { it.person.name },
            )
        }

    fun observeEntries(tripId: Long, personId: Long): Flow<List<DebtEntry>> =
        debtDao.observeEntries(tripId, personId).map { rows -> rows.map { it.toDomain() } }

    /** [signedMinor]: плюс — он мне должен, минус — я ему. */
    suspend fun addEntry(
        tripId: Long,
        personId: Long,
        signedMinor: Long,
        comment: String?,
        occurredAt: LocalDateTime,
    ): Long {
        require(signedMinor != 0L)
        return debtDao.insert(
            DebtEntryEntity(
                tripId = tripId,
                personId = personId,
                amountMinor = signedMinor,
                comment = comment,
                occurredAt = occurredAt,
                expenseId = null,
                createdAt = now(),
            ),
        )
    }

    /** Записи из трат удаляются только вместе с тратой — иначе чек и долги разойдутся. */
    suspend fun deleteEntry(id: Long) {
        val entry = debtDao.get(id) ?: return
        if (entry.expenseId == null) debtDao.delete(id)
    }

    /** «Погасить полностью»: компенсирующая запись сводит накопленное в ноль. */
    suspend fun settle(tripId: Long, personId: Long, comment: String?, occurredAt: LocalDateTime) {
        val balance = debtDao.balance(tripId, personId)
        if (balance == 0L) return
        addEntry(tripId, personId, -balance, comment, occurredAt)
    }
}
