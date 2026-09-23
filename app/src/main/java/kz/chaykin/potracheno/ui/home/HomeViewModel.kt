package kz.chaykin.potracheno.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kz.chaykin.potracheno.data.prefs.SettingsStore
import kz.chaykin.potracheno.data.repo.OperationRepository
import kz.chaykin.potracheno.data.repo.TripRepository
import kz.chaykin.potracheno.domain.stats.TripStats
import kz.chaykin.potracheno.model.Operation
import kz.chaykin.potracheno.model.Trip
import kz.chaykin.potracheno.ui.appContainer
import kz.chaykin.potracheno.ui.statsOf
import kz.chaykin.potracheno.util.AppClock

data class HomeState(
    val loading: Boolean = true,
    val trip: Trip? = null,
    val trips: List<Trip> = emptyList(),
    val stats: TripStats? = null,
    val today: List<Operation> = emptyList(),
    val showInRub: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val tripRepository: TripRepository,
    operationRepository: OperationRepository,
    private val settingsStore: SettingsStore,
    clock: AppClock,
) : ViewModel() {

    val state: StateFlow<HomeState> = combine(
        tripRepository.observeCurrent(),
        tripRepository.observeAll(),
        settingsStore.settings,
    ) { trip, trips, settings -> Triple(trip, trips, settings.showInRub) }
        .flatMapLatest { (trip, trips, showInRub) ->
            if (trip == null) {
                flowOf(HomeState(loading = false, trips = trips, showInRub = showInRub))
            } else {
                combine(operationRepository.observeByTrip(trip.id), clock.todayFlow) { operations, today ->
                    HomeState(
                        loading = false,
                        trip = trip,
                        trips = trips,
                        stats = statsOf(trip, operations, today),
                        today = operations.filter { it.occurredAt.toLocalDate() == today },
                        showInRub = showInRub,
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun selectTrip(id: Long) = viewModelScope.launch { tripRepository.select(id) }

    fun setShowInRub(enabled: Boolean) = viewModelScope.launch { settingsStore.setShowInRub(enabled) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    appContainer.tripRepository,
                    appContainer.operationRepository,
                    appContainer.settingsStore,
                    appContainer.clock,
                )
            }
        }
    }
}
