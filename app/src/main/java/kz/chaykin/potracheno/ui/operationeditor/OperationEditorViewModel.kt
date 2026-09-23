package kz.chaykin.potracheno.ui.operationeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.chaykin.potracheno.data.repo.OperationRepository
import kz.chaykin.potracheno.data.repo.PersonRepository
import kz.chaykin.potracheno.data.repo.TripRepository
import kz.chaykin.potracheno.model.Category
import kz.chaykin.potracheno.model.DebtShare
import kz.chaykin.potracheno.model.Operation
import kz.chaykin.potracheno.model.OperationType
import kz.chaykin.potracheno.model.Person
import kz.chaykin.potracheno.model.Trip
import kz.chaykin.potracheno.ui.appContainer
import kz.chaykin.potracheno.ui.navigation.OperationEditorRoute
import kz.chaykin.potracheno.util.AppClock
import kz.chaykin.potracheno.util.MoneyFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Строка делёжки: за кого и сколько. [key] — чтобы Compose не путал строки при удалении. */
data class SplitRow(val key: Long, val personId: Long? = null, val amountText: String = "")

data class OperationEditorState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val trip: Trip? = null,
    val type: OperationType = OperationType.EXPENSE,
    val amountText: String = "",
    val category: Category = Category.Default,
    val comment: String = "",
    val occurredAt: LocalDateTime = LocalDateTime.now().withNano(0),
    val excludeFromDaily: Boolean = false,
    val splits: List<SplitRow> = emptyList(),
    val amountError: Boolean = false,
    val splitError: SplitError? = null,
    /** Сохранение уже идёт: второй тап по кнопке не должен завести дубль. */
    val saving: Boolean = false,
    val isSaved: Boolean = false,
) {
    val isExpense: Boolean get() = type == OperationType.EXPENSE

    val paidMinor: Long? get() = MoneyFormat.parse(amountText)

    /** Живой итог «моя доля»: пока что-то не распарсилось, считаем это нулём. */
    val myShareMinor: Long? get() {
        val paid = paidMinor ?: return null
        return paid - splits.sumOf { MoneyFormat.parse(it.amountText) ?: 0L }
    }
}

enum class SplitError { TOO_MUCH, INVALID }

