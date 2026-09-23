package kz.chaykin.potracheno.ui.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Плавная кривая по дням с заливкой и необязательной пунктирной линией-ориентиром.
 * Сглаживание — монотонное (Фриц — Карлсон): обычные кривые Безье на резком перепаде
 * «проваливаются» ниже нуля, и график врёт, будто остаток был отрицательным.
 */
@Composable
fun LineChart(
    values: List<Long>,
    labels: List<String>,
    color: Color,
    formatY: (Long) -> String,
    description: String,
    modifier: Modifier = Modifier,
    reference: List<Long>? = null,
) {
    if (values.isEmpty()) return
    val measurer = rememberTextMeasurer()
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = TextStyle(fontSize = 10.sp, color = axisColor)
    val progress = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { progress.animateTo(1f, tween(900)) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .semantics { contentDescription = description },
    ) {
        val all = values + (reference ?: emptyList())
        val maxV = (all.maxOrNull() ?: 0L).coerceAtLeast(1L)
        val minV = (all.minOrNull() ?: 0L).coerceAtMost(0L)
        val range = (maxV - minV).toFloat().coerceAtLeast(1f)

        val leftPad = 44.dp.toPx()
        val bottomPad = 20.dp.toPx()
        val topPad = 8.dp.toPx()
        val w = size.width - leftPad
        val h = size.height - bottomPad - topPad

        fun x(i: Int): Float = leftPad + if (values.size == 1) w / 2 else w * i / (values.size - 1)
        fun y(v: Long): Float = topPad + h * (1f - (v - minV) / range)

        // Сетка и подписи по Y: ноль, середина, максимум.
        listOf(minV, (minV + maxV) / 2, maxV).distinct().forEach { v ->
            val yy = y(v)
            drawLine(gridColor, Offset(leftPad, yy), Offset(size.width, yy), strokeWidth = 1f)
            val text = measurer.measure(formatY(v), labelStyle)
            drawText(text, topLeft = Offset(leftPad - text.size.width - 6.dp.toPx(), yy - text.size.height / 2))
        }
        if (minV < 0) {
            drawLine(axisColor, Offset(leftPad, y(0)), Offset(size.width, y(0)), strokeWidth = 2f)
        }

        // Подписи по X прореживаем, чтобы на длинной поездке они не налезали друг на друга.
        val maxLabels = (w / 44.dp.toPx()).toInt().coerceAtLeast(2)
        val step = ((labels.size + maxLabels - 1) / maxLabels).coerceAtLeast(1)
        labels.forEachIndexed { i, label ->
            if (i % step != 0 && i != labels.lastIndex) return@forEachIndexed
            val text = measurer.measure(label, labelStyle)
            val cx = (x(i) - text.size.width / 2).coerceIn(leftPad, size.width - text.size.width)
            drawText(text, topLeft = Offset(cx, size.height - text.size.height))
        }

        reference?.let { ref ->
            val refPath = Path()
            ref.forEachIndexed { i, v -> if (i == 0) refPath.moveTo(x(i), y(v)) else refPath.lineTo(x(i), y(v)) }
            drawPath(
                refPath,
                color = axisColor.copy(alpha = 0.6f),
                style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))),
            )
        }

        val points = values.mapIndexed { i, v -> Offset(x(i), y(v)) }
        val line = monotonePath(points)
        val fill = Path().apply {
            addPath(line)
            lineTo(points.last().x, topPad + h)
            lineTo(points.first().x, topPad + h)
            close()
        }

        clipRect(right = leftPad + w * progress.value + 4.dp.toPx()) {
            drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent), startY = topPad, endY = topPad + h))
            drawPath(line, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(color, radius = 5.dp.toPx(), center = points.last())
            drawCircle(Color.White, radius = 2.5.dp.toPx(), center = points.last())
        }
    }
}

private fun monotonePath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    if (points.size == 1) return path
    val n = points.size
    val dx = FloatArray(n - 1) { points[it + 1].x - points[it].x }
    val slope = FloatArray(n - 1) { (points[it + 1].y - points[it].y) / dx[it].coerceAtLeast(0.0001f) }
    val tangent = FloatArray(n)
    tangent[0] = slope[0]
    tangent[n - 1] = slope[n - 2]
    for (i in 1 until n - 1) {
        tangent[i] = if (slope[i - 1] * slope[i] <= 0f) 0f else (slope[i - 1] + slope[i]) / 2f
    }
    for (i in 0 until n - 1) {
        if (slope[i] == 0f) {
            tangent[i] = 0f
            tangent[i + 1] = 0f
            continue
        }
        val a = tangent[i] / slope[i]
        val b = tangent[i + 1] / slope[i]
        val s = a * a + b * b
        if (s > 9f) {
            val t = 3f / kotlin.math.sqrt(s)
            tangent[i] = t * a * slope[i]
            tangent[i + 1] = t * b * slope[i]
        }
    }
    for (i in 0 until n - 1) {
        val p0 = points[i]
        val p1 = points[i + 1]
        val d = dx[i] / 3f
        path.cubicTo(p0.x + d, p0.y + tangent[i] * d, p1.x - d, p1.y - tangent[i + 1] * d, p1.x, p1.y)
    }
    return path
}
