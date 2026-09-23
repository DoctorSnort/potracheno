package kz.chaykin.potracheno.domain.stats

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class ExpenseFact(val amountMinor: Long, val day: LocalDate, val excluded: Boolean)

data class TopUpFact(val amountMinor: Long, val day: LocalDate)

data class TripStatsInput(
    val start: LocalDate,
    /** Включительно. */
    val end: LocalDate,
    val today: LocalDate,
    val topUps: List<TopUpFact>,
    /** Только своя доля: долги сюда не попадают вовсе. */
    val expenses: List<ExpenseFact>,
)

/**
 * Статистика поездки. Правила, о которых договорились:
 * - сегодняшние траты не входят ни в одно среднее, но итог за сегодня виден;
 * - траты «вне среднего» (экскурсия) есть во «всего потрачено», но не в средних;
 * - «дней осталось» считает сегодняшний день;
 * - среднедневной остаток — остаток на утро сегодняшнего дня на оставшиеся дни.
 */
class TripStatsCalculator {

    fun calculate(input: TripStatsInput): TripStats {
        val start = input.start
        val end = if (input.end.isBefore(start)) start else input.end
        val today = input.today

        val totalDays = daysBetween(start, end) + 1
        val phase = when {
            today.isBefore(start) -> TripPhase.UPCOMING
            today.isAfter(end) -> TripPhase.FINISHED
            else -> TripPhase.ACTIVE
        }
        val daysLeft = when (phase) {
            TripPhase.UPCOMING -> totalDays
            TripPhase.ACTIVE -> daysBetween(today, end) + 1
            TripPhase.FINISHED -> 0
        }
        val elapsed = when (phase) {
            TripPhase.UPCOMING -> 0
            TripPhase.ACTIVE -> daysBetween(start, today)
            TripPhase.FINISHED -> totalDays
        }
        val daysUntilStart = if (phase == TripPhase.UPCOMING) daysBetween(today, start) else 0

        val topUpTotal = input.topUps.sumOf { it.amountMinor }
        val spentTotal = input.expenses.sumOf { it.amountMinor }
        val remaining = topUpTotal - spentTotal
        val todayExpenses = input.expenses.filter { it.day == today }
        val todaySpent = todayExpenses.sumOf { it.amountMinor }
        val todayCounted = todayExpenses.filterNot { it.excluded }.sumOf { it.amountMinor }
        val remainingAtStartOfToday = remaining + todaySpent

        // Окно средней: с первого дня по вчера (или по последний день, если поездка кончилась).
        val windowEnd = minOf(today.minusDays(1), end)
        val windowSpent = input.expenses
            .filter { !it.excluded && !it.day.isBefore(start) && !it.day.isAfter(windowEnd) }
            .sumOf { it.amountMinor }

        val avgDailySpent = divRoundHalfUp(windowSpent, elapsed.toLong())
        // До начала поездки сегодня — не её день: билеты, купленные сегодня, уже потрачены,
        // и возвращать их в бюджет на дни поездки нельзя.
        val budgetBase = if (phase == TripPhase.UPCOMING) remaining else remainingAtStartOfToday
        val avgDailyLeft = divRoundHalfUp(budgetBase, daysLeft.toLong())
        val canSpendToday = avgDailyLeft?.let { it - todayCounted }

        val active = phase == TripPhase.ACTIVE
        return TripStats(
            phase = phase,
            totalDays = totalDays,
            elapsedDays = elapsed,
            daysLeft = daysLeft,
            daysUntilStart = daysUntilStart,
            topUpTotal = topUpTotal,
            spentTotal = spentTotal,
            remaining = remaining,
            todaySpent = todaySpent,
            todayCounted = todayCounted,
            remainingAtStartOfToday = remainingAtStartOfToday,
            avgDailySpent = avgDailySpent,
            avgDailyLeft = avgDailyLeft,
            canSpendToday = canSpendToday,
            pace = if (active) pace(remainingAtStartOfToday, avgDailySpent, avgDailyLeft, todayCounted) else null,
            forecast = if (active) forecast(remainingAtStartOfToday, avgDailySpent, daysLeft, today) else null,
        )
    }

    private fun pace(r0: Long, avgSpent: Long?, avgLeft: Long?, todayCounted: Long): Pace? {
        if (r0 <= 0) return Pace.BROKE
        val left = avgLeft ?: return null
        if (left <= 0) return Pace.BROKE
        // В первый день средней ещё нет — сравниваем то, что потрачено сегодня.
        val spent = avgSpent ?: todayCounted
        val ratio = spent.toDouble() / left
        return when {
            ratio <= ON_TRACK_MAX -> Pace.ON_TRACK
            ratio <= TIGHT_MAX -> Pace.TIGHT
            else -> Pace.OVERSPENDING
        }
    }

    private fun forecast(r0: Long, avgSpent: Long?, daysLeft: Int, today: LocalDate): Forecast {
        if (r0 <= 0) return Forecast.AlreadyBroke
        val avg = avgSpent ?: return Forecast.NotEnoughData
        if (avg <= 0) return Forecast.LastsWholeTrip(surplusMinor = r0)
        val daysCovered = r0 / avg
        return if (daysCovered >= daysLeft) {
            Forecast.LastsWholeTrip(surplusMinor = r0 - avg * daysLeft)
        } else {
            // Покрыто daysCovered дней, начиная с сегодняшнего; ноль — кончатся уже сегодня.
            val lastCovered = if (daysCovered == 0L) today else today.plusDays(daysCovered - 1)
            Forecast.RunsOutOn(
                date = lastCovered,
                shortDays = (daysLeft - daysCovered).toInt(),
                today = daysCovered == 0L,
            )
        }
    }

    companion object {
        /** Тратишь не больше, чем можно в день, — в бюджете. */
        const val ON_TRACK_MAX = 1.0

        /** До +15 % — ещё не катастрофа, но уже впритык. */
        const val TIGHT_MAX = 1.15

        private fun daysBetween(from: LocalDate, to: LocalDate): Int =
            ChronoUnit.DAYS.between(from, to).toInt()

        /** Деление без исключений: на ноль и меньше — «не посчитать», а не падение. */
        fun divRoundHalfUp(numerator: Long, denominator: Long): Long? {
            if (denominator <= 0) return null
            return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 0, RoundingMode.HALF_UP)
                .toLong()
        }
    }
}
