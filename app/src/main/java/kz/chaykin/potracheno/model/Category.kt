package kz.chaykin.potracheno.model

import androidx.annotation.StringRes
import kz.chaykin.potracheno.R

/**
 * Категории зашиты намертво: в поездке некогда вести справочник категорий,
 * а десяти хватает, чтобы пончик в отчёте что-то говорил.
 * Цвет у каждой свой, он живёт в теме — у светлой и тёмной он разный.
 */
enum class Category(val emoji: String, @StringRes val title: Int) {
    FOOD("🍜", R.string.category_food),
    CAFE("☕", R.string.category_cafe),
    TRANSPORT("🚕", R.string.category_transport),
    HOUSING("🏨", R.string.category_housing),
    FUN("🎡", R.string.category_fun),
    SHOPPING("🛍️", R.string.category_shopping),
    SOUVENIRS("🎁", R.string.category_souvenirs),
    CONNECTION("📱", R.string.category_connection),
    HEALTH("💊", R.string.category_health),
    OTHER("✨", R.string.category_other),
    ;

    companion object {
        val Default = FOOD

        fun fromNameOrDefault(name: String?): Category =
            name?.let { runCatching { valueOf(it) }.getOrNull() } ?: OTHER
    }
}
