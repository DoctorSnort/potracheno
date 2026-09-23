package kz.chaykin.potracheno.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kz.chaykin.potracheno.R
import kz.chaykin.potracheno.model.Trip
import kz.chaykin.potracheno.ui.components.DateFormats
import kz.chaykin.potracheno.ui.components.TripBadge
import kz.chaykin.potracheno.util.RuPlural
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripPickerSheet(
    trips: List<Trip>,
    currentId: Long?,
    onSelect: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.trips_title), style = MaterialTheme.typography.titleLarge)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f, fill = false)) {
                items(trips, key = { it.id }) { trip ->
                    val selected = trip.id == currentId
                    val days = ChronoUnit.DAYS.between(trip.startDate, trip.endDate).toInt() + 1
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                            )
                            .clickable { onSelect(trip.id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TripBadge(trip.emoji, trip.cover, size = 48.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                trip.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${DateFormats.short(trip.startDate)} — ${DateFormats.short(trip.endDate)} · " +
                                    "${RuPlural.days(days)} · ${trip.currencyCode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onEdit(trip.id) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                    }
                }
            }
            FilledTonalButton(
                onClick = onCreate,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.trip_new), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