class OperationEditorViewModel(
    private val operationRepository: OperationRepository,
    private val tripRepository: TripRepository,
    private val personRepository: PersonRepository,
    private val clock: AppClock,
    private val operationId: Long,
    initialType: OperationType,
) : ViewModel() {

    private val _state = MutableStateFlow(
        OperationEditorState(isNew = operationId == 0L, type = initialType, occurredAt = clock.now().withNano(0)),
    )
    val state: StateFlow<OperationEditorState> = _state.asStateFlow()

    val persons: StateFlow<List<Person>> = personRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var original: Operation? = null
    private var nextKey = 1L

    init {
        viewModelScope.launch {
            if (operationId == 0L) {
                val trip = tripRepository.observeCurrent().first()
                _state.update { it.copy(loading = false, trip = trip) }
            } else {
                val operation = operationRepository.get(operationId)
                if (operation == null) {
                    // Операцию уже удалили (вместе с поездкой или импортом) — редактировать нечего.
                    _state.update { it.copy(isSaved = true) }
                    return@launch
                }
                original = operation
                val trip = tripRepository.get(operation.tripId)
                val shares = operationRepository.sharesOf(operationId)
                _state.update {
                    it.copy(
                        loading = false,
                        isNew = false,
                        trip = trip,
                        type = operation.type,
                        amountText = MoneyFormat.toInput(operation.paidMinor ?: operation.amountMinor),
                        category = operation.category ?: Category.Default,
                        comment = operation.comment.orEmpty(),
                        occurredAt = operation.occurredAt,
                        excludeFromDaily = operation.excludeFromDaily,
                        splits = shares.map { share ->
                            SplitRow(key = nextKey++, personId = share.personId, amountText = MoneyFormat.toInput(share.amountMinor))
                        },
                    )
                }
            }
        }
    }

    fun onTypeChange(type: OperationType) = _state.update { it.copy(type = type, splitError = null) }

    fun onAmountChange(value: String) = _state.update { it.copy(amountText = value, amountError = false, splitError = null) }

    fun onCategoryChange(value: Category) = _state.update { it.copy(category = value) }

    fun onCommentChange(value: String) = _state.update { it.copy(comment = value) }

    fun onDateChange(date: LocalDate) = _state.update { it.copy(occurredAt = LocalDateTime.of(date, it.occurredAt.toLocalTime())) }

    fun onTimeChange(time: LocalTime) = _state.update { it.copy(occurredAt = LocalDateTime.of(it.occurredAt.toLocalDate(), time)) }

    fun onExcludeChange(value: Boolean) = _state.update { it.copy(excludeFromDaily = value) }

    fun addSplit() = _state.update { it.copy(splits = it.splits + SplitRow(key = nextKey++), splitError = null) }

    fun removeSplit(key: Long) = _state.update { it.copy(splits = it.splits.filterNot { row -> row.key == key }, splitError = null) }

    fun onSplitPerson(key: Long, personId: Long) = updateSplit(key) { it.copy(personId = personId) }

    fun onSplitAmount(key: Long, value: String) = updateSplit(key) { it.copy(amountText = value) }

    /** Поровну на меня и всех, за кого платил. Остаток от деления остаётся на мне. */
    fun splitEqually() = _state.update { current ->
        val paid = current.paidMinor ?: return@update current.copy(amountError = true)
        if (current.splits.isEmpty()) return@update current
        val each = paid / (current.splits.size + 1)
        current.copy(
            splits = current.splits.map { it.copy(amountText = MoneyFormat.toInput(each)) },
            splitError = null,
        )
    }

    /** Новый человек прямо из траты — без фото, его можно добавить потом на экране долгов. */
    fun createPerson(name: String, forSplitKey: Long) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val id = personRepository.save(Person(name = trimmed))
            onSplitPerson(forSplitKey, id)
        }
    }

    fun save() {
        val current = _state.value
        if (current.saving) return
        val trip = current.trip ?: return
        val paid = current.paidMinor
        if (paid == null || paid <= 0L) {
            _state.update { it.copy(amountError = true) }
            return
        }

        val shares = if (current.isExpense) {
            val parsed = current.splits.map { row -> row.personId to MoneyFormat.parse(row.amountText) }
            val valid = parsed.all { (person, amount) -> person != null && amount != null && amount > 0 } &&
                parsed.map { it.first }.toSet().size == parsed.size
            if (!valid) {
                _state.update { it.copy(splitError = SplitError.INVALID) }
                return
            }
            val result = parsed.map { (person, amount) -> DebtShare(requireNotNull(person), requireNotNull(amount)) }
            if (result.sumOf { it.amountMinor } > paid) {
                _state.update { it.copy(splitError = SplitError.TOO_MUCH) }
                return
            }
            result
        } else {
            emptyList()
        }

        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val operation = Operation(
                id = operationId,
                tripId = trip.id,
                type = current.type,
                amountMinor = paid,
                category = if (current.isExpense) current.category else null,
                comment = current.comment.trim().ifBlank { null },
                occurredAt = current.occurredAt,
                excludeFromDaily = current.isExpense && current.excludeFromDaily,
                createdAt = original?.createdAt ?: 0L,
            )
            if (current.isExpense) {
                operationRepository.saveExpense(operation, paid, shares)
            } else {
                operationRepository.saveTopUp(operation)
            }
            _state.update { it.copy(isSaved = true) }
        }
    }

    fun delete() {
        if (operationId == 0L) return
        viewModelScope.launch {
            operationRepository.delete(operationId)
            _state.update { it.copy(isSaved = true) }
        }
    }

    private fun updateSplit(key: Long, change: (SplitRow) -> SplitRow) = _state.update { current ->
        current.copy(splits = current.splits.map { if (it.key == key) change(it) else it }, splitError = null)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val route: OperationEditorRoute = createSavedStateHandle().toRoute()
                OperationEditorViewModel(
                    appContainer.operationRepository,
                    appContainer.tripRepository,
                    appContainer.personRepository,
                    appContainer.clock,
                    route.operationId,
                    route.type,
                )
            }
        }
    }
}
