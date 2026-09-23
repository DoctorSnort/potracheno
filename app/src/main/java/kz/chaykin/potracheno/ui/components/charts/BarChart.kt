package kz.chaykin.potracheno.ui.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp

data class Bar(val label: String, val counted: Long, val excluded: Long, val highlighted: Boolean)

/**
 * Столбик на день: снизу то, что идёт в среднее, сверху бледная «экскурсия».
 * Пунктир — среднедневной расход. На длинной поездке график прокручивается к сегодняшнему дню.
 */
@Composable
fun BarChart(
    bars: List<Bar>,
    average: Long?,
    color: Color,
    description: String,
    modifier: Modifier = Modifier,
) {
    if (bars.isEmpty()) return
    val measurer = rememberTextMeasurer()
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = TextStyle(fontSize = 10.sp, color = axisColor)
    val progress = remember(bars) { Animatable(0f) }
    LaunchedEffect(bars) { progress.animateTo(1f, tween(700)) }
    val scroll = rememberScrollState()
    LaunchedEffect(bars.size) { scroll.scrollTo(scroll.maxValue) }

    BoxWithConstraints(modifier = modifier.semantics { contentDescription = description }) {
        val slot = 28.dp
        val width = max(maxWidth, slot * bars.size)
        Box(modifier = Modifier.horizontalScroll(scroll)) {
            Canvas(
                modifier = Modifier
                    .width(width)
                    .height(180.dp),
            ) {
                val bottomPad = 18.dp.toPx()
                val h = size.height - bottomPad
                val maxV = maxOf(bars.maxOf { it.counted + it.excluded }, average ?: 0L).coerceAtLeast(1L).toFloat()
                val slotPx = size.width / bars.size
                val barW = (slotPx * 0.6f).coerceAtMost(22.dp.toPx())
                val radius = CornerRadius(6.dp.toPx())

                bars.forEachIndexed { i, bar ->
                    val cx = slotPx * i + slotPx / 2
                    val countedH = h * bar.counted / maxV * progress.value
                    val excludedH = h * bar.excluded / maxV * progress.value
                    if (excludedH > 0f) {
                        drawRoundRect(
                            color.copy(alpha = 0.35f),
                            topLeft = Offset(cx - barW / 2, h - countedH - excludedH),
                            size = Size(barW, excludedH + countedH),
                            cornerRadius = radius,
                        )
                    }
                    if (countedH > 0f) {
                        drawRoundRect(color, topLeft = Offset(cx - barW / 2, h - countedH), size = Size(barW, countedH), cornerRadius = radius)
                    }
                    if (bar.highlighted) {
                        drawRoundRect(
                            axisColor,
                            topLeft = Offset(cx - barW / 2 - 3f, h - countedH - excludedH - 3f),
                            size = Size(barW + 6f, countedH + excludedH + 6f),
                            cornerRadius = radius,
                            style = Stroke(width = 2f),
                        )
                    }
                    if (bars.size <= 14 || i % 2 == 0 || bar.highlighted) {
                        val text = measurer.measure(bar.label, labelStyle)
                        drawText(text, topLeft = Offset(cx - text.size.width / 2, size.height - text.size.height))
                    }
                }

                average?.let { avg ->
                    val yy = h - h * avg / maxV
                    drawLine(
                        axisColor,
                        Offset(0f, yy),
                        Offset(size.width, yy),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                    )
                }
            }
        }
    }
}
