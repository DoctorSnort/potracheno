package kz.chaykin.potracheno.ui.debts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kz.chaykin.potracheno.R
import kz.chaykin.potracheno.data.repo.DebtRepository
import kz.chaykin.potracheno.data.repo.TripRepository
import kz.chaykin.potracheno.model.PersonBalance
import kz.chaykin.potracheno.model.Trip
import kz.chaykin.potracheno.ui.appContainer
import kz.chaykin.potracheno.ui.components.EmptyState
import kz.chaykin.potracheno.ui.components.MoneyDisplay
import kz.chaykin.potracheno.ui.components.PersonAvatar
import kz.chaykin.potracheno.ui.theme.brandColors
import kz.chaykin.potracheno.ui.theme.gradient

data class DebtsState(
    val loading: Boolean = true,
    val trip: Trip? = null,
    val balances: List<PersonBalance> = emptyList(),
) {
    val owedToMe: Long get() = balances.filter { it.balanceMinor > 0 }.sumOf { it.balanceMinor }
    val iOwe: Long get() = -balances.filter { it.balanceMinor < 0 }.sumOf { it.balanceMinor }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DebtsViewModel(tripRepository: TripRepository, debtRepository: DebtRepository) : ViewModel() {
    val state: StateFlow<DebtsState> = tripRepository.observeCurrent().flatMapLatest { trip ->
        if (trip == null) {
            flowOf(DebtsState(loading = false))
        } else {
            debtRepository.observeBalances(trip.id).map { DebtsState(loading = false, trip = trip, balances = it) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DebtsState())

    companion object {
        val Factory = viewModelFactory {
            initializer { DebtsViewModel(appContainer.tripRepository, appContainer.debtRepository) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    onOpenPerson: (Long) -> Unit,
    onAddPerson: () -> Unit,
    viewModel: DebtsViewModel = viewModel(factory = DebtsViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.debts_title)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            if (state.trip != null) {
                ExtendedFloatingActionButton(
                    onClick = onAddPerson,
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text(stringResource(R.string.debts_add_person)) },
                )
            }
        },
    ) { padding ->
        val trip = state.trip
        when {
            state.loading -> Unit
            trip == null -> EmptyState(
                emoji = "🤝",
                title = stringResource(R.string.debts_no_trip),
                modifier = Modifier.padding(padding),
            )
            state.balances.isEmpty() -> EmptyState(
                emoji = "🤝",
                title = stringResource(R.string.debts_empty_title),
                text = stringResource(R.string.debts_empty_text),
                modifier = Modifier.padding(padding),
            )
            else -> {
                val money = MoneyDisplay.native(trip)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "summary") {
                        Summary(trip, state.owedToMe, state.iOwe, money)
                    }
                    items(state.balances, key = { it.person.id }) { balance ->
                        BalanceRow(balance, money, onClick = { onOpenPerson(balance.person.id) })
                    }
                    item(key = "hint") {
                        Text(
                            text = stringResource(R.string.debts_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Summary(trip: Trip, owedToMe: Long, iOwe: Long, money: MoneyDisplay) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(trip.cover.gradient()))
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.debts_owed_to_me), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
            Text(money.format(owedToMe), style = MaterialTheme.typography.headlineSmall, color = Color.White, maxLines = 1)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.debts_i_owe), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
            Text(money.format(iOwe), style = MaterialTheme.typography.headlineSmall, color = Color.White, maxLines = 1)
        }
    }
}

@Composable
private fun BalanceRow(balance: PersonBalance, money: MoneyDisplay, onClick: () -> Unit) {
    val amount = balance.balanceMinor
    val (label, color) = when {
        amount > 0 -> stringResource(R.string.debt_owes_me) to MaterialTheme.brandColors.positive
        amount < 0 -> stringResource(R.string.debt_i_owe) to MaterialTheme.brandColors.negative
        else -> stringResource(R.string.debt_settled) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PersonAvatar(balance.person, size = 48.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(balance.person.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.bodySmall, color = color)
        }
        if (amount != 0L) {
            Text(money.format(kotlin.math.abs(amount)), style = MaterialTheme.typography.titleMedium, color = color)
        } else {
            Text("✌️", style = MaterialTheme.typography.titleMedium)
        }
    }
}
