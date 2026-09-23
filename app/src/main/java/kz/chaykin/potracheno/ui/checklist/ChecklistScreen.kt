package kz.chaykin.potracheno.ui.checklist

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kz.chaykin.potracheno.R
import kz.chaykin.potracheno.data.repo.ChecklistRepository
import kz.chaykin.potracheno.data.repo.TripRepository
import kz.chaykin.potracheno.model.ChecklistItem
import kz.chaykin.potracheno.model.Trip
import kz.chaykin.potracheno.ui.appContainer
import kz.chaykin.potracheno.ui.components.EmptyState

data class ChecklistState(
    val loading: Boolean = true,
    val trip: Trip? = null,
    val items: List<ChecklistItem> = emptyList(),
) {
    val doneCount: Int get() = items.count { it.done }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChecklistViewModel(
    tripRepository: TripRepository,
    private val checklistRepository: ChecklistRepository,
) : ViewModel() {

    val state: StateFlow<ChecklistState> = tripRepository.observeCurrent().flatMapLatest { trip ->
        checklistRepository.observeVisible(trip?.id).map { ChecklistState(loading = false, trip = trip, items = it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChecklistState())

    /** Новый пункт — про текущую поездку; без поездки — «всегда». */
    fun add(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { checklistRepository.add(trimmed, state.value.trip?.id) }
    }

    fun toggle(item: ChecklistItem) = viewModelScope.launch { checklistRepository.setDone(item.id, !item.done) }

    fun toggleScope(item: ChecklistItem) {
        val tripId = state.value.trip?.id ?: return
        viewModelScope.launch { checklistRepository.setScope(item.id, if (item.isUniversal) tripId else null) }
    }

    fun delete(item: ChecklistItem) = viewModelScope.launch { checklistRepository.delete(item.id) }

    fun restore(item: ChecklistItem) = viewModelScope.launch { checklistRepository.restore(item) }

    fun clearDone() = viewModelScope.launch { checklistRepository.clearDone(state.value.trip?.id) }

    companion object {
        val Factory = viewModelFactory {
            initializer { ChecklistViewModel(appContainer.tripRepository, appContainer.checklistRepository) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(viewModel: ChecklistViewModel = viewModel(factory = ChecklistViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    val deletedText = stringResource(R.string.checklist_deleted)
    val undoText = stringResource(R.string.action_undo)

    fun submit() {
        viewModel.add(input)
        input = ""
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.checklist_title)) },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.action_more))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.checklist_clear_done)) },
                            enabled = state.doneCount > 0,
                            onClick = {
                                menuOpen = false
                                viewModel.clearDone()
                            },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text(stringResource(R.string.checklist_input)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                trailingIcon = {
                    IconButton(onClick = ::submit, enabled = input.isNotBlank()) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.action_add))
                    }
                },
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            if (state.items.isNotEmpty()) {
                Progress(done = state.doneCount, total = state.items.size)
            }

            if (!state.loading && state.items.isEmpty()) {
                EmptyState(
                    emoji = "📝",
                    title = stringResource(R.string.checklist_empty_title),
                    text = stringResource(R.string.checklist_empty_text),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(state.items, key = { it.id }) { item ->
                        SwipeRow(
                            onDelete = {
                                viewModel.delete(item)
                                scope.launch {
                                    val result = snackbarHost.showSnackbar(deletedText, actionLabel = undoText)
                                    if (result == SnackbarResult.ActionPerformed) viewModel.restore(item)
                                }
                            },
                            modifier = Modifier.animateItem(),
                        ) {
                            ItemRow(
                                item = item,
                                trip = state.trip,
                                onToggle = { viewModel.toggle(item) },
                                onToggleScope = { viewModel.toggleScope(item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Progress(done: Int, total: Int) {
    val fraction by animateFloatAsState(if (total == 0) 0f else done.toFloat() / total, label = "progress")
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Text(
                text = if (done == total) stringResource(R.string.checklist_all_done) else stringResource(R.string.checklist_progress, done, total),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeRow(onDelete: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        onDismiss = { if (it != SwipeToDismissBoxValue.Settled) onDelete() },
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) { Text("🗑️") }
        },
    ) { content() }
}

@Composable
private fun ItemRow(item: ChecklistItem, trip: Trip?, onToggle: () -> Unit, onToggleScope: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onToggle)
            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = item.done, onCheckedChange = { onToggle() })
        Text(
            text = item.text,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (item.done) TextDecoration.LineThrough else null,
            color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        val scopeLabel = if (item.isUniversal || trip == null) {
            "🌍 " + stringResource(R.string.checklist_scope_all)
        } else {
            stringResource(R.string.checklist_scope_trip, trip.emoji)
        }
        Text(
            text = scopeLabel,
            style = MaterialTheme.typography.labelMedium,
            color = if (item.isUniversal) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (item.isUniversal) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer)
                .clickable(enabled = trip != null, onClick = onToggleScope)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}
