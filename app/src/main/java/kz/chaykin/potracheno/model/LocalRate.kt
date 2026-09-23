package kz.chaykin.potracheno.model

/**
 * Как меняла назвал курс. Одни говорят «юань — двенадцать с половиной», другие —
 * «за рубль дам ноль восемь», поэтому направление выбирается, а не угадывается.
 */
enum class LocalRateDirection {
    /** «1 валюты = X ₽». */
    CUR_TO_RUB,

    /** «1 ₽ = X валюты». */
    RUB_TO_CUR,
}

data class LocalRate(val text: String, val direction: LocalRateDirection)
