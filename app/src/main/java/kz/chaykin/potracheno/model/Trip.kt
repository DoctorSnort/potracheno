package kz.chaykin.potracheno.model

import java.math.BigDecimal
import java.time.LocalDate

data class Trip(
    val id: Long = 0,
    val name: String,
    val emoji: String = DEFAULT_EMOJI,
    val cover: CoverColor = CoverColor.Default,
    val currencyCode: String,
    val currencySymbol: String,
    /** Сколько рублей стоит одна единица валюты поездки. Один курс на всю поездку. */
    val rate: BigDecimal,
    val startDate: LocalDate,
    /** Включительно. */
    val endDate: LocalDate,
    val createdAt: Long = 0,
) {
    /** Рублёвой поездке пересчитывать нечего — переключатель валют и конвертер прячутся. */
    val isRub: Boolean get() = currencyCode == RUB_CODE

    companion object {
        const val DEFAULT_EMOJI = "✈️"
        const val RUB_CODE = "RUB"
        const val RUB_SYMBOL = "₽"
    }
}
