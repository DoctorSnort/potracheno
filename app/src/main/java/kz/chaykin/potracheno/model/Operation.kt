package kz.chaykin.potracheno.model

import java.time.LocalDateTime

data class Operation(
    val id: Long = 0,
    val tripId: Long,
    val type: OperationType,
    /**
     * Сколько ушло из (или пришло в) кошелёк. У траты с долгом — только своя доля:
     * заплатил 3000 за двоих — здесь 1500, остальное «вернут».
     */
    val amountMinor: Long,
    /** Полная сумма чека, если часть заплачена за других; иначе null. Только для показа. */
    val paidMinor: Long? = null,
    val category: Category? = null,
    val comment: String? = null,
    /** Местное время без часового пояса: ужин в 23:30 не должен переехать на завтра при смене зоны. */
    val occurredAt: LocalDateTime,
    /** «Не учитывать в среднедневных» — разовые крупные траты вроде экскурсии. */
    val excludeFromDaily: Boolean = false,
    val createdAt: Long = 0,
) {
    val isExpense: Boolean get() = type == OperationType.EXPENSE
    val hasDebts: Boolean get() = paidMinor != null && paidMinor != amountMinor
}

/** Часть траты, которую заплатил за человека: он её должен. */
data class DebtShare(val personId: Long, val amountMinor: Long)
