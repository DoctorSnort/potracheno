package kz.chaykin.potracheno.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Часы приложения. Всё, что зависит от «сегодня», берёт его отсюда — чтобы блок
 * «Сегодня» сам обнулялся в полночь, а тесты могли подсунуть любую дату.
 */
open class AppClock {

    open fun now(): LocalDateTime = LocalDateTime.now()

    fun today(): LocalDate = now().toLocalDate()

    fun nowMillis(): Long = System.currentTimeMillis()

    /**
     * Текущая дата, обновляется в полночь. Раз в минуту перепроверяем на случай,
     * если телефон перевели на другой часовой пояс — таймер до полуночи этого не заметит.
     */
    val todayFlow: Flow<LocalDate> = flow {
        while (true) {
            val now = now()
            emit(now.toLocalDate())
            val untilMidnight = Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay())
            delay(untilMidnight.toMillis().coerceIn(1_000, RECHECK_MS))
        }
    }.distinctUntilChanged()

    private companion object {
        const val RECHECK_MS = 60_000L
    }
}
