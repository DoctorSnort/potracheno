package kz.chaykin.potracheno.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kz.chaykin.potracheno.R
import kz.chaykin.potracheno.domain.stats.Forecast
import kz.chaykin.potracheno.domain.stats.Pace
import kz.chaykin.potracheno.domain.stats.TripPhase
import kz.chaykin.potracheno.domain.stats.TripStats
import kz.chaykin.potracheno.model.Trip
import kz.chaykin.potracheno.ui.components.DateFormats
import kz.chaykin.potracheno.ui.components.GlassTile
import kz.chaykin.potracheno.ui.components.MoneyDisplay
import kz.chaykin.potracheno.ui.theme.gradient
import kz.chaykin.potracheno.util.RuPlural

/**
 * Главная карточка на градиенте обложки поездки. Всё главное — одним взглядом:
 * сколько осталось, сколько можно в день, сколько ещё сегодня, и хватит ли до конца.
 */
@Composable
fun HeroCard(
    trip: Trip,
    stats: TripStats,
    money: MoneyDisplay,
    showInRub: Boolean,
    onToggleRub: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(32.dp)
    val gradient = trip.cover.gradient()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 18.dp, shape = shape, ambientColor = gradient.last(), spotColor = gradient.first())
            .clip(shape)
            .background(Brush.linearGradient(gradient))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(if (stats.remaining < 0) R.string.home_in_minus else R.string.home_remaining),
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.weight(1f),
            )
            if (!trip.isRub) {
                CurrencyToggle(
                    currencySymbol = trip.currencySymbol,
                    inRub = showInRub,
                    onChange = onToggleRub,
                )
            }
        }

        AnimatedContent(
            targetState = money.format(stats.remaining),
            transitionSpec = {
                (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
            },
            label = "remaining",
        ) { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        SpentBar(spent = stats.spentTotal, total = stats.topUpTotal)
        Text(
            text = stringResource(R.string.home_spent_of, money.format(stats.spentTotal), money.format(stats.topUpTotal)),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f),
        )

        StatsGrid(stats, money)

        PaceLine(stats, money)
    }
}

@Composable
private fun CurrencyToggle(currencySymbol: String, inRub: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(3.dp),
    ) {
        listOf(false to currencySymbol, true to stringResource(R.string.currency_rub)).forEach { (rub, label) ->
            val selected = rub == inRub
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color(0xFF2A1454) else Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) Color.White else Color.Transparent)
                    .clickable { onChange(rub) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun SpentBar(spent: Long, total: Long) {
    val target = if (total <= 0) (if (spent > 0) 1f else 0f) else (spent.toFloat() / total).coerceIn(0f, 1f)
    val fraction by animateFloatAsState(target, label = "spent")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.22f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .clip(RoundedCornerShape(50))
                .background(Color.White),
        )
    }
}

@Composable
private fun StatsGrid(stats: TripStats, money: MoneyDisplay) {
    val dash = "—"
    val upcoming = stats.phase == TripPhase.UPCOMING
    val finished = stats.phase == TripPhase.FINISHED

    val daysLabel = stringResource(if (upcoming) R.string.home_days_until_start else R.string.home_days_left)
    val daysValue = if (upcoming) {
        RuPlural.days(stats.daysUntilStart)
    } else {
        RuPlural.days(stats.daysLeft)
    }

    val avgSpent = stats.avgDailySpent
    val avgLeft = stats.avgDailyLeft
    val canToday = stats.canSpendToday

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(daysLabel, daysValue, Modifier.weight(1f))
            StatTile(
                label = stringResource(R.string.home_avg_spent),
                value = avgSpent?.let { money.format(it) } ?: dash,
                hint = if (avgSpent == null && !upcoming) stringResource(R.string.home_avg_tomorrow) else null,
                modifier = Modifier.weight(1f),
            )
        }
        if (!finished) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(
                    label = stringResource(if (upcoming) R.string.home_budget_per_day else R.string.home_avg_left),
                    value = avgLeft?.let { money.format(it) } ?: dash,
                    modifier = Modifier.weight(1f),
                )
                if (!upcoming) {
                    StatTile(
                        label = stringResource(
                            if (canToday != null && canToday < 0) R.string.home_over_today else R.string.home_can_today,
                        ),
                        value = canToday?.let { money.format(kotlin.math.abs(it)) } ?: dash,
                        emphasize = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    emphasize: Boolean = false,
) {
    GlassTile(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (emphasize) FontWeight.Black else FontWeight.Bold,
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (hint != null) {
                Text(hint, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun PaceLine(stats: TripStats, money: MoneyDisplay) {
    if (stats.phase == TripPhase.FINISHED) {
        Text(
            text = "🏁 " + stringResource(R.string.home_trip_finished),
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
        )
        return
    }
    // Свежая поездка без денег — это не «деньги кончились», а «ещё не начали».
    if (stats.topUpTotal == 0L && stats.spentTotal == 0L) {
        Text(
            text = "💡 " + stringResource(R.string.home_start_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
        )
        return
    }
    val pace = stats.pace
    val forecast = stats.forecast ?: return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (pace != null) {
            val (emoji, label) = when (pace) {
                Pace.ON_TRACK -> "🟢" to R.string.pace_on_track
                Pace.TIGHT -> "🟡" to R.string.pace_tight
                Pace.OVERSPENDING -> "🔴" to R.string.pace_overspending
                Pace.BROKE -> "💸" to R.string.pace_broke
            }
            Text(
                text = "$emoji ${stringResource(label)}",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF2A1454),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        Text(
            text = forecastText(forecast, money),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun forecastText(forecast: Forecast, money: MoneyDisplay): String = when (forecast) {
    Forecast.AlreadyBroke -> stringResource(R.string.forecast_broke)
    Forecast.NotEnoughData -> stringResource(R.string.forecast_not_enough)
    is Forecast.LastsWholeTrip -> stringResource(R.string.forecast_whole_trip, money.format(forecast.surplusMinor))
    is Forecast.RunsOutOn -> if (forecast.today) {
        stringResource(R.string.forecast_runs_out_today)
    } else {
        stringResource(
            R.string.forecast_runs_out,
            DateFormats.short(forecast.date),
            RuPlural.daysShort(forecast.shortDays),
        )
    }
}
