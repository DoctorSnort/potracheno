package kz.chaykin.potracheno.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Пересчёт по курсу «₽ за 1 единицу валюты». И суммы в валюте, и рубли хранятся
 * в сотых, поэтому формула прямая: сотые × курс = сотые рубля.
 */
object RateMath {

    fun toRub(minor: Long, rate: BigDecimal): Long =
        BigDecimal.valueOf(minor).multiply(rate).setScale(0, RoundingMode.HALF_UP).toLong()

    fun fromRub(rubMinor: Long, rate: BigDecimal): Long {
        if (rate.signum() == 0) return 0
        return BigDecimal.valueOf(rubMinor).divide(rate, 0, RoundingMode.HALF_UP).toLong()
    }
}
