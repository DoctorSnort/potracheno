package kz.chaykin.potracheno.domain.stats

import kz.chaykin.potracheno.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReportBuilderTest {

    private val builder = ReportBuilder()
    private fun day(n: Int): LocalDate = LocalDate.of(2026, 9, n)

    @Test
    fun `ряд дней — с начала по сегодня, операции за краями прижимаются к краям`() {
        val report = builder.build(
            start = day(5),
            end = day(15),
            today = day(7),
            topUps = listOf(TopUpFact(10_000, day(1))),
            expenses = listOf(
                ReportExpense(100, day(2), false, Category.FOOD),
                ReportExpense(200, day(6), false, Category.FOOD),
                ReportExpense(300, day(9), true, Category.FUN),
            ),
        )
        assertEquals(listOf(day(5), day(6), day(7)), report.days.map { it.date })
        assertEquals(100L, report.days[0].counted)
        assertEquals(10_000L - 100, report.days[0].remainingEndOfDay)
        assertEquals(300L, report.days[2].excluded)
        assertEquals(600L, report.days.last().cumSpent)
        assertEquals(10_000L - 600, report.days.last().remainingEndOfDay)
        assertTrue(report.days.last().isToday)
    }

    @Test
    fun `поездка не началась — ряда нет, а пончик есть`() {
        val report = builder.build(
            start = day(10),
            end = day(15),
            today = day(1),
            topUps = emptyList(),
            expenses = listOf(ReportExpense(500, day(1), false, Category.HOUSING)),
        )
        assertTrue(report.days.isEmpty())
        assertEquals(Category.HOUSING, report.categories.single().category)
    }

    @Test
    fun `доли категорий в сумме дают единицу и идут по убыванию`() {
        val report = builder.build(
            start = day(1),
            end = day(3),
            today = day(3),
            topUps = emptyList(),
            expenses = listOf(
                ReportExpense(100, day(1), false, Category.FOOD),
                ReportExpense(300, day(2), false, Category.TRANSPORT),
                ReportExpense(600, day(3), true, Category.FUN),
            ),
        )
        assertEquals(listOf(Category.FUN, Category.TRANSPORT, Category.FOOD), report.categories.map { it.category })
        assertEquals(1f, report.categories.sumOf { it.fraction.toDouble() }.toFloat(), 0.0001f)
    }

    @Test
    fun `идеальный остаток доходит до нуля в последний день`() {
        val report = builder.build(
            start = day(1),
            end = day(4),
            today = day(10),
            topUps = listOf(TopUpFact(4_000, day(1))),
            expenses = emptyList(),
        )
        assertEquals(listOf(3_000L, 2_000L, 1_000L, 0L), report.idealRemaining)
    }
}
