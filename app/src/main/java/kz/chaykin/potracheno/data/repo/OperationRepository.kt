package kz.chaykin.potracheno.data.repo

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kz.chaykin.potracheno.data.db.PotrachenoDatabase
import kz.chaykin.potracheno.data.db.entity.DebtEntryEntity
import kz.chaykin.potracheno.data.db.entity.OperationEntity
import kz.chaykin.potracheno.model.DebtShare
import kz.chaykin.potracheno.model.Operation
import kz.chaykin.potracheno.model.OperationType

class OperationRepository(
    private val database: PotrachenoDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val operationDao = database.operationDao()
    private val debtDao = database.debtDao()

    fun observeByTrip(tripId: Long): Flow<List<Operation>> =
        operationDao.observeByTrip(tripId).map { list -> list.map { it.toDomain() } }

    suspend fun get(id: Long): Operation? = operationDao.get(id)?.toDomain()

    /** Кому и сколько заплачено внутри траты — для редактора. */
    suspend fun sharesOf(expenseId: Long): List<DebtShare> =
        debtDao.byExpense(expenseId).map { DebtShare(it.personId, it.amountMinor) }

    suspend fun saveTopUp(operation: Operation): Long {
        require(operation.type == OperationType.TOP_UP)
        return database.withTransaction {
            val id = upsert(
                operation.copy(paidMinor = null, category = null, excludeFromDaily = false),
            )
            // Трату могли переделать в пополнение — долги от неё уже ни к чему.
            debtDao.deleteByExpense(id)
            id
        }
    }

    /**
     * Трата с возможной делёжкой. В кошелёк и статистику идёт только своя доля:
     * `amountMinor = paid − Σ долей`, а на каждого, за кого заплатил, заводится долг.
     * Всё в одной транзакции — иначе при сбое трата и долги разойдутся.
     */
    suspend fun saveExpense(operation: Operation, paidMinor: Long, shares: List<DebtShare>): Long {
        require(operation.type == OperationType.EXPENSE)
        require(shares.all { it.amountMinor > 0 }) { "Доля должна быть больше нуля" }
        require(shares.map { it.personId }.toSet().size == shares.size) { "Один человек — одна доля" }
        val sharedTotal = shares.sumOf { it.amountMinor }
        require(sharedTotal <= paidMinor) { "За других заплачено больше, чем весь чек" }

        return database.withTransaction {
            val id = upsert(
                operation.copy(
                    amountMinor = paidMinor - sharedTotal,
                    paidMinor = if (shares.isEmpty()) null else paidMinor,
                ),
            )
            debtDao.deleteByExpense(id)
            if (shares.isNotEmpty()) {
                val createdAt = now()
                debtDao.insertAll(
                    shares.map {
                        DebtEntryEntity(
                            tripId = operation.tripId,
                            personId = it.personId,
                            amountMinor = it.amountMinor,
                            comment = null,
                            occurredAt = operation.occurredAt,
                            expenseId = id,
                            createdAt = createdAt,
                        )
                    },
                )
            }
            id
        }
    }

    /** Каскад уносит и долги, которые трата породила. */
    suspend fun delete(id: Long) = operationDao.delete(id)

    private suspend fun upsert(operation: Operation): Long {
        val timestamp = now()
        val existing = if (operation.id != 0L) operationDao.get(operation.id) else null
        val entity = OperationEntity(
            id = operation.id,
            tripId = operation.tripId,
            type = operation.type,
            amountMinor = operation.amountMinor,
            paidMinor = operation.paidMinor,
            category = operation.category,
            comment = operation.comment,
            occurredAt = operation.occurredAt,
            excludeFromDaily = operation.excludeFromDaily,
            createdAt = existing?.createdAt ?: timestamp,
            updatedAt = timestamp,
        )
        return if (existing == null) {
            operationDao.insert(entity.copy(id = 0))
        } else {
            operationDao.update(entity)
            entity.id
        }
    }
}
