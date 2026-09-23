package kz.chaykin.potracheno.ui.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

data class DonutSlice(val fraction: Float, val color: Color)

/** Пончик категорий: сегменты с зазором, в центре — общая сумма. */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    centerTitle: String,
    centerValue: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val progress = remember(slices) { Animatable(0f) }
    LaunchedEffect(slices) { progress.animateTo(1f, tween(900)) }

    Box(modifier = modifier.size(200.dp).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val stroke = 28.dp.toPx()
            val gap = if (slices.size > 1) 3f else 0f
            var start = -90f
            slices.forEach { slice ->
                val sweep = 360f * slice.fraction * progress.value
                if (sweep > gap) {
                    drawArc(
                        color = slice.color,
                        startAngle = start + gap / 2,
                        sweepAngle = sweep - gap,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Butt),
                        topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                        size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                    )
                }
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(centerValue, style = MaterialTheme.typography.titleLarge)
        }
    }
}
