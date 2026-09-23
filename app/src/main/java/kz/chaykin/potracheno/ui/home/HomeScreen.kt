package kz.chaykin.potracheno.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.chaykin.potracheno.R
import kz.chaykin.potracheno.model.OperationType
import kz.chaykin.potracheno.ui.components.EmptyState
import kz.chaykin.potracheno.ui.components.MoneyDisplay
import kz.chaykin.potracheno.ui.components.OperationRow
import kz.chaykin.potracheno.ui.components.SectionTitle
import kz.chaykin.potracheno.ui.components.TripBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCreateTrip: () -> Unit,
    onEditTrip: (Long) -> Unit,
    onAddOperation: (OperationType) -> Unit,
    onOpenOperation: (Long) -> Unit,
    onOpenAllOperations: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pickerOpen by remember { mutableStateOf(false) }
    val trip = state.trip

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (trip != null) {
                        Row(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.large)
                                .clickable { pickerOpen = true }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            TripBadge(trip.emoji, trip.cover, size = 36.dp)
                            Text(
                                text = trip.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.home_pick_trip),
                            )
                        }
                    } else {
                        Text(stringResource(R.string.app_name))
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            if (trip != null) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SmallFloatingActionButton(
                        onClick = { onAddOperation(OperationType.TOP_UP) },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.home_top_up))
                    }
                    ExtendedFloatingActionButton(
                        onClick = { onAddOperation(OperationType.EXPENSE) },
                        icon = { Icon(Icons.Outlined.ShoppingBag, contentDescription = null) },
                        text = { Text(stringResource(R.string.home_spend)) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        },
    ) { padding ->
        when {
            state.loading -> Unit

            trip == null -> EmptyState(
                emoji = "🧳",
                title = stringResource(R.string.home_no_trips_title),
                text = stringResource(R.string.home_no_trips_text),
                modifier = Modifier.padding(padding),
                action = {
                    Button(onClick = onCreateTrip) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.home_create_trip), modifier = Modifier.padding(start = 8.dp))
                    }
                },
            )

            else -> {
                val stats = state.stats ?: return@Scaffold
                val money = MoneyDisplay.of(trip, state.showInRub)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item(key = "hero") {
                        HeroCard(
                            trip = trip,
                            stats = stats,
                            money = money,
                            showInRub = state.showInRub,
                            onToggleRub = viewModel::setShowInRub,
                        )
                    }
                    item(key = "today-title") {
                        Spacer(Modifier.height(16.dp))
                        SectionTitle(
                            text = stringResource(R.string.home_today),
                            trailing = {
                                if (stats.todaySpent > 0) {
                                    Text(
                                        text = stringResource(R.string.home_today_total, money.format(stats.todaySpent)),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                        )
                    }
                    if (state.today.isEmpty()) {
                        item(key = "today-empty") {
                            Text(
                                text = stringResource(R.string.home_today_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        }
                    } else {
                        items(state.today, key = { it.id }) { operation ->
                            OperationRow(
                                operation = operation,
                                money = money,
                                onClick = { onOpenOperation(operation.id) },
                            )
                        }
                    }
                    item(key = "all") {
                        TextButton(onClick = onOpenAllOperations, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.home_all_operations))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    if (pickerOpen) {
        TripPickerSheet(
            trips = state.trips,
            currentId = trip?.id,
            onSelect = {
                viewModel.selectTrip(it)
                pickerOpen = false
            },
            onEdit = {
                pickerOpen = false
                onEditTrip(it)
            },
            onCreate = {
                pickerOpen = false
                onCreateTrip()
            },
            onDismiss = { pickerOpen = false },
        )
    }
}
