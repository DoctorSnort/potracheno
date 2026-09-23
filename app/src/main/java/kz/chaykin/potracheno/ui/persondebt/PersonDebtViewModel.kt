package kz.chaykin.potracheno.ui.persondebt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kz.chaykin.potracheno.data.repo.DebtRepository
import kz.chaykin.potracheno.data.repo.PersonRepository
import kz.chaykin.potracheno.data.repo.TripRepository
import kz.chaykin.potracheno.model.DebtEntry
import kz.chaykin.potracheno.model.Person
import kz.chaykin.potracheno.model.Trip
import kz.chaykin.potracheno.ui.appContainer
import kz.chaykin.potracheno.ui.navigation.PersonDebtRoute
import kz.chaykin.potracheno.util.AppClock

data class PersonDebtState(
    val loading: Boolean = true,
    val person: Person? = null,
    val trip: Trip? = null,
    val entries: List<DebtEntry> = emptyList(),
) {
    val balance: Long get() = entries.sumOf { it.amountMinor }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PersonDebtViewModel(
    personRepository: PersonRepository,
    tripRepository: TripRepository,
    private val debtRepository: DebtRepository,
    private val clock: AppClock,
    private val personId: Long,
) : ViewModel() {

    val state: StateFlow<PersonDebtState> = combine(
        personRepository.observe(personId),
        tripRepository.observeCurrent(),
    ) { person, trip -> person to trip }
        .flatMapLatest { (person, trip) ->
            if (trip == null) {
                flowOf(PersonDebtState(loading = false, person = person))
            } else {
                debtRepository.observeEntries(trip.id, personId).map { entries ->
                    PersonDebtState(loading = false, person = person, trip = trip, entries = entries)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonDebtState())

    /** [signedMinor]: плюс — он мне должен, минус — я ему. */
    fun addEntry(signedMinor: Long, comment: String?) {
        val trip = state.value.trip ?: return
        if (signedMinor == 0L) return
        viewModelScope.launch {
            debtRepository.addEntry(trip.id, personId, signedMinor, comment?.trim()?.ifBlank { null }, clock.now().withNano(0))
        }
    }

    fun settle(comment: String) {
        val trip = state.value.trip ?: return
        viewModelScope.launch { debtRepository.settle(trip.id, personId, comment, clock.now().withNano(0)) }
    }

    fun deleteEntry(id: Long) = viewModelScope.launch { debtRepository.deleteEntry(id) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val route: PersonDebtRoute = createSavedStateHandle().toRoute()
                PersonDebtViewModel(
                    appContainer.personRepository,
                    appContainer.tripRepository,
                    appContainer.debtRepository,
                    appContainer.clock,
                    route.personId,
                )
            }
        }
    }
}
