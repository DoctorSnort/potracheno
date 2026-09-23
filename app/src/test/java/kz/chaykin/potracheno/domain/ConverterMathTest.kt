package kz.chaykin.potracheno.domain

import kz.chaykin.potracheno.model.LocalRateDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class ConverterMathTest {

    @Test
    fun `курс «1 валюты = X ₽» берётся как есть`() {
        assertEquals(BigDecimal("12.5"), ConverterMath.rubPerUnit(BigDecimal("12.5"), LocalRateDirection.CUR_TO_RUB))
    }

    @Test
    fun `курс «1 ₽ = X валюты» переворачивается`() {
        val rub = ConverterMath.rubPerUnit(BigDecimal("0.08"), LocalRateDirection.RUB_TO_CUR)!!
        assertEquals(0, BigDecimal("12.5").compareTo(rub))
    }

    @Test
    fun `нулевой курс — не курс`() {
        assertNull(ConverterMath.rubPerUnit(BigDecimal.ZERO, LocalRateDirection.RUB_TO_CUR))
    }

    @Test
    fun `меняла просит меньше рублей — выгоднее, больше — хуже`() {
        val better = ConverterMath.gainVersusTrip(BigDecimal("12.5"), BigDecimal("12"))!!
        assertEquals(BigDecimal("4.2"), ConverterMath.percent(better))
        val worse = ConverterMath.gainVersusTrip(BigDecimal("12.5"), BigDecimal("13.5"))!!
        assertEquals(BigDecimal("-7.4"), ConverterMath.percent(worse))
    }

    @Test
    fun `пересчёт в рубли и обратно`() {
        assertEquals(1_250_000L, RateMath.toRub(100_000, BigDecimal("12.5")))
        assertEquals(100_000L, RateMath.fromRub(1_250_000, BigDecimal("12.5")))
        // 10 000 донгов по 0,0035 ₽ — 35 ₽.
        assertEquals(3_500L, RateMath.toRub(1_000_000, BigDecimal("0.0035")))
    }
}
