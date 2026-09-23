package kz.chaykin.potracheno.model

/** Обложка поездки — градиент карточки на главной. Сами цвета — в теме. */
enum class CoverColor {
    SUNSET,
    GRAPE,
    OCEAN,
    MINT,
    LEMON,
    CHERRY,
    NIGHT,
    LIME,
    ;

    companion object {
        val Default = GRAPE

        fun fromNameOrDefault(name: String?): CoverColor =
            name?.let { runCatching { valueOf(it) }.getOrNull() } ?: Default
    }
}
