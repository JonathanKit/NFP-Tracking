package de.easynfp.app.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.easynfp.app.ui.components.hatched
import de.easynfp.rules.statistics.NumberSummary
import java.util.Locale

/** One part of a [SegmentBar]; [hatchColor] marks fertile days (N-05: not only color). */
data class Segment(val value: Float, val color: Color, val hatchColor: Color? = null)

fun Double.oneDecimal(): String = String.format(Locale.getDefault(), "%.1f", this)

/** Horizontal bar split into parts proportional to their values, with optional markers (0..[total]). */
@Composable
fun SegmentBar(
    segments: List<Segment>,
    description: String,
    modifier: Modifier = Modifier,
    total: Float = segments.sumOf { it.value.toDouble() }.toFloat(),
    marker: Float? = null,
    markerColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(
        modifier
            .fillMaxWidth()
            .height(SEGMENT_BAR_HEIGHT.dp)
            .clip(RoundedCornerShape(50))
            .semantics { contentDescription = description },
    ) {
        drawRect(track)
        if (total <= 0f) return@Canvas
        var left = 0f
        segments.forEach { segment ->
            val width = size.width * segment.value / total
            drawRect(segment.color, Offset(left, 0f), Size(width, size.height))
            segment.hatchColor?.let { hatch(left, left + width, it) }
            left += width
        }
        marker?.let {
            val x = (size.width * it / total).coerceIn(0f, size.width)
            drawLine(markerColor, Offset(x, 0f), Offset(x, size.height), 3.dp.toPx(), StrokeCap.Round)
        }
    }
}

private fun DrawScope.hatch(left: Float, right: Float, color: Color) {
    clipRect(left = left, right = right) {
        val spacing = 10.dp.toPx()
        var x = left - size.height
        while (x < right) {
            drawLine(color, Offset(x, size.height), Offset(x + size.height, 0f), 1.5.dp.toPx())
            x += spacing
        }
    }
}

/** Color swatch and text, explaining one part of a chart. */
@Composable
fun LegendItem(color: Color, text: String, hatchColor: Color? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
                .then(if (hatchColor != null) Modifier.hatched(hatchColor, spacing = 8f) else Modifier),
        )
        Box(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Legend(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        content()
    }
}

/** A big number with a short label below. */
@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Spread of one figure on a shared day axis 0..[scaleMax]: thin line from minimum to maximum,
 * thick band for mean ± standard deviation, dot for the mean.
 */
@Composable
fun RangeRow(label: String, summary: NumberSummary?, scaleMax: Int, noData: String) {
    val lineColor = MaterialTheme.colorScheme.secondary
    val bandColor = MaterialTheme.colorScheme.primary.copy(alpha = BAND_ALPHA)
    val dotColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                if (summary == null) noData else "Ø ${summary.mean.oneDecimal()} · ${summary.min}–${summary.max}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(RANGE_ROW_HEIGHT.dp)
                .semantics {
                    contentDescription = if (summary == null) {
                        "$label: $noData"
                    } else {
                        "$label: Ø ${summary.mean.oneDecimal()}, ${summary.min}–${summary.max}"
                    }
                },
        ) {
            val y = size.height / 2
            drawLine(trackColor, Offset(0f, y), Offset(size.width, y), 2.dp.toPx(), StrokeCap.Round)
            if (summary == null || scaleMax <= 0) return@Canvas
            fun x(value: Double) = (size.width * value / scaleMax).toFloat().coerceIn(0f, size.width)
            drawLine(
                lineColor,
                Offset(x(summary.min.toDouble()), y),
                Offset(x(summary.max.toDouble()), y),
                2.dp.toPx(),
                StrokeCap.Round,
            )
            val low = maxOf(summary.min.toDouble(), summary.mean - summary.standardDeviation)
            val high = minOf(summary.max.toDouble(), summary.mean + summary.standardDeviation)
            drawLine(bandColor, Offset(x(low), y), Offset(x(high), y), 10.dp.toPx(), StrokeCap.Round)
            drawCircle(dotColor, 6.dp.toPx(), Offset(x(summary.mean), y))
        }
    }
}

private const val SEGMENT_BAR_HEIGHT = 28
private const val RANGE_ROW_HEIGHT = 20
private const val BAND_ALPHA = 0.45f
