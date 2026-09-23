package kz.chaykin.potracheno.model

import java.time.LocalDateTime

/** Попутчик. Общий на все поездки, фотография — чисто для красоты. */
data class Person(
    val id: Long = 0,
    val name: String,
    val photoFileName: String? = null,
    val createdAt: Long = 0,
)

/**
 * Запись долга в валюте поездки. Плюс — человек должен мне, минус — я ему.
 * Долги не трогают ни баланс, ни статистику: расчёт на то, что всё вернут.
 */
data class DebtEntry(
    val id: Long = 0,
    val tripId: Long,
    val personId: Long,
    val amountMinor: Long,
    val comment: String? = null,
    val occurredAt: LocalDateTime,
    /** Запись родилась из траты — правится только через неё. */
    val expenseId: Long? = null,
    val expenseCategory: Category? = null,
    val expenseComment: String? = null,
)

data class PersonBalance(
    val person: Person,
    /** Накопительно по поездке: плюс — должен мне, минус — я ему. */
    val balanceMinor: Long,
    val entryCount: Int,
)
