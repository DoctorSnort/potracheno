package kz.chaykin.potracheno.ui.operationeditor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.chaykin.potracheno.R
import kz.chaykin.potracheno.domain.RateMath
import kz.chaykin.potracheno.model.Category
import kz.chaykin.potracheno.model.OperationType
import kz.chaykin.potracheno.model.Person
import kz.chaykin.potracheno.ui.components.ConfirmDialog
import kz.chaykin.potracheno.ui.components.DateFormats
import kz.chaykin.potracheno.ui.components.MoneyField
import kz.chaykin.potracheno.ui.components.PersonAvatar
import kz.chaykin.potracheno.ui.theme.categoryColors
import kz.chaykin.potracheno.util.MoneyFormat
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OperationEditorScreen(
    onDone: () -> Unit,
    viewModel: OperationEditorViewModel = viewModel(factory = OperationEditorViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val persons by viewModel.persons.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var newPersonFor by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onDone()
    }

    val title = when {
        state.isNew && state.isExpense -> R.string.operation_new_expense
        state.isNew -> R.string.operation_new_top_up
        state.isExpense -> R.string.operation_edit_expense
        else -> R.string.operation_edit_top_up
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    TextButton(onClick = viewModel::save, enabled = !state.loading && !state.saving) {
                        Text(stringResource(R.string.action_save))
                    }
                },
            )
        },
    ) { padding ->
        val trip = state.trip ?: return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(OperationType.EXPENSE, OperationType.TOP_UP).forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = state.type == type,
                        onClick = { viewModel.onTypeChange(type) },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                    ) {
                        Text(
                            stringResource(
                                if (type == OperationType.EXPENSE) R.string.operation_type_expense else R.string.operation_type_top_up,
                            ),
                        )
                    }
                }
            }

            val paid = state.paidMinor
            MoneyField(
                value = state.amountText,
                onValueChange = viewModel::onAmountChange,
                label = stringResource(if (state.isExpense) R.string.operation_paid else R.string.operation_amount),
                suffix = trip.currencySymbol,
                large = true,
                isError = state.amountError,
                supportingText = when {
                    state.amountError -> stringResource(R.string.error_amount_invalid)
                    paid != null && !trip.isRub ->
                        stringResource(R.string.operation_in_rub, MoneyFormat.format(RateMath.toRub(paid, trip.rate), "₽"))
                    else -> null
                },
            )

            if (state.isExpense) {
                Label(stringResource(R.string.operation_category))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Category.entries.forEach { category ->
                        CategoryChip(category, selected = category == state.category) { viewModel.onCategoryChange(category) }
                    }
                }
            }

            OutlinedTextField(
                value = state.comment,
                onValueChange = viewModel::onCommentChange,
                label = { Text(stringResource(R.string.operation_comment)) },
                placeholder = { Text(stringResource(R.string.operation_comment_hint)) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Label(stringResource(R.string.operation_date))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickDate = true }) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(DateFormats.long(state.occurredAt.toLocalDate()), modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedButton(onClick = { pickTime = true }) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(DateFormats.time(state.occurredAt), modifier = Modifier.padding(start = 8.dp))
                }
            }

            if (state.isExpense) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable { viewModel.onExcludeChange(!state.excludeFromDaily) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.operation_exclude), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.operation_exclude_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = state.excludeFromDaily, onCheckedChange = viewModel::onExcludeChange)
                }

                SplitSection(
                    state = state,
                    persons = persons,
                    currencySymbol = trip.currencySymbol,
                    onAdd = viewModel::addSplit,
                    onRemove = viewModel::removeSplit,
                    onPerson = viewModel::onSplitPerson,
                    onAmount = viewModel::onSplitAmount,
                    onEqual = viewModel::splitEqually,
                    onNewPerson = { newPersonFor = it },
                )
            }
        }
    }

    if (pickDate) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateFormats.toPickerMillis(state.occurredAt.toLocalDate()),
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { viewModel.onDateChange(DateFormats.fromPickerMillis(it)) }
                    pickDate = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(state = pickerState) }
    }

    if (pickTime) {
        val timeState = rememberTimePickerState(
            initialHour = state.occurredAt.hour,
            initialMinute = state.occurredAt.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { pickTime = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onTimeChange(LocalTime.of(timeState.hour, timeState.minute))
                    pickTime = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text(stringResource(R.string.action_cancel)) } },
            text = { TimePicker(state = timeState) },
        )
    }

    newPersonFor?.let { key ->
        NewPersonDialog(
            onConfirm = { name ->
                viewModel.createPerson(name, key)
                newPersonFor = null
            },
            onDismiss = { newPersonFor = null },
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.operation_delete_title),
            text = stringResource(R.string.operation_delete_text),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun CategoryChip(category: Category, selected: Boolean, onClick: () -> Unit) {
    val color = MaterialTheme.categoryColors[category]
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .background(if (selected) color else color.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(category.emoji, fontSize = 16.sp)
        Text(
            text = stringResource(category.title),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SplitSection(
    state: OperationEditorState,
    persons: List<Person>,
    currencySymbol: String,
    onAdd: () -> Unit,
    onRemove: (Long) -> Unit,
    onPerson: (Long, Long) -> Unit,
    onAmount: (Long, String) -> Unit,
    onEqual: () -> Unit,
    onNewPerson: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("🤝 " + stringResource(R.string.operation_split), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.operation_split_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        state.splits.forEach { row ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PersonPicker(
                    persons = persons,
                    selectedId = row.personId,
                    onSelect = { onPerson(row.key, it) },
                    onNew = { onNewPerson(row.key) },
                    modifier = Modifier.weight(1f),
                )
                MoneyField(
                    value = row.amountText,
                    onValueChange = { onAmount(row.key, it) },
                    label = stringResource(R.string.operation_split_amount),
                    suffix = currencySymbol,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onRemove(row.key) }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_delete))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = onAdd,
                label = { Text(stringResource(R.string.operation_split_add)) },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
            if (state.splits.isNotEmpty()) {
                AssistChip(
                    onClick = onEqual,
                    label = {
                        Text(
                            stringResource(
                                if (state.splits.size == 1) R.string.operation_split_half else R.string.operation_split_equal,
                            ),
                        )
                    },
                )
            }
        }

        AnimatedVisibility(visible = state.splits.isNotEmpty()) {
            val share = state.myShareMinor
            if (share != null) {
                Text(
                    text = stringResource(R.string.operation_my_share, MoneyFormat.format(share, currencySymbol)),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (share < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        }

        state.splitError?.let { error ->
            Text(
                text = stringResource(
                    when (error) {
                        SplitError.TOO_MUCH -> R.string.error_split_too_much
                        SplitError.INVALID -> R.string.error_split_invalid
                    },
                ),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PersonPicker(
    persons: List<Person>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onNew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = persons.firstOrNull { it.id == selectedId }
    Column(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            if (selected != null) {
                PersonAvatar(selected, size = 22.dp)
                Text(selected.name, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
            } else {
                Text(stringResource(R.string.operation_split_pick))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            persons.forEach { person ->
                DropdownMenuItem(
                    text = { Text(person.name) },
                    leadingIcon = { PersonAvatar(person, size = 28.dp) },
                    onClick = {
                        onSelect(person.id)
                        expanded = false
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.operation_split_new_person)) },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = {
                    expanded = false
                    onNew()
                },
            )
        }
    }
}

@Composable
private fun NewPersonDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.person_new)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.person_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
