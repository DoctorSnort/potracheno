package kz.chaykin.potracheno.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.chaykin.potracheno.R
import kz.chaykin.potracheno.model.Category
import kz.chaykin.potracheno.model.Operation
import kz.chaykin.potracheno.ui.theme.brandColors

/**
 * Строка операции. Трата — кружок категории и сумма, пополнение — зелёный «+».
 * Если часть чека заплачена за других, второй строкой: «3 000 ¥ · моя доля 1 500 ¥».
 */
@Composable
fun OperationRow(
    operation: Operation,
    money: MoneyDisplay,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDate: Boolean = false,
) {
    val category = operation.category ?: Category.OTHER
    val title = operation.comment?.takeIf { it.isNotBlank() }
        ?: if (operation.isExpense) stringResource(category.title) else stringResource(R.string.operation_top_up_title)
    val whenText = if (showDate) DateFormats.shortWithTime(operation.occurredAt) else DateFormats.time(operation.occurredAt)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (operation.isExpense) {
            CategoryBadge(category)
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.brandColors.positive.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) { Text("💰", fontSize = 20.sp) }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = whenText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (operation.excludeFromDaily) {
                    Text(
                        text = stringResource(R.string.operation_excluded_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 8.dp, vertical = 1.dp),
                    )
                }
            }
            val paid = operation.paidMinor
            if (operation.hasDebts && paid != null) {
                Text(
                    text = stringResource(R.string.operation_paid_share, money.format(paid), money.format(operation.amountMinor)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = if (operation.isExpense) money.format(operation.amountMinor) else money.signed(operation.amountMinor),
            style = MaterialTheme.typography.titleMedium,
            color = if (operation.isExpense) MaterialTheme.colorScheme.onSurface else MaterialTheme.brandColors.positive,
        )
    }
}
