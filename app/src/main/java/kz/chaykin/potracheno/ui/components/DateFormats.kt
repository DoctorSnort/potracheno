package kz.chaykin.potracheno.ui.components

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Форматы дат по-русски: «23 сент», «23 сентября, ср», «14:05». */
object DateFormats {
    private val ru = Locale.forLanguageTag("ru")

    private val shortDate = DateTimeFormatter.ofPattern("d MMM", ru)
    private val longDate = DateTimeFormatter.ofPattern("d MMMM, EE", ru)
    private val time = DateTimeFormatter.ofPattern("HH:mm", ru)
    private val dayMonth = DateTimeFormatter.ofPattern("d.MM", ru)

    fun short(date: LocalDate): String = date.format(shortDate).trimEnd('.')

    fun long(date: LocalDate): String = date.format(longDate)

    fun time(dateTime: LocalDateTime): String = dateTime.format(time)

    /** Подписи оси X графиков: «23.09». */
    fun axis(date: LocalDate): String = date.format(dayMonth)

    fun shortWithTime(dateTime: LocalDateTime): String = "${short(dateTime.toLocalDate())}, ${time(dateTime)}"

    /**
     * DatePicker из Material 3 отдаёт полночь по UTC. Переводить только через UTC:
     * через местную зону в Америке дата съехала бы на день назад.
     */
    fun fromPickerMillis(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

    fun toPickerMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
