package kz.chaykin.potracheno.domain.stats

import kz.chaykin.potracheno.model.Category
import java.time.LocalDate

data class ReportExpense(
    val amountMinor: Long,
    val day: LocalDate,
    val excluded: Boolean,
    val category: Category,
)

data class DayPoint(
    val date: LocalDate,
    /** Траты дня, которые идут в среднее. */
    val counted: Long,
    /** Траты дня «вне среднего» — на столбике бледной верхушкой. */
    val excluded: Long,
    val cumSpent: Long,
    val remainingEndOfDay: Long,
    val isToday: Boolean,
) {
    val total: Long get() = counted + excluded
}

data class CategoryShare(val category: Category, val amountMinor: Long, val fraction: Float)

data class ReportData(
    val topUpTotal: Long,
    val spentTotal: Long,
    val days: List<DayPoint>,
    /** Остаток «по плану»: ровная линия от всех пополнений в первый день до нуля в последний. */
    val idealRemaining: List<Long>,
    val categories: List<CategoryShare>,
    val maxDayTotal: Long,
)

/**
 * Данные для графиков отчёта. Ряд дней — с первого дня поездки по сегодня
 * (или по последний день, если поездка закончилась). Операции за пределами дат
 * прижимаются к краям ряда: итоги от этого не меняются, а график не рвётся.
 */
class ReportBuilder {

    fun build(
        start: LocalDate,
        end: LocalDate,
        today: LocalDate,
        topUps: List<TopUpFact>,
        expenses: List<ReportExpense>,
    ): ReportData {
        val topUpTotal = topUps.sumOf { it.amountMinor }
        val spentTotal = expenses.sumOf { it.amountMinor }
        val categories = categoriesOf(expenses, spentTotal)

        val lastDay = minOf(today, end)
        if (today.isBefore(start)) {
            return ReportData(topUpTotal, spentTotal, emptyList(), emptyList(), categories, 0)
        }

        val dates = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(lastDay) }.toList()
        fun clamp(day: LocalDate): LocalDate = when {
            day.isBefore(start) -> start
            day.isAfter(lastDay) -> lastDay
            else -> day
        }

        val countedByDay = expenses.filterNot { it.excluded }.groupBy { clamp(it.day) }
            .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
        val excludedByDay = expenses.filter { it.excluded }.groupBy { clamp(it.day) }
            .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
        val topUpByDay = topUps.groupBy { clamp(it.day) }
            .mapValues { (_, list) -> list.sumOf { it.amountMinor } }

        var cumSpent = 0L
        var cumTopUp = 0L
        val days = dates.map { date ->
            val counted = countedByDay[date] ?: 0L
            val excluded = excludedByDay[date] ?: 0L
            cumSpent += counted + excluded
            cumTopUp += topUpByDay[date] ?: 0L
            DayPoint(
                date = date,
                counted = counted,
                excluded = excluded,
                cumSpent = cumSpent,
                remainingEndOfDay = cumTopUp - cumSpent,
                isToday = date == today,
            )
        }

        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt() + 1
        val ideal = days.indices.map { index ->
            // К концу i-го дня по плану должно остаться (N − i − 1) / N от всех пополнений.
            topUpTotal * (totalDays - index - 1) / totalDays
        }

        return ReportData(
            topUpTotal = topUpTotal,
            spentTotal = spentTotal,
            days = days,
            idealRemaining = ideal,
            categories = categories,
            maxDayTotal = days.maxOfOrNull { it.total } ?: 0L,
        )
    }

    private fun categoriesOf(expenses: List<ReportExpense>, total: Long): List<CategoryShare> {
        if (total <= 0) return emptyList()
        return expenses.groupBy { it.category }
            .map { (category, list) ->
                val sum = list.sumOf { it.amountMinor }
                CategoryShare(category, sum, sum.toFloat() / total)
            }
            .filter { it.amountMinor > 0 }
            .sortedByDescending { it.amountMinor }
    }
}
