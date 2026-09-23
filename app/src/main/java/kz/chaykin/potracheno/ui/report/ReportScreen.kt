package kz.chaykin.potracheno.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kz.chaykin.potracheno.R
import kz.chaykin.potracheno.data.prefs.SettingsStore
import kz.chaykin.potracheno.data.repo.OperationRepository
import kz.chaykin.potracheno.data.repo.TripRepository
import kz.chaykin.potracheno.domain.stats.ReportData
import kz.chaykin.potracheno.domain.stats.TripStats
import kz.chaykin.potracheno.model.Operation
import kz.chaykin.potracheno.model.Trip
import kz.chaykin.potracheno.ui.appContainer
import kz.chaykin.potracheno.ui.components.DateFormats
import kz.chaykin.potracheno.ui.components.EmptyState
import kz.chaykin.potracheno.ui.components.MoneyDisplay
import kz.chaykin.potracheno.ui.components.OperationRow
import kz.chaykin.potracheno.ui.components.charts.Bar
import kz.chaykin.potracheno.ui.components.charts.BarChart
import kz.chaykin.potracheno.ui.components.charts.DonutChart
import kz.chaykin.potracheno.ui.components.charts.DonutSlice
import kz.chaykin.potracheno.ui.components.charts.LineChart
import kz.chaykin.potracheno.ui.reportOf
import kz.chaykin.potracheno.ui.statsOf
import kz.chaykin.potracheno.ui.theme.brandColors
import kz.chaykin.potracheno.ui.theme.categoryColors

data class ReportState(
    val loading: Boolean = true,
    val trip: Trip? = null,
    val report: ReportData? = null,
    val stats: TripStats? = null,
    val top: List<Operation> = emptyList(),
    val showInRub: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModel(
    tripRepository: TripRepository,
    operationRepository: OperationRepository,
    settingsStore: SettingsStore,
    clock: kz.chaykin.potracheno.util.AppClock,
) : ViewModel() {
    val state: StateFlow<ReportState> = combine(tripRepository.observeCurrent(), settingsStore.settings) { trip, settings ->
        trip to settings.showInRub
    }.flatMapLatest { (trip, inRub) ->
        if (trip == null) {
            flowOf(ReportState(loading = false))
        } else {
            combine(operationRepository.observeByTrip(trip.id), clock.todayFlow) { operations, today ->
                ReportState(
                    loading = false,
                    trip = trip,
                    report = reportOf(trip, operations, today),
                    stats = statsOf(trip, operations, today),
                    top = operations.filter { it.isExpense }.sortedByDescending { it.amountMinor }.take(TOP_COUNT),
                    showInRub = inRub,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportState())

    companion object {
        private const val TOP_COUNT = 5

        val Factory = viewModelFactory {
            initializer {
                ReportViewModel(
                    appContainer.tripRepository,
                    appContainer.operationRepository,
                    appContainer.settingsStore,
                    appContainer.clock,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    onOpenOperation: (Long) -> Unit,
    viewModel: ReportViewModel = viewModel(factory = ReportViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.report_title)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        val trip = state.trip
        val report = state.report
        val stats = state.stats
        when {
            state.loading -> Unit
            trip == null || report == null || stats == null ->
                EmptyState("📊", stringResource(R.string.report_no_trip), Modifier.padding(padding))
            report.spentTotal == 0L && report.topUpTotal == 0L ->
                EmptyState("📊", stringResource(R.string.report_empty), Modifier.padding(padding))
            else -> ReportContent(
                trip = trip,
                report = report,
                stats = stats,
                top = state.top,
                money = MoneyDisplay.of(trip, state.showInRub),
                onOpenOperation = onOpenOperation,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportContent(
    trip: Trip,
    report: ReportData,
    stats: TripStats,
    top: List<Operation>,
    money: MoneyDisplay,
    onOpenOperation: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showRemaining by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryTile(stringResource(R.string.report_top_ups), money.format(report.topUpTotal), MaterialTheme.brandColors.positive, Modifier.weight(1f))
            SummaryTile(stringResource(R.string.report_spent), money.format(report.spentTotal), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryTile(
                stringResource(R.string.report_remaining),
                money.format(stats.remaining),
                if (stats.remaining < 0) MaterialTheme.brandColors.negative else MaterialTheme.colorScheme.secondary,
                Modifier.weight(1f),
            )
            SummaryTile(
                stringResource(R.string.report_avg_day),
                stats.avgDailySpent?.let { money.format(it) } ?: "—",
                MaterialTheme.colorScheme.tertiary,
                Modifier.weight(1f),
            )
        }

        if (report.days.isEmpty()) {
            Card {
                Text(
                    stringResource(R.string.report_upcoming),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Card {
                Text(stringResource(R.string.report_curve), style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(true, false).forEachIndexed { index, remaining ->
                        SegmentedButton(
                            selected = showRemaining == remaining,
                            onClick = { showRemaining = remaining },
                            shape = SegmentedButtonDefaults.itemShape(index, 2),
                        ) {
                            Text(stringResource(if (remaining) R.string.report_curve_remaining else R.string.report_curve_spent))
                        }
                    }
                }
                val values = report.days.map { if (showRemaining) money.convert(it.remainingEndOfDay) else money.convert(it.cumSpent) }
                LineChart(
                    values = values,
                    labels = report.days.map { DateFormats.axis(it.date) },
                    color = if (showRemaining) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    formatY = { kz.chaykin.potracheno.util.MoneyFormat.compact(it) },
                    reference = if (showRemaining) report.idealRemaining.map { money.convert(it) } else null,
                    description = stringResource(R.string.report_curve),
                )
                if (showRemaining) {
                    Hint(stringResource(R.string.report_curve_ideal))
                }
            }

            Card {
                Text(stringResource(R.string.report_days), style = MaterialTheme.typography.titleMedium)
                BarChart(
                    bars = report.days.map {
                        Bar(DateFormats.axis(it.date).substringBefore('.'), money.convert(it.counted), money.convert(it.excluded), it.isToday)
                    },
                    average = stats.avgDailySpent?.let { money.convert(it) },
                    color = MaterialTheme.colorScheme.primary,
                    description = stringResource(R.string.report_days),
                )
                Hint(stringResource(R.string.report_days_hint))
            }
        }

        if (report.categories.isNotEmpty()) {
            Card {
                Text(stringResource(R.string.report_categories), style = MaterialTheme.typography.titleMedium)
                val colors = MaterialTheme.categoryColors
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    DonutChart(
                        slices = report.categories.map { DonutSlice(it.fraction, colors[it.category]) },
                        centerTitle = stringResource(R.string.report_spent),
                        centerValue = money.format(report.spentTotal),
                        description = stringResource(R.string.report_categories),
                    )
                }
                report.categories.forEach { share ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(colors[share.category]),
                        )
                        Text("${share.category.emoji} ${stringResource(share.category.title)}", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${(share.fraction * 100).toInt().coerceAtLeast(if (share.fraction > 0f) 1 else 0)}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(money.format(share.amountMinor), style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }

        if (top.isNotEmpty()) {
            Card {
                Text(stringResource(R.string.report_top), style = MaterialTheme.typography.titleMedium)
                top.forEach { operation ->
                    OperationRow(operation = operation, money = money, onClick = { onOpenOperation(operation.id) }, showDate = true)
                }
            }
        }

        Box(modifier = Modifier.padding(bottom = 24.dp))
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

@Composable
private fun SummaryTile(label: String, value: String, accent: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(accent.copy(alpha = 0.13f))
            .padding(14.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = accent)
        Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
