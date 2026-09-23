package kz.chaykin.potracheno.ui.converter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.chaykin.potracheno.R
import kz.chaykin.potracheno.data.prefs.SettingsStore
import kz.chaykin.potracheno.data.repo.TripRepository
import kz.chaykin.potracheno.domain.ConverterMath
import kz.chaykin.potracheno.model.LocalRate
import kz.chaykin.potracheno.model.LocalRateDirection
import kz.chaykin.potracheno.model.Trip
import kz.chaykin.potracheno.ui.appContainer
import kz.chaykin.potracheno.ui.components.EmptyState
import kz.chaykin.potracheno.ui.components.MoneyField
import kz.chaykin.potracheno.ui.theme.brandColors
import kz.chaykin.potracheno.ui.theme.gradient
import kz.chaykin.potracheno.util.MoneyFormat
import kz.chaykin.potracheno.util.RateFormat
import java.math.BigDecimal
import java.math.RoundingMode

/** Пара связанных полей: пишешь в одно — пересчитывается другое. */
data class PairFields(val currency: String = "", val rub: String = "")

data class ConverterState(
    val loading: Boolean = true,
    val trip: Trip? = null,
    val tripPair: PairFields = PairFields(),
    val localRateText: String = "",
    val localDirection: LocalRateDirection = LocalRateDirection.CUR_TO_RUB,
    val localPair: PairFields = PairFields(),
) {
    val localRubPerUnit: BigDecimal?
        get() = RateFormat.parse(localRateText)?.let { ConverterMath.rubPerUnit(it, localDirection) }

    /** Доля выгоды относительно курса поездки; null — сравнивать не с чем. */
    val gain: BigDecimal?
        get() {
            val trip = trip ?: return null
            val local = localRubPerUnit ?: return null
            return ConverterMath.gainVersusTrip(trip.rate, local)
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ConverterViewModel(
    tripRepository: TripRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val fields = MutableStateFlow(ConverterFields())

    private data class ConverterFields(
        val tripPair: PairFields = PairFields(),
        val localPair: PairFields = PairFields(),
        /** Поле, которое правили последним, — ведущее: второе пересчитывается от него. */
        val tripLeadRub: Boolean = false,
        val localLeadRub: Boolean = true,
        val local: LocalRate = LocalRate("", LocalRateDirection.CUR_TO_RUB),
    )

    private var currentTripId: Long? = null

    /**
     * Курс менялы живёт в памяти, а в настройки только сохраняется: если гонять набираемый
     * текст через DataStore и обратно, курсор в поле начинает скакать.
     */
    val state: StateFlow<ConverterState> = tripRepository.observeCurrent().flatMapLatest { trip ->
        currentTripId = trip?.id
        if (trip == null) {
            flowOf(ConverterState(loading = false))
        } else {
            val saved = settingsStore.localRate(trip.id).first() ?: LocalRate("", LocalRateDirection.CUR_TO_RUB)
            fields.update { it.copy(local = saved, localPair = PairFields()) }
            fields.map { f ->
                val base = ConverterState(
                    loading = false,
                    trip = trip,
                    localRateText = f.local.text,
                    localDirection = f.local.direction,
                )
                base.copy(
                    tripPair = recalc(f.tripPair, f.tripLeadRub, trip.rate),
                    localPair = recalc(f.localPair, f.localLeadRub, base.localRubPerUnit),
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConverterState())

    fun onTripCurrency(value: String) = fields.update { it.copy(tripPair = it.tripPair.copy(currency = value), tripLeadRub = false) }

    fun onTripRub(value: String) = fields.update { it.copy(tripPair = it.tripPair.copy(rub = value), tripLeadRub = true) }

    fun onLocalCurrency(value: String) = fields.update { it.copy(localPair = it.localPair.copy(currency = value), localLeadRub = false) }

    fun onLocalRub(value: String) = fields.update { it.copy(localPair = it.localPair.copy(rub = value), localLeadRub = true) }

    fun onLocalRate(text: String) = saveLocal { it.copy(text = text) }

    fun onLocalDirection(direction: LocalRateDirection) = saveLocal { it.copy(direction = direction) }

    private fun saveLocal(change: (LocalRate) -> LocalRate) {
        fields.update { it.copy(local = change(it.local)) }
        val tripId = currentTripId ?: return
        val local = fields.value.local
        viewModelScope.launch { settingsStore.setLocalRate(tripId, local) }
    }

    /** Пересчитывает ведомое поле из ведущего по курсу «₽ за единицу». */
    private fun recalc(pair: PairFields, leadRub: Boolean, rubPerUnit: BigDecimal?): PairFields {
        if (rubPerUnit == null || rubPerUnit.signum() <= 0) return if (leadRub) pair.copy(currency = "") else pair.copy(rub = "")
        return if (leadRub) {
            val rub = MoneyFormat.parse(pair.rub) ?: return pair.copy(currency = "")
            val cur = BigDecimal.valueOf(rub).divide(rubPerUnit, 0, RoundingMode.HALF_UP).toLong()
            pair.copy(currency = MoneyFormat.amount(cur))
        } else {
            val cur = MoneyFormat.parse(pair.currency) ?: return pair.copy(rub = "")
            val rub = BigDecimal.valueOf(cur).multiply(rubPerUnit).setScale(0, RoundingMode.HALF_UP).toLong()
            pair.copy(rub = MoneyFormat.amount(rub))
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ConverterViewModel(appContainer.tripRepository, appContainer.settingsStore) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(viewModel: ConverterViewModel = viewModel(factory = ConverterViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.converter_title)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        val trip = state.trip
        when {
            state.loading -> Unit
            trip == null -> EmptyState("💱", stringResource(R.string.converter_no_trip), Modifier.padding(padding))
            trip.isRub -> EmptyState("🇷🇺", stringResource(R.string.converter_rub_trip), Modifier.padding(padding))
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TripRateCard(state, trip, viewModel)
                LocalRateCard(state, trip, viewModel)
            }
        }
    }
}

@Composable
private fun TripRateCard(state: ConverterState, trip: Trip, viewModel: ConverterViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(trip.cover.gradient()))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.converter_trip_rate), style = MaterialTheme.typography.titleMedium, color = Color.White)
        Text(
            stringResource(R.string.converter_trip_rate_value, trip.currencySymbol, RateFormat.format(trip.rate)),
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
        )
        Column(
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MoneyField(
                value = state.tripPair.currency,
                onValueChange = viewModel::onTripCurrency,
                label = trip.currencyCode,
                suffix = trip.currencySymbol,
                large = true,
            )
            Icon(Icons.Default.SwapVert, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            MoneyField(
                value = state.tripPair.rub,
                onValueChange = viewModel::onTripRub,
                label = "RUB",
                suffix = "₽",
                large = true,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalRateCard(state: ConverterState, trip: Trip, viewModel: ConverterViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("🧔 " + stringResource(R.string.converter_local), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.converter_local_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf(LocalRateDirection.CUR_TO_RUB, LocalRateDirection.RUB_TO_CUR).forEachIndexed { index, direction ->
                SegmentedButton(
                    selected = state.localDirection == direction,
                    onClick = { viewModel.onLocalDirection(direction) },
                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                ) {
                    Text(
                        stringResource(
                            if (direction == LocalRateDirection.CUR_TO_RUB) R.string.converter_dir_cur_to_rub else R.string.converter_dir_rub_to_cur,
                            trip.currencySymbol,
                        ),
                    )
                }
            }
        }
        MoneyField(
            value = state.localRateText,
            onValueChange = viewModel::onLocalRate,
            label = stringResource(R.string.converter_local_rate),
            suffix = if (state.localDirection == LocalRateDirection.CUR_TO_RUB) "₽" else trip.currencySymbol,
        )

        state.gain?.let { gain -> GainLine(gain) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            MoneyField(
                value = state.localPair.rub,
                onValueChange = viewModel::onLocalRub,
                label = "RUB",
                suffix = "₽",
                modifier = Modifier.weight(1f),
            )
            Text("→", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            MoneyField(
                value = state.localPair.currency,
                onValueChange = viewModel::onLocalCurrency,
                label = trip.currencyCode,
                suffix = trip.currencySymbol,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GainLine(gain: BigDecimal) {
    val percent = ConverterMath.percent(gain)
    val (text, color) = when {
        percent.signum() > 0 -> stringResource(R.string.converter_better, percent.toPlainString().replace('.', ',')) to
            MaterialTheme.brandColors.positive
        percent.signum() < 0 -> stringResource(R.string.converter_worse, percent.negate().toPlainString().replace('.', ',')) to
            MaterialTheme.brandColors.negative
        else -> stringResource(R.string.converter_same) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(color.copy(alpha = 0.14f))
            .padding(12.dp),
    ) {
        Text(
            (if (percent.signum() > 0) "👍 " else if (percent.signum() < 0) "👎 " else "🤝 ") + text,
            style = MaterialTheme.typography.titleSmall,
            color = color,
        )
        Text(
            stringResource(R.string.converter_compare_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
