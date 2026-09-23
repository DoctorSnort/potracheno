package kz.chaykin.potracheno.ui.operations

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kz.chaykin.potracheno.R
import kz.chaykin.potracheno.data.prefs.SettingsStore
import kz.chaykin.potracheno.data.repo.OperationRepository
import kz.chaykin.potracheno.data.repo.TripRepository
import kz.chaykin.potracheno.model.Operation
import kz.chaykin.potracheno.model.Trip
import kz.chaykin.potracheno.ui.appContainer
import kz.chaykin.potracheno.ui.components.DateFormats
import kz.chaykin.potracheno.ui.components.EmptyState
import kz.chaykin.potracheno.ui.components.MoneyDisplay
import kz.chaykin.potracheno.ui.components.OperationRow
import java.time.LocalDate

data class OperationsState(
    val trip: Trip? = null,
    val days: List<Pair<LocalDate, List<Operation>>> = emptyList(),
    val showInRub: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class OperationsViewModel(
    tripRepository: TripRepository,
    operationRepository: OperationRepository,
    settingsStore: SettingsStore,
) : ViewModel() {
    val state: StateFlow<OperationsState> = combine(tripRepository.observeCurrent(), settingsStore.settings) { trip, settings ->
        trip to settings.showInRub
    }.flatMapLatest { (trip, inRub) ->
        if (trip == null) {
            flowOf(OperationsState())
        } else {
            operationRepository.observeByTrip(trip.id).map { operations ->
                OperationsState(
                    trip = trip,
                    // Операции уже идут от новых к старым — группировка сохраняет порядок.
                    days = operations.groupBy { it.occurredAt.toLocalDate() }.toList(),
                    showInRub = inRub,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OperationsState())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                OperationsViewModel(appContainer.tripRepository, appContainer.operationRepository, appContainer.settingsStore)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OperationsScreen(
    onBack: () -> Unit,
    onOpenOperation: (Long) -> Unit,
    onAdd: () -> Unit,
    viewModel: OperationsViewModel = viewModel(factory = OperationsViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.operations_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add))
            }
        },
    ) { padding ->
        val trip = state.trip
        if (trip == null || state.days.isEmpty()) {
            EmptyState(emoji = "🧾", title = stringResource(R.string.operations_empty), modifier = Modifier.padding(padding))
            return@Scaffold
        }
        val money = MoneyDisplay.of(trip, state.showInRub)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp),
        ) {
            state.days.forEach { (date, operations) ->
                stickyHeader(key = "h$date") {
                    val spent = operations.filter { it.isExpense }.sumOf { it.amountMinor }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                    ) {
                        Text(
                            DateFormats.long(date),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                        if (spent > 0) {
                            Text(
                                stringResource(R.string.operations_day_total, money.format(spent)),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                items(operations, key = { it.id }) { operation ->
                    OperationRow(operation = operation, money = money, onClick = { onOpenOperation(operation.id) })
                }
            }
        }
    }
}
