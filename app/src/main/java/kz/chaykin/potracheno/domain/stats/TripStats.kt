package kz.chaykin.potracheno.domain.stats

import java.time.LocalDate

enum class TripPhase {
    /** Ещё не началась. */
    UPCOMING,
    ACTIVE,
    FINISHED,
}

/** Темп трат относительно того, сколько можно тратить в день. */
enum class Pace {
    ON_TRACK,
    TIGHT,
    OVERSPENDING,
    BROKE,
}

sealed interface Forecast {
    data object AlreadyBroke : Forecast

    /** Первый день поездки — средней пока нет. */
    data object NotEnoughData : Forecast

    /** Хватит до конца, ещё и останется примерно [surplusMinor]. */
    data class LastsWholeTrip(val surplusMinor: Long) : Forecast

    /** Кончатся [date] (последний день, который ещё покрыт), не хватит на [shortDays] дней. */
    data class RunsOutOn(
        val date: LocalDate,
        val shortDays: Int,
        /** Остатка не хватает даже на сегодняшний средний день. */
        val today: Boolean = false,
    ) : Forecast
}

/**
 * Всё, что показывает карточка на главной. Суммы — в сотых валюты поездки;
 * рубли считаются уже при показе, каждое число отдельно.
 */
data class TripStats(
    val phase: TripPhase,
    val totalDays: Int,
    /** Завершённые дни до сегодняшнего — знаменатель среднедневного расхода. */
    val elapsedDays: Int,
    /** Включая сегодня. */
    val daysLeft: Int,
    val daysUntilStart: Int,
    val topUpTotal: Long,
    /** Всего потрачено: все траты, включая сегодняшние и «вне среднего». */
    val spentTotal: Long,
    val remaining: Long,
    val todaySpent: Long,
    /** Сегодняшние траты без «вне среднего» — из них считается «сегодня можно ещё». */
    val todayCounted: Long,
    val remainingAtStartOfToday: Long,
    val avgDailySpent: Long?,
    val avgDailyLeft: Long?,
    val canSpendToday: Long?,
    val pace: Pace?,
    val forecast: Forecast?,
)
