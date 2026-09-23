package kz.chaykin.potracheno.util

import java.math.BigDecimal

/** Курс — не деньги: бывает 0,0035 ₽ за донг, поэтому он хранится BigDecimal, а не сотыми. */
object RateFormat {

    const val MAX_SCALE = 6

    /** «12,5», «12.50», «0,0035». Ноль, минус, мусор и больше шести знаков после запятой — null. */
    fun parse(input: String): BigDecimal? {
        val cleaned = input.filterNot { it.isWhitespace() || it == ' ' }.replace(',', '.')
        if (cleaned.isEmpty() || cleaned.count { it == '.' } > 1) return null
        if (!cleaned.all { it.isDigit() || it == '.' }) return null
        val value = cleaned.toBigDecimalOrNull() ?: return null
        if (value.signum() <= 0) return null
        val normalized = value.stripTrailingZeros()
        if (normalized.scale() > MAX_SCALE) return null
        return normalized
    }

    /** Без хвостовых нулей и с запятой: «12,5». */
    fun format(rate: BigDecimal): String =
        rate.stripTrailingZeros().let { if (it.scale() < 0) it.setScale(0) else it }
            .toPlainString()
            .replace('.', ',')
}
