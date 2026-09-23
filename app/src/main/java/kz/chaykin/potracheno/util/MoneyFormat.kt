package kz.chaykin.potracheno.util

import kotlin.math.absoluteValue

/**
 * Деньги хранятся в целых сотых долях, а не в Double: 0.1 + 0.2 в Double не равно 0.3,
 * и на сумме трат за две недели это однажды вылезет. Сотые — для любой валюты:
 * у донга и иены дробная часть просто всегда нулевая, а [format] её прячет.
 */
object MoneyFormat {

    private const val MINOR_IN_MAJOR = 100L
    private const val NBSP = ' '

    /** «2 500 ¥», дробная часть — только если она есть: «2 500,50 ¥». Минус не теряется и у копеек. */
    fun format(minor: Long, symbol: String): String = "${amount(minor)}$NBSP$symbol"

    /** Со знаком всегда: «+1 500 ¥» / «−1 500 ¥» — для долгов и пополнений. */
    fun formatSigned(minor: Long, symbol: String): String = when {
        minor > 0 -> "+${format(minor, symbol)}"
        minor < 0 -> "−${format(-minor, symbol)}"
        else -> format(0, symbol)
    }

    /** Число без валюты: «2 500» или «2 500,50». */
    fun amount(minor: Long): String {
        val sign = if (minor < 0) "-" else ""
        val abs = minor.absoluteValue
        val major = abs / MINOR_IN_MAJOR
        val fraction = abs % MINOR_IN_MAJOR
        val grouped = groupThousands(major)
        val body = if (fraction == 0L) grouped else "$grouped,${fraction.toString().padStart(2, '0')}"
        return sign + body
    }

    /**
     * Короткая подпись для осей графиков: «950», «12,5к», «3,2м».
     * Точность здесь не нужна — нужна читаемость на ширине в полпальца.
     */
    fun compact(minor: Long): String {
        val major = minor / MINOR_IN_MAJOR
        val abs = major.absoluteValue
        val sign = if (major < 0) "-" else ""
        // Порог — с учётом округления до десятых, иначе 999 950 превращается в «1000к».
        return when {
            abs >= 999_950 -> sign + oneDecimal(abs, 1_000_000) + "м"
            abs >= 1_000 -> sign + oneDecimal(abs, 1_000) + "к"
            else -> major.toString()
        }
    }

    /** То же число для поля ввода: без разрядов и валюты, чтобы [parse] принял его обратно. */
    fun toInput(minor: Long): String {
        val sign = if (minor < 0) "-" else ""
        val abs = minor.absoluteValue
        val major = abs / MINOR_IN_MAJOR
        val fraction = abs % MINOR_IN_MAJOR
        return sign + if (fraction == 0L) major.toString() else "$major,${fraction.toString().padStart(2, '0')}"
    }

    /**
     * Разбирает то, что набрано руками: «2500», «2 500», «2500,50», «2500.5».
     * Отрицательных не бывает — знак у долгов выбирается переключателем.
     * Пустая или кривая строка — null.
     */
    fun parse(input: String): Long? {
        val cleaned = input.filterNot { it.isWhitespace() || it == NBSP }.replace(',', '.')
        if (cleaned.isEmpty()) return null
        if (!cleaned.all { it.isDigit() || it == '.' }) return null

        val parts = cleaned.split('.')
        if (parts.size > 2) return null
        if (parts[0].length > MAX_MAJOR_DIGITS) return null

        val major = parts[0].ifEmpty { "0" }.toLongOrNull() ?: return null
        val fraction = when {
            parts.size == 1 -> 0L
            else -> parts[1].take(2).padEnd(2, '0').toLongOrNull() ?: return null
        }
        return major * MINOR_IN_MAJOR + fraction
    }

    private fun oneDecimal(value: Long, unit: Long): String {
        val tenths = (value * 10 + unit / 2) / unit
        val whole = tenths / 10
        val rest = tenths % 10
        return if (rest == 0L) whole.toString() else "$whole,$rest"
    }

    private fun groupThousands(value: Long): String =
        value.toString().reversed().chunked(3).joinToString(NBSP.toString()).reversed()

    /** Двенадцать знаков — триллион донгов; больше не бывает, а переполнение Long так исключено. */
    private const val MAX_MAJOR_DIGITS = 12
}
