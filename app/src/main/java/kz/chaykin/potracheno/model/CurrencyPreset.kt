package kz.chaykin.potracheno.model

/** Быстрый выбор валюты в редакторе поездки. Любую другую можно вписать руками. */
data class CurrencyPreset(val code: String, val symbol: String, val flag: String)

val CurrencyPresets: List<CurrencyPreset> = listOf(
    CurrencyPreset("CNY", "¥", "🇨🇳"),
    CurrencyPreset("THB", "฿", "🇹🇭"),
    CurrencyPreset("TRY", "₺", "🇹🇷"),
    CurrencyPreset("VND", "₫", "🇻🇳"),
    CurrencyPreset("USD", "$", "🇺🇸"),
    CurrencyPreset("EUR", "€", "🇪🇺"),
    CurrencyPreset("KZT", "₸", "🇰🇿"),
    CurrencyPreset("AED", "د.إ", "🇦🇪"),
    CurrencyPreset("GEL", "₾", "🇬🇪"),
    CurrencyPreset("JPY", "¥", "🇯🇵"),
    CurrencyPreset("KRW", "₩", "🇰🇷"),
    CurrencyPreset("IDR", "Rp", "🇮🇩"),
    CurrencyPreset("RUB", "₽", "🇷🇺"),
)
