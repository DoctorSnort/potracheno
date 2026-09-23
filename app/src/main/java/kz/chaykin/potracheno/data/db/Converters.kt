package kz.chaykin.potracheno.data.db

import androidx.room.TypeConverter
import kz.chaykin.potracheno.model.Category
import kz.chaykin.potracheno.model.CoverColor
import kz.chaykin.potracheno.model.OperationType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Даты и время пишутся строками фиксированного формата, а не миллисекундами:
 * у трат нет часового пояса (см. [kz.chaykin.potracheno.model.Operation.occurredAt]),
 * а строка «2026-09-23T14:05:00» ещё и сортируется как время.
 */
class Converters {
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal): String = value.toPlainString()

    @TypeConverter
    fun toBigDecimal(value: String): BigDecimal = BigDecimal(value)

    @TypeConverter
    fun fromLocalDate(value: LocalDate): String = value.format(DateTimeFormatter.ISO_LOCAL_DATE)

    @TypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime): String = formatDateTime(value)

    @TypeConverter
    fun toLocalDateTime(value: String): LocalDateTime = parseDateTime(value)

    @TypeConverter
    fun fromOperationType(value: OperationType): String = value.name

    @TypeConverter
    fun toOperationType(value: String): OperationType =
        runCatching { OperationType.valueOf(value) }.getOrDefault(OperationType.EXPENSE)

    @TypeConverter
    fun fromCategory(value: Category?): String? = value?.name

    @TypeConverter
    fun toCategory(value: String?): Category? = value?.let { Category.fromNameOrDefault(it) }

    @TypeConverter
    fun fromCover(value: CoverColor): String = value.name

    @TypeConverter
    fun toCover(value: String): CoverColor = CoverColor.fromNameOrDefault(value)

    companion object {
        /** Секунды пишутся всегда: иначе «14:05» и «14:05:30» сортировались бы криво. */
        private val DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        fun formatDateTime(value: LocalDateTime): String = value.withNano(0).format(DATE_TIME)

        fun parseDateTime(value: String): LocalDateTime = LocalDateTime.parse(value, DATE_TIME)
    }
}
