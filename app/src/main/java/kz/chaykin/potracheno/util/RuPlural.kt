package kz.chaykin.potracheno.util

import kotlin.math.absoluteValue

/**
 * Русское склонение числительных. Не `plurals` из ресурсов: Android выбирает форму
 * по языку системы, и на английском телефоне из русских «день/дня/дней» доступны
 * только one и other — получается «7 дня». Приложение только русское, поэтому
 * правило своё и от языка телефона не зависит.
 */
object RuPlural {

    fun choose(n: Int, one: String, few: String, many: String): String {
        val abs = n.absoluteValue
        val lastTwo = abs % 100
        val last = abs % 10
        return when {
            lastTwo in 11..14 -> many
            last == 1 -> one
            last in 2..4 -> few
            else -> many
        }
    }

    fun days(n: Int): String = "$n ${choose(n, "день", "дня", "дней")}"

    fun daysShort(n: Int): String = "$n дн."
}
