package kz.chaykin.potracheno.ui.tripeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.chaykin.potracheno.R
import kz.chaykin.potracheno.model.CoverColor
import kz.chaykin.potracheno.model.CurrencyPresets
import kz.chaykin.potracheno.ui.components.ConfirmDialog
import kz.chaykin.potracheno.ui.components.DateFormats
import kz.chaykin.potracheno.ui.components.MoneyField
import kz.chaykin.potracheno.ui.theme.gradient
import kz.chaykin.potracheno.util.RuPlural
import java.time.temporal.ChronoUnit

/** Тематические значки поездки: первое, что приходит в голову при слове «отпуск». */
private val TripEmojis = listOf(
    "✈️", "🏝️", "🏔️", "🏙️", "🐉", "🗼", "🕌", "🏯", "🌋", "🏖️", "⛩️", "🗽",
    "🎿", "🚂", "🚗", "⛵", "🍜", "🍷", "🌴", "🐘", "🦘", "🎡", "🎒", "🌍",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TripEditorScreen(
    onDone: () -> Unit,
    viewModel: TripEditorViewModel = viewModel(factory = TripEditorViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    var pickDates by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.trip_new else R.string.trip_edit)) },
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
                    TextButton(onClick = viewModel::save, enabled = !state.saving) { Text(stringResource(R.string.action_save)) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Превью обложки: сразу видно, как поездка будет выглядеть на главной.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(Brush.linearGradient(state.cover.gradient()))
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(state.emoji, fontSize = 40.sp)
                Column {
                    Text(
                        text = state.name.ifBlank { stringResource(R.string.trip_new) },
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    Text(
                        text = "${state.currencyCode} · ${state.currencySymbol}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.trip_name)) },
                placeholder = { Text(stringResource(R.string.trip_name_hint)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                isError = state.nameError,
                supportingText = if (state.nameError) {
                    { Text(stringResource(R.string.error_name_required)) }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Label(stringResource(R.string.trip_emoji))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TripEmojis) { emoji ->
                    val selected = emoji == state.emoji
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            )
                            .clickable { viewModel.onEmojiChange(emoji) },
                        contentAlignment = Alignment.Center,
                    ) { Text(emoji, fontSize = 22.sp) }
                }
            }

            Label(stringResource(R.string.trip_cover))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CoverColor.entries.forEach { cover ->
                    val selected = cover == state.cover
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(cover.gradient()))
                            .then(
                                if (selected) {
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                } else {
                                    Modifier
                                },
                            )
                            .clickable { viewModel.onCoverChange(cover) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Label(stringResource(R.string.trip_currency))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CurrencyPresets.forEach { preset ->
                    FilterChip(
                        selected = preset.code == state.currencyCode,
                        onClick = { viewModel.onPreset(preset) },
                        label = { Text("${preset.flag} ${preset.code}") },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.currencyCode,
                    onValueChange = viewModel::onCodeChange,
                    label = { Text(stringResource(R.string.trip_currency_code)) },
                    singleLine = true,
                    isError = state.currencyError,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.currencySymbol,
                    onValueChange = viewModel::onSymbolChange,
                    label = { Text(stringResource(R.string.trip_currency_symbol)) },
                    singleLine = true,
                    isError = state.currencyError,
                    modifier = Modifier.weight(1f),
                )
            }
            if (state.currencyError) {
                Text(
                    stringResource(R.string.error_currency_required),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (!state.isRub) {
                Label(stringResource(R.string.trip_rate))
                MoneyField(
                    value = state.rateText,
                    onValueChange = viewModel::onRateChange,
                    label = stringResource(R.string.trip_rate_label, state.currencySymbol),
                    suffix = "₽",
                    isError = state.rateError,
                    supportingText = stringResource(if (state.rateError) R.string.error_rate_invalid else R.string.trip_rate_hint),
                )
            }

            Label(stringResource(R.string.trip_dates))
            val days = ChronoUnit.DAYS.between(state.startDate, state.endDate).toInt() + 1
            OutlinedButton(onClick = { pickDates = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    text = stringResource(
                        R.string.trip_dates_value,
                        DateFormats.short(state.startDate),
                        DateFormats.short(state.endDate),
                        RuPlural.days(days),
                    ),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }

    if (pickDates) {
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = DateFormats.toPickerMillis(state.startDate),
            initialSelectedEndDateMillis = DateFormats.toPickerMillis(state.endDate),
        )
        DatePickerDialog(
            onDismissRequest = { pickDates = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = pickerState.selectedStartDateMillis
                        if (start != null) {
                            val startDate = DateFormats.fromPickerMillis(start)
                            val endDate = pickerState.selectedEndDateMillis?.let(DateFormats::fromPickerMillis) ?: startDate
                            viewModel.onDatesChange(startDate, endDate)
                        }
                        pickDates = false
                    },
                ) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickDates = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DateRangePicker(
                state = pickerState,
                title = { Text(stringResource(R.string.trip_dates_pick), modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.trip_delete_title),
            text = stringResource(R.string.trip_delete_text, state.name),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
