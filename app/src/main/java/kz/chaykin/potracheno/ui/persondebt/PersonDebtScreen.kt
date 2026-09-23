package kz.chaykin.potracheno.ui.persondebt

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kz.chaykin.potracheno.R
import kz.chaykin.potracheno.model.DebtEntry
import kz.chaykin.potracheno.ui.components.ConfirmDialog
import kz.chaykin.potracheno.ui.components.DateFormats
import kz.chaykin.potracheno.ui.components.MoneyDisplay
import kz.chaykin.potracheno.ui.components.MoneyField
import kz.chaykin.potracheno.ui.components.PersonAvatar
import kz.chaykin.potracheno.ui.components.PhotoViewerDialog
import kz.chaykin.potracheno.ui.theme.brandColors
import kz.chaykin.potracheno.util.MoneyFormat
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PersonDebtScreen(
    onBack: () -> Unit,
    onEditPerson: (Long) -> Unit,
    onOpenOperation: (Long) -> Unit,
    viewModel: PersonDebtViewModel = viewModel(factory = PersonDebtViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var addOpen by remember { mutableStateOf(false) }
    var confirmSettle by remember { mutableStateOf(false) }
    var deleteEntry by remember { mutableStateOf<DebtEntry?>(null) }
    var viewPhoto by remember { mutableStateOf(false) }

    val settledComment = stringResource(R.string.debt_settled_comment)
    val settledDone = stringResource(R.string.debt_settled_done)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text(state.person?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    state.person?.let { person ->
                        IconButton(onClick = { onEditPerson(person.id) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                    }
                },
            )
        },
    ) { padding ->
        val person = state.person ?: return@Scaffold
        val trip = state.trip ?: return@Scaffold
        val money = MoneyDisplay.native(trip)
        val balance = state.balance
        val color = when {
            balance > 0 -> MaterialTheme.brandColors.positive
            balance < 0 -> MaterialTheme.brandColors.negative
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "header") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PersonAvatar(
                        person,
                        size = 112.dp,
                        modifier = Modifier.clickable(enabled = person.photoFileName != null) { viewPhoto = true },
                    )
                    Text(
                        text = when {
                            balance > 0 -> stringResource(R.string.debt_owes_me)
                            balance < 0 -> stringResource(R.string.debt_i_owe)
                            else -> stringResource(R.string.debt_settled)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = color,
                    )
                    Text(
                        text = if (balance == 0L) "✌️" else money.format(balance.absoluteValue),
                        style = MaterialTheme.typography.displaySmall,
                        color = color,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        FilledTonalButton(onClick = { addOpen = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.debt_add), modifier = Modifier.padding(start = 6.dp))
                        }
                        Button(onClick = { confirmSettle = true }, enabled = balance != 0L) {
                            Text(stringResource(R.string.debt_settle))
                        }
                    }
                }
            }

            item(key = "history") {
                Text(
                    stringResource(R.string.debt_history),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            if (state.entries.isEmpty()) {
                item(key = "empty") {
                    Text(
                        stringResource(R.string.debt_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.entries, key = { it.id }) { entry ->
                EntryRow(
                    entry = entry,
                    money = money,
                    onClick = { entry.expenseId?.let(onOpenOperation) },
                    onLongClick = { if (entry.expenseId == null) deleteEntry = entry },
                )
            }
        }
    }

    if (addOpen) {
        AddDebtDialog(
            currencySymbol = state.trip?.currencySymbol.orEmpty(),
            onConfirm = { signed, comment ->
                viewModel.addEntry(signed, comment)
                addOpen = false
            },
            onDismiss = { addOpen = false },
        )
    }

    if (confirmSettle) {
        val person = state.person
        val trip = state.trip
        if (person != null && trip != null) {
            val amount = MoneyFormat.format(state.balance.absoluteValue, trip.currencySymbol)
            ConfirmDialog(
                title = stringResource(R.string.debt_settle_title),
                text = if (state.balance > 0) {
                    stringResource(R.string.debt_settle_text_owes_me, person.name, amount)
                } else {
                    stringResource(R.string.debt_settle_text_i_owe, person.name, amount)
                },
                confirmLabel = stringResource(R.string.debt_settle_confirm),
                onConfirm = {
                    confirmSettle = false
                    viewModel.settle(settledComment)
                    scope.launch { snackbarHost.showSnackbar(settledDone) }
                },
                onDismiss = { confirmSettle = false },
            )
        }
    }

    deleteEntry?.let { entry ->
        ConfirmDialog(
            title = stringResource(R.string.debt_delete_title),
            text = stringResource(R.string.debt_delete_text),
            onConfirm = {
                viewModel.deleteEntry(entry.id)
                deleteEntry = null
            },
            onDismiss = { deleteEntry = null },
        )
    }

    if (viewPhoto) {
        state.person?.photoFileName?.let { PhotoViewerDialog(fileName = it, onDismiss = { viewPhoto = false }) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryRow(entry: DebtEntry, money: MoneyDisplay, onClick: () -> Unit, onLongClick: () -> Unit) {
    val positive = entry.amountMinor > 0
    val fromExpense = entry.expenseId != null
    val title = when {
        fromExpense -> entry.expenseComment?.takeIf { it.isNotBlank() }
            ?: entry.expenseCategory?.let { stringResource(it.title) }
            ?: stringResource(R.string.debt_from_expense)
        else -> entry.comment?.takeIf { it.isNotBlank() }
            ?: stringResource(if (positive) R.string.debt_direction_owes_me else R.string.debt_direction_i_owe)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(if (fromExpense) entry.expenseCategory?.emoji ?: "🧾" else if (positive) "📥" else "📤", fontSize = 22.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = DateFormats.shortWithTime(entry.occurredAt) +
                    if (fromExpense) " · " + stringResource(R.string.debt_from_expense) else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = money.signed(entry.amountMinor),
            style = MaterialTheme.typography.titleMedium,
            color = if (positive) MaterialTheme.brandColors.positive else MaterialTheme.brandColors.negative,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDebtDialog(
    currencySymbol: String,
    onConfirm: (signedMinor: Long, comment: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var owesMe by remember { mutableStateOf(true) }
    var amount by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    val parsed = MoneyFormat.parse(amount)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.debt_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(true, false).forEachIndexed { index, value ->
                        SegmentedButton(
                            selected = owesMe == value,
                            onClick = { owesMe = value },
                            shape = SegmentedButtonDefaults.itemShape(index, 2),
                        ) {
                            Text(stringResource(if (value) R.string.debt_direction_owes_me else R.string.debt_direction_i_owe))
                        }
                    }
                }
                MoneyField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = stringResource(R.string.operation_amount),
                    suffix = currencySymbol,
                )
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text(stringResource(R.string.operation_comment)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let { onConfirm(if (owesMe) it else -it, comment) } },
                enabled = parsed != null && parsed > 0,
            ) { Text(stringResource(R.string.action_add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
