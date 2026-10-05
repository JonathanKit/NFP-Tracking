package de.easynfp.app.ui.chart

import android.content.res.Resources
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import de.easynfp.app.R
import de.easynfp.app.ui.theme.LocalPhaseColors
import de.easynfp.app.ui.theme.PhaseColors
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

fun chartLabels(res: Resources): ChartLabels = ChartLabels(
    date = res.getString(R.string.chart_row_date),
    cycleDay = res.getString(R.string.chart_row_cycle_day),
    bleeding = res.getString(R.string.chart_row_bleeding),
    mucus = res.getString(R.string.chart_row_mucus),
    peak = res.getString(R.string.chart_row_peak),
    disturbance = res.getString(R.string.chart_row_disturbance),
    intercourse = res.getString(R.string.chart_row_intercourse),
    phase = res.getString(R.string.chart_row_phase),
    infertileShort = res.getString(R.string.chart_infertile_short),
    fertileShort = res.getString(R.string.chart_fertile_short),
    coverline = res.getString(R.string.chart_coverline_short),
)

fun chartColors(phase: PhaseColors, background: Int, text: Int, textSecondary: Int): ChartColors = ChartColors(
    background = background,
    text = text,
    textSecondary = textSecondary,
    infertile = phase.infertile.toArgb(),
    onInfertile = phase.onInfertile.toArgb(),
    fertile = phase.fertile.toArgb(),
    onFertile = phase.onFertile.toArgb(),
    fertileHatch = phase.fertileHatch.toArgb(),
    bleeding = phase.bleeding.toArgb(),
    coverline = phase.coverline.toArgb(),
    line = phase.temperatureLine.toArgb(),
    excluded = phase.excluded.toArgb(),
    reference = phase.reference.toArgb(),
    higher = phase.higher.toArgb(),
    disturbance = phase.disturbance.toArgb(),
    grid = phase.grid.toArgb(),
    gridStrong = phase.gridStrong.toArgb(),
    today = phase.today.toArgb(),
)

/**
 * F-30 to F-34: the cycle sheet. The axis stays fixed, the days scroll horizontally.
 * Tapping a day opens its entry (F-33).
 */
@Composable
fun CycleChart(
    model: ChartModel,
    onDayClick: (LocalDate) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val resources = LocalContext.current.resources
    val phaseColors = LocalPhaseColors.current
    val scheme = MaterialTheme.colorScheme
    val style = remember(density, phaseColors, scheme) {
        ChartStyle(
            unit = density.density,
            fontScale = density.fontScale,
            colors = chartColors(
                phaseColors,
                background = scheme.surface.toArgb(),
                text = scheme.onSurface.toArgb(),
                textSecondary = scheme.onSurfaceVariant.toArgb(),
            ),
            labels = chartLabels(resources),
        )
    }
    val renderer = remember(style) { CycleChartRenderer(style) }
    val height = with(density) { renderer.totalHeight(model).toDp() }
    val bodyWidth = with(density) { renderer.bodyWidth(model).toDp() }
    val axisWidth = with(density) { style.axisWidth.toDp() }
    val scroll = rememberScrollState()
    val today = model.columns.indexOfFirst { it.isToday }

    LaunchedEffect(model.columns.size, today) {
        // Show today (current cycle) or the end of the cycle.
        withTimeoutOrNull(SCROLL_WAIT_MILLIS) { snapshotFlow { scroll.maxValue }.first { it > 0 } }
        val target = if (today >= 0) {
            ((today + 1) * style.columnWidth - scroll.viewportSize * VIEWPORT_TODAY_POSITION).toInt()
        } else {
            scroll.maxValue
        }
        scroll.scrollTo(target.coerceIn(0, scroll.maxValue))
    }

    Row(modifier = modifier.semantics { this.contentDescription = contentDescription }) {
        Canvas(
            Modifier
                .width(axisWidth)
                .height(height),
        ) {
            drawIntoCanvas { renderer.drawAxis(it.nativeCanvas, model) }
        }
        Box(Modifier.horizontalScroll(scroll)) {
            Canvas(
                Modifier
                    .width(bodyWidth)
                    .height(height)
                    .pointerInput(model) {
                        detectTapGestures { offset ->
                            renderer.columnAt(model, offset.x)?.let { index ->
                                val column = model.columns[index]
                                if (column.phase != null) onDayClick(column.date)
                            }
                        }
                    },
            ) {
                drawIntoCanvas { renderer.drawBody(it.nativeCanvas, model) }
            }
        }
    }
}

private const val SCROLL_WAIT_MILLIS = 1_000L
private const val VIEWPORT_TODAY_POSITION = 0.8f
