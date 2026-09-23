package kz.chaykin.potracheno.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class MoneyFormatTest {

    private val nbsp = ' '

    @Test
    fun `разряды и дробная часть только когда есть`() {
        assertEquals("2${nbsp}500$nbsp¥", MoneyFormat.format(250_000, "¥"))
        assertEquals("2${nbsp}500,50$nbsp¥", MoneyFormat.format(250_050, "¥"))
        assertEquals("0,05$nbsp₽", MoneyFormat.format(5, "₽"))
    }

    @Test
    fun `минус не теряется даже у копеек`() {
        assertEquals("-0,50$nbsp₽", MoneyFormat.format(-50, "₽"))
        assertEquals("-1${nbsp}000$nbsp₽", MoneyFormat.format(-100_000, "₽"))
    }

    @Test
    fun `со знаком`() {
        assertEquals("+15$nbsp¥", MoneyFormat.formatSigned(1_500, "¥"))
        assertEquals("−15$nbsp¥", MoneyFormat.formatSigned(-1_500, "¥"))
        assertEquals("0$nbsp¥", MoneyFormat.formatSigned(0, "¥"))
    }

    @Test
    fun `компактные подписи осей`() {
        assertEquals("950", MoneyFormat.compact(95_000))
        assertEquals("12,5к", MoneyFormat.compact(1_250_000))
        assertEquals("3к", MoneyFormat.compact(300_000))
        assertEquals("3,2м", MoneyFormat.compact(320_000_000))
        assertEquals("1м", MoneyFormat.compact(99_995_000))
        assertEquals("999,9к", MoneyFormat.compact(99_994_000))
    }

    @Test
    fun `разбор ввода`() {
        assertEquals(250_000L, MoneyFormat.parse("2500"))
        assertEquals(250_000L, MoneyFormat.parse("2 500"))
        assertEquals(250_050L, MoneyFormat.parse("2500,5"))
        assertEquals(250_050L, MoneyFormat.parse("2500.50"))
        assertEquals(50L, MoneyFormat.parse(",5"))
        assertNull(MoneyFormat.parse(""))
        assertNull(MoneyFormat.parse("-5"))
        assertNull(MoneyFormat.parse("1.2.3"))
        assertNull(MoneyFormat.parse("abc"))
        assertNull(MoneyFormat.parse("1234567890123"))
    }

    @Test
    fun `ввод и вывод — туда и обратно без потерь`() {
        listOf(0L, 5L, 250_050L, 100L).forEach {
            assertEquals(it, MoneyFormat.parse(MoneyFormat.toInput(it)))
        }
    }

    @Test
    fun `курс`() {
        assertEquals(BigDecimal("12.5"), RateFormat.parse("12,50"))
        assertEquals(BigDecimal("0.0035"), RateFormat.parse("0.0035"))
        assertNull(RateFormat.parse("0"))
        assertNull(RateFormat.parse("0,0000001"))
        assertNull(RateFormat.parse("1,2,3"))
        assertEquals("12,5", RateFormat.format(BigDecimal("12.500")))
        assertEquals("100", RateFormat.format(BigDecimal("1E+2")))
    }
}
