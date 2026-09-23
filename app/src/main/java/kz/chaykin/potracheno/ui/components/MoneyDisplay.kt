package kz.chaykin.potracheno.ui.components

import androidx.compose.runtime.Immutable
import kz.chaykin.potracheno.domain.RateMath
import kz.chaykin.potracheno.model.Trip
import kz.chaykin.potracheno.util.MoneyFormat
import java.math.BigDecimal

/**
 * Как показывать суммы поездки: в её валюте или в рублях. Пересчёт делается
 * для каждого показанного числа отдельно — так «потрачено + осталось» в рублях
 * расходится с «пополнено» максимум на копейку, а не накапливает ошибку.
 */
@Immutable
data class MoneyDisplay(
    val symbol: String,
    /** Курс для пересчёта в рубли; null — показываем как есть. */
    private val rate: BigDecimal?,
) {
    fun convert(minor: Long): Long = if (rate == null) minor else RateMath.toRub(minor, rate)

    fun format(minor: Long): String = MoneyFormat.format(convert(minor), symbol)

    fun signed(minor: Long): String = MoneyFormat.formatSigned(convert(minor), symbol)

    fun compact(minor: Long): String = MoneyFormat.compact(convert(minor))

    companion object {
        fun of(trip: Trip, inRub: Boolean): MoneyDisplay =
            if (inRub && !trip.isRub) MoneyDisplay(Trip.RUB_SYMBOL, trip.rate) else MoneyDisplay(trip.currencySymbol, null)

        /** Всегда в валюте поездки — для долгов и конвертера. */
        fun native(trip: Trip): MoneyDisplay = MoneyDisplay(trip.currencySymbol, null)
    }
}
