package kz.chaykin.potracheno.ui.tripeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.chaykin.potracheno.data.repo.TripRepository
import kz.chaykin.potracheno.model.CoverColor
import kz.chaykin.potracheno.model.CurrencyPreset
import kz.chaykin.potracheno.model.Trip
import kz.chaykin.potracheno.ui.appContainer
import kz.chaykin.potracheno.ui.navigation.TripEditorRoute
import kz.chaykin.potracheno.util.AppClock
import kz.chaykin.potracheno.util.RateFormat
import java.math.BigDecimal
import java.time.LocalDate

data class TripEditorState(
    val isNew: Boolean = true,
    val name: String = "",
    val emoji: String = Trip.DEFAULT_EMOJI,
    val cover: CoverColor = CoverColor.Default,
    val currencyCode: String = "CNY",
    val currencySymbol: String = "¥",
    val rateText: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate = LocalDate.now().plusDays(6),
    val nameError: Boolean = false,
    val rateError: Boolean = false,
    val currencyError: Boolean = false,
    val saving: Boolean = false,
    val isSaved: Boolean = false,
) {
    val isRub: Boolean get() = currencyCode.equals(Trip.RUB_CODE, ignoreCase = true)
}

class TripEditorViewModel(
    private val tripRepository: TripRepository,
    private val tripId: Long,
    clock: AppClock,
) : ViewModel() {

    private val _state = MutableStateFlow(
        TripEditorState(
            isNew = tripId == 0L,
            startDate = clock.today(),
            endDate = clock.today().plusDays(DEFAULT_LENGTH_DAYS - 1),
        ),
    )
    val state: StateFlow<TripEditorState> = _state.asStateFlow()

    private var original: Trip? = null

    init {
        if (tripId != 0L) {
            viewModelScope.launch {
                val trip = tripRepository.get(tripId) ?: return@launch
                original = trip
                _state.update {
                    it.copy(
                        isNew = false,
                        name = trip.name,
                        emoji = trip.emoji,
                        cover = trip.cover,
                        currencyCode = trip.currencyCode,
                        currencySymbol = trip.currencySymbol,
                        rateText = if (trip.isRub) "" else RateFormat.format(trip.rate),
                        startDate = trip.startDate,
                        endDate = trip.endDate,
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value, nameError = false) }

    fun onEmojiChange(value: String) = _state.update { it.copy(emoji = value) }

    fun onCoverChange(value: CoverColor) = _state.update { it.copy(cover = value) }

    fun onPreset(preset: CurrencyPreset) = _state.update {
        it.copy(currencyCode = preset.code, currencySymbol = preset.symbol, currencyError = false, rateError = false)
    }

    fun onCodeChange(value: String) =
        _state.update { it.copy(currencyCode = value.uppercase().take(5), currencyError = false) }

    fun onSymbolChange(value: String) = _state.update { it.copy(currencySymbol = value.take(4), currencyError = false) }

    fun onRateChange(value: String) = _state.update { it.copy(rateText = value, rateError = false) }

    fun onDatesChange(start: LocalDate, end: LocalDate) =
        _state.update { it.copy(startDate = start, endDate = if (end.isBefore(start)) start else end) }

    fun save() {
        val current = _state.value
        if (current.saving) return
        val rate = if (current.isRub) BigDecimal.ONE else RateFormat.parse(current.rateText)
        val nameError = current.name.isBlank()
        val currencyError = current.currencyCode.isBlank() || current.currencySymbol.isBlank()
        val rateError = rate == null
        if (nameError || currencyError || rateError) {
            _state.update { it.copy(nameError = nameError, currencyError = currencyError, rateError = rateError) }
            return
        }

        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val id = tripRepository.save(
                Trip(
                    id = tripId,
                    name = current.name.trim(),
                    emoji = current.emoji.ifBlank { Trip.DEFAULT_EMOJI },
                    cover = current.cover,
                    currencyCode = current.currencyCode.trim().uppercase(),
                    currencySymbol = current.currencySymbol.trim(),
                    rate = requireNotNull(rate),
                    startDate = current.startDate,
                    endDate = current.endDate,
                    createdAt = original?.createdAt ?: 0L,
                ),
            )
            // Новую поездку заводят, чтобы в ней жить: сразу делаем её текущей.
            if (tripId == 0L) tripRepository.select(id)
            _state.update { it.copy(isSaved = true) }
        }
    }

    fun delete() {
        if (tripId == 0L) return
        viewModelScope.launch {
            tripRepository.delete(tripId)
            _state.update { it.copy(isSaved = true) }
        }
    }

    companion object {
        private const val DEFAULT_LENGTH_DAYS = 7L

        val Factory = viewModelFactory {
            initializer {
                val route: TripEditorRoute = createSavedStateHandle().toRoute()
                TripEditorViewModel(appContainer.tripRepository, route.tripId, appContainer.clock)
            }
        }
    }
}
