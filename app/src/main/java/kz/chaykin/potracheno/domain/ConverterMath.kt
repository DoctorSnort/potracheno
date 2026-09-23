package kz.chaykin.potracheno.domain

import kz.chaykin.potracheno.model.LocalRateDirection
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

object ConverterMath {

    private const val SCALE = 10

    /**
     * Приводит курс менялы к виду «₽ за 1 единицу», как у поездки.
     * Ноль или минус — не курс, тогда null.
     */
    fun rubPerUnit(value: BigDecimal, direction: LocalRateDirection): BigDecimal? {
        if (value.signum() <= 0) return null
        return when (direction) {
            LocalRateDirection.CUR_TO_RUB -> value
            LocalRateDirection.RUB_TO_CUR -> BigDecimal.ONE.divide(value, SCALE, RoundingMode.HALF_UP)
        }
    }

    /**
     * Насколько курс менялы лучше курса поездки, если меняешь рубли на валюту.
     * Плюс — выгоднее (за ту же валюту просят меньше рублей), минус — хуже.
     * Возвращает долю: 0.042 — это 4,2 %.
     */
    fun gainVersusTrip(tripRubPerUnit: BigDecimal, localRubPerUnit: BigDecimal): BigDecimal? {
        if (tripRubPerUnit.signum() <= 0 || localRubPerUnit.signum() <= 0) return null
        return tripRubPerUnit.divide(localRubPerUnit, MathContext.DECIMAL64).subtract(BigDecimal.ONE)
    }

    /** Проценты с одним знаком: 0.04166 → 4.2. */
    fun percent(fraction: BigDecimal): BigDecimal =
        fraction.movePointRight(2).setScale(1, RoundingMode.HALF_UP)
}
