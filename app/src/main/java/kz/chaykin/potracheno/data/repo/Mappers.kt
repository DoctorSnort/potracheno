package kz.chaykin.potracheno.data.repo

import kz.chaykin.potracheno.data.db.dao.DebtEntryRow
import kz.chaykin.potracheno.data.db.dao.PersonBalanceRow
import kz.chaykin.potracheno.data.db.entity.ChecklistItemEntity
import kz.chaykin.potracheno.data.db.entity.OperationEntity
import kz.chaykin.potracheno.data.db.entity.PersonEntity
import kz.chaykin.potracheno.data.db.entity.TripEntity
import kz.chaykin.potracheno.model.ChecklistItem
import kz.chaykin.potracheno.model.DebtEntry
import kz.chaykin.potracheno.model.Operation
import kz.chaykin.potracheno.model.Person
import kz.chaykin.potracheno.model.PersonBalance
import kz.chaykin.potracheno.model.Trip

fun TripEntity.toDomain() = Trip(
    id = id,
    name = name,
    emoji = emoji,
    cover = cover,
    currencyCode = currencyCode,
    currencySymbol = currencySymbol,
    rate = rate,
    startDate = startDate,
    endDate = endDate,
    createdAt = createdAt,
)

fun OperationEntity.toDomain() = Operation(
    id = id,
    tripId = tripId,
    type = type,
    amountMinor = amountMinor,
    paidMinor = paidMinor,
    category = category,
    comment = comment,
    occurredAt = occurredAt,
    excludeFromDaily = excludeFromDaily,
    createdAt = createdAt,
)

fun PersonEntity.toDomain() = Person(
    id = id,
    name = name,
    photoFileName = photoFileName,
    createdAt = createdAt,
)

fun PersonBalanceRow.toDomain() = PersonBalance(
    person = person.toDomain(),
    balanceMinor = balanceMinor,
    entryCount = entryCount,
)

fun DebtEntryRow.toDomain() = DebtEntry(
    id = entry.id,
    tripId = entry.tripId,
    personId = entry.personId,
    amountMinor = entry.amountMinor,
    comment = entry.comment,
    occurredAt = entry.occurredAt,
    expenseId = entry.expenseId,
    expenseCategory = expenseCategory,
    expenseComment = expenseComment,
)

fun ChecklistItemEntity.toDomain() = ChecklistItem(
    id = id,
    tripId = tripId,
    text = text,
    done = done,
    createdAt = createdAt,
)
