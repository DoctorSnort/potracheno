package kz.chaykin.potracheno.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.chaykin.potracheno.model.Category
import kz.chaykin.potracheno.model.CoverColor
import kz.chaykin.potracheno.ui.theme.categoryColors
import kz.chaykin.potracheno.ui.theme.gradient

/** Пустой экран: крупный эмодзи вместо серой пиктограммы — так веселее. */
@Composable
fun EmptyState(
    emoji: String,
    title: String,
    modifier: Modifier = Modifier,
    text: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        Text(text = emoji, fontSize = 64.sp)
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        if (text != null) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        action?.invoke()
    }
}

/** Кружок категории с эмодзи на её цвете. */
@Composable
fun CategoryBadge(category: Category, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val color = MaterialTheme.categoryColors[category]
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = category.emoji, fontSize = (size.value * 0.46f).sp)
    }
}

/** Кружок поездки: её эмодзи на градиенте обложки. */
@Composable
fun TripBadge(emoji: String, cover: CoverColor, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(cover.gradient())),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, fontSize = (size.value * 0.46f).sp)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = text, style = MaterialTheme.typography.titleLarge)
        trailing?.invoke()
    }
}

/** Поле суммы: цифровая клавиатура, крупные цифры одинаковой ширины. */
@Composable
fun MoneyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    large: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() || it == ',' || it == '.' || it == ' ' }) },
        label = { Text(label) },
        suffix = suffix?.let { { Text(it, style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge) } },
        textStyle = if (large) {
            MaterialTheme.typography.headlineMedium
        } else {
            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        singleLine = true,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        modifier = modifier.fillMaxWidth(),
    )
}

/** Полупрозрачная плашка поверх градиента: белый текст на ней читается всегда. */
@Composable
fun GlassTile(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(Color.White.copy(alpha = 0.16f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) { content() }
}
