package kz.chaykin.potracheno.domain.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TripStatsCalculatorTest {

    private val calculator = TripStatsCalculator()
    private val start = LocalDate.of(2026, 9, 1)
    private val end = LocalDate.of(2026, 9, 10)

    private fun day(n: Int): LocalDate = LocalDate.of(2026, 9, n)

    private fun calc(
        today: LocalDate,
        topUps: List<TopUpFact> = listOf(TopUpFact(1_000_000, day(1))),
        expenses: List<ExpenseFact> = emptyList(),
        start: LocalDate = this.start,
        end: LocalDate = this.end,
    ) = calculator.calculate(TripStatsInput(start, end, today, topUps, expenses))

    @Test
    fun `поездка ещё не началась — бюджет на день на все дни, средних трат нет`() {
        val stats = calc(today = day(1).minusDays(3))
        assertEquals(TripPhase.UPCOMING, stats.phase)
        assertEquals(10, stats.daysLeft)
        assertEquals(0, stats.elapsedDays)
        assertEquals(3, stats.daysUntilStart)
        assertNull(stats.avgDailySpent)
        assertEquals(100_000L, stats.avgDailyLeft)
        assertNull(stats.pace)
        assertNull(stats.forecast)
    }

    @Test
    fun `до поездки сегодняшние траты уже не входят в бюджет на день`() {
        // Сегодня купили билеты на 3000 — эти деньги на поездку уже не обещаны.
        val stats = calc(
            today = day(1).minusDays(5),
            topUps = listOf(TopUpFact(10_000, day(1).minusDays(5))),
            expenses = listOf(ExpenseFact(3_000, day(1).minusDays(5), excluded = false)),
            start = day(1),
            end = day(7),
        )
        assertEquals(1_000L, stats.avgDailyLeft)
    }

    @Test
    fun `первый день — средней нет, сегодняшние траты не уменьшают остаток на утро`() {
        val stats = calc(
            today = day(1),
            expenses = listOf(ExpenseFact(20_000, day(1), excluded = false)),
        )
        assertEquals(0, stats.elapsedDays)
        assertEquals(10, stats.daysLeft)
        assertNull(stats.avgDailySpent)
        assertEquals(1_000_000L, stats.remainingAtStartOfToday)
        assertEquals(980_000L, stats.remaining)
        assertEquals(100_000L, stats.avgDailyLeft)
        assertEquals(80_000L, stats.canSpendToday)
        assertEquals(Forecast.NotEnoughData, stats.forecast)
    }

    @Test
    fun `середина поездки — прошедшие дни без сегодня, оставшиеся с сегодня`() {
        val stats = calc(today = day(4))
        assertEquals(3, stats.elapsedDays)
        assertEquals(7, stats.daysLeft)
    }

    @Test
    fun `последний день — весь остаток на сегодня`() {
        val stats = calc(today = day(10))
        assertEquals(1, stats.daysLeft)
        assertEquals(stats.remainingAtStartOfToday, stats.avgDailyLeft)
    }

    @Test
    fun `поездка закончилась — среднее за всю поездку, остатка на день нет`() {
        val stats = calc(
            today = day(15),
            expenses = listOf(ExpenseFact(100_000, day(2), false), ExpenseFact(100_000, day(10), false)),
        )
        assertEquals(TripPhase.FINISHED, stats.phase)
        assertEquals(0, stats.daysLeft)
        assertEquals(10, stats.elapsedDays)
        assertEquals(20_000L, stats.avgDailySpent)
        assertNull(stats.avgDailyLeft)
        assertNull(stats.canSpendToday)
        assertNull(stats.pace)
    }

    @Test
    fun `однодневная поездка`() {
        val stats = calc(today = day(5), start = day(5), end = day(5))
        assertEquals(1, stats.totalDays)
        assertEquals(1, stats.daysLeft)
        assertEquals(0, stats.elapsedDays)
    }

    @Test
    fun `трата вне среднего есть в итогах, но не в среднем`() {
        val stats = calc(
            today = day(3),
            expenses = listOf(
                ExpenseFact(10_000, day(1), false),
                ExpenseFact(10_000, day(2), false),
                ExpenseFact(500_000, day(2), excluded = true),
            ),
        )
        assertEquals(520_000L, stats.spentTotal)
        assertEquals(480_000L, stats.remaining)
        assertEquals(10_000L, stats.avgDailySpent)
    }

    @Test
    fun `трата до начала и в будущем — в итогах есть, в средних и сегодня нет`() {
        val stats = calc(
            today = day(3),
            expenses = listOf(
                ExpenseFact(30_000, start.minusDays(5), false),
                ExpenseFact(40_000, day(8), false),
            ),
        )
        assertEquals(70_000L, stats.spentTotal)
        assertEquals(0L, stats.avgDailySpent)
        assertEquals(0L, stats.todaySpent)
        assertEquals(930_000L, stats.remainingAtStartOfToday)
    }

    @Test
    fun `пополнение сегодняшним числом сразу идёт в остаток на утро`() {
        val stats = calc(
            today = day(3),
            topUps = listOf(TopUpFact(100_000, day(1)), TopUpFact(80_000, day(3))),
        )
        assertEquals(180_000L, stats.remainingAtStartOfToday)
    }

    @Test
    fun `без пополнений — деньги кончились`() {
        val stats = calc(
            today = day(3),
            topUps = emptyList(),
            expenses = listOf(ExpenseFact(1_000, day(1), false)),
        )
        assertEquals(-1_000L, stats.remaining)
        assertEquals(Pace.BROKE, stats.pace)
        assertEquals(Forecast.AlreadyBroke, stats.forecast)
    }

    @Test
    fun `ничего не тратил — хватит на всю поездку с остатком`() {
        val stats = calc(today = day(3))
        assertEquals(Forecast.LastsWholeTrip(1_000_000), stats.forecast)
        assertEquals(Pace.ON_TRACK, stats.pace)
    }

    @Test
    fun `деньги кончатся раньше конца`() {
        // Утро 4-го: остаток 1000, в среднем 300 в день → покрыто 3 дня: 4, 5, 6.
        val stats = calc(
            today = day(4),
            topUps = listOf(TopUpFact(1_900, day(1))),
            expenses = listOf(
                ExpenseFact(300, day(1), false),
                ExpenseFact(300, day(2), false),
                ExpenseFact(300, day(3), false),
            ),
        )
        assertEquals(1_000L, stats.remainingAtStartOfToday)
        assertEquals(300L, stats.avgDailySpent)
        assertEquals(Forecast.RunsOutOn(day(6), shortDays = 4), stats.forecast)
        assertEquals(Pace.OVERSPENDING, stats.pace)
    }

    @Test
    fun `денег меньше, чем на день, — кончатся сегодня`() {
        val stats = calc(
            today = day(3),
            topUps = listOf(TopUpFact(1_100, day(1))),
            expenses = listOf(ExpenseFact(500, day(1), false), ExpenseFact(500, day(2), false)),
        )
        assertEquals(Forecast.RunsOutOn(day(3), shortDays = 8, today = true), stats.forecast)
    }

    @Test
    fun `перерасход сегодня уводит «можно ещё» в минус, экскурсия сегодня не считается`() {
        val stats = calc(
            today = day(1),
            topUps = listOf(TopUpFact(10_000, day(1))),
            expenses = listOf(
                ExpenseFact(1_500, day(1), false),
                ExpenseFact(5_000, day(1), excluded = true),
            ),
        )
        assertEquals(1_000L, stats.avgDailyLeft)
        assertEquals(6_500L, stats.todaySpent)
        assertEquals(1_500L, stats.todayCounted)
        assertEquals(-500L, stats.canSpendToday)
    }

    @Test
    fun `пороги темпа`() {
        fun paceFor(avgSpent: Long): Pace? {
            // 10 дней, сегодня 3-е: осталось 8 дней. Утром остаток 8000 → 1000 в день.
            val spentPerDay = avgSpent
            return calc(
                today = day(3),
                topUps = listOf(TopUpFact(8_000 + spentPerDay * 2, day(1))),
                expenses = listOf(ExpenseFact(spentPerDay, day(1), false), ExpenseFact(spentPerDay, day(2), false)),
            ).pace
        }
        assertEquals(Pace.ON_TRACK, paceFor(1_000))
        assertEquals(Pace.TIGHT, paceFor(1_100))
        assertEquals(Pace.OVERSPENDING, paceFor(1_200))
    }

    @Test
    fun `деление округляется до ближайшего`() {
        assertEquals(333L, TripStatsCalculator.divRoundHalfUp(1_000, 3))
        assertEquals(334L, TripStatsCalculator.divRoundHalfUp(1_001, 3))
        assertEquals(-333L, TripStatsCalculator.divRoundHalfUp(-1_000, 3))
        assertNull(TripStatsCalculator.divRoundHalfUp(1_000, 0))
    }

    @Test
    fun `сценарий из плана — Китай`() {
        // Поездка с 1 по 10, сегодня 3-е.
        val stats = calc(
            today = day(3),
            topUps = listOf(TopUpFact(1_000_000, day(1))),
            expenses = listOf(
                ExpenseFact(30_000, day(1), false),
                ExpenseFact(50_000, day(2), false),
                ExpenseFact(200_000, day(2), excluded = true),
                ExpenseFact(20_000, day(3), false),
            ),
        )
        assertEquals(300_000L, stats.spentTotal)
        assertEquals(700_000L, stats.remaining)
        assertEquals(40_000L, stats.avgDailySpent)
        assertEquals(720_000L, stats.remainingAtStartOfToday)
        assertEquals(8, stats.daysLeft)
        assertEquals(90_000L, stats.avgDailyLeft)
        assertEquals(70_000L, stats.canSpendToday)
        assertEquals(Forecast.LastsWholeTrip(720_000L - 40_000L * 8), stats.forecast)
    }
}
