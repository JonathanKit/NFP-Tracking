package de.easynfp.app.ui.chart

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import de.easynfp.app.text.Formats
import de.easynfp.data.model.Intercourse
import de.easynfp.rules.DayPhase
import de.easynfp.rules.Phase
import de.easynfp.rules.model.Bleeding
import de.easynfp.rules.model.Temperature

/** Colors of the chart as ARGB integers. */
data class ChartColors(
    val background: Int,
    val text: Int,
    val textSecondary: Int,
    val infertile: Int,
    val onInfertile: Int,
    val fertile: Int,
    val onFertile: Int,
    val fertileHatch: Int,
    val bleeding: Int,
    val coverline: Int,
    val line: Int,
    val excluded: Int,
    val reference: Int,
    val higher: Int,
    val disturbance: Int,
    val grid: Int,
    val gridStrong: Int,
    val today: Int,
    val todayOutline: Int,
    val onTodayOutline: Int,
    val infertileColumn: Int,
    val firstHigherColumn: Int,
)

/** Row and band labels, from string resources. */
data class ChartLabels(
    val date: String,
    val cycleDay: String,
    val bleeding: String,
    val mucus: String,
    val peak: String,
    val disturbance: String,
    val intercourse: String,
    val phase: String,
    val infertileShort: String,
    val fertileShort: String,
    val coverline: String,
    val today: String,
    val forecast: String,
)

/**
 * Geometry in pixels. [unit] is the size of 1 dp (screen) or of one point unit (PDF).
 * [fontScale] follows the system font size (N-05).
 */
data class ChartStyle(
    val unit: Float,
    val fontScale: Float,
    val colors: ChartColors,
    val labels: ChartLabels,
    val columnWidth: Float = 34f * unit,
    val axisWidth: Float = 70f * unit,
    /** Height of 0.05 °C. */
    val stepHeight: Float = 14.3f * unit,
)

/**
 * Draws the cycle sheet (F-30 to F-32) onto an [android.graphics.Canvas].
 * Used by the screen (via Compose `drawIntoCanvas`) and by the PDF export (F-52).
 *
 * Layout from top to bottom: date, cycle day, phase band, temperature curve, rows for period,
 * mucus, peak, disturbances and intercourse.
 *
 * N-05: phases are shown with color, hatching (fertile) and a letter in the phase band.
 * F-47: forecast days are pale, marked with "?" and separated from today by a dashed line.
 */
class CycleChartRenderer(private val style: ChartStyle) {
    private val u = style.unit
    private val c = style.colors

    val bandHeight = 22f * u
    private val gap = 10f * u
    private val rowHeight = 22f * u * style.fontScale.coerceAtLeast(1f)
    private val pointRadius = 3.5f * u

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    private fun sp(value: Float) = value * u * style.fontScale

    fun plotHeight(model: ChartModel): Float = (model.yMax - model.yMin) / STEP * style.stepHeight

    private fun bottomRows(model: ChartModel): List<String> = buildList {
        add(style.labels.bleeding)
        add(style.labels.mucus)
        add(style.labels.peak)
        add(style.labels.disturbance)
        if (model.showIntercourse) add(style.labels.intercourse)
    }

    fun totalHeight(model: ChartModel): Float = plotBottom(model) + gap + bottomRows(model).size * rowHeight

    fun bodyWidth(model: ChartModel): Float = model.columns.size * style.columnWidth

    private fun topRowCenter(index: Int) = index * rowHeight + rowHeight / 2

    private fun bandTop() = TOP_ROWS * rowHeight

    private fun plotTop() = bandTop() + bandHeight + gap

    private fun plotBottom(model: ChartModel) = plotTop() + plotHeight(model)

    private fun y(model: ChartModel, hundredths: Int): Float =
        plotTop() + (model.yMax - hundredths) / STEP.toFloat() * style.stepHeight

    private fun rowTop(model: ChartModel, index: Int) = plotBottom(model) + gap + index * rowHeight

    private fun left(index: Int) = index * style.columnWidth

    private fun centerX(index: Int) = left(index) + style.columnWidth / 2

    /** Column index at horizontal position [x] of the body, or null. */
    fun columnAt(model: ChartModel, x: Float): Int? =
        (x / style.columnWidth).toInt().takeIf { x >= 0 && it in model.columns.indices }

    // ---------------------------------------------------------------- axis

    fun drawAxis(canvas: Canvas, model: ChartModel) {
        val right = style.axisWidth - 6f * u
        text.textAlign = Paint.Align.RIGHT
        text.color = c.textSecondary
        text.textSize = sp(10f)
        text.isFakeBoldText = false
        drawCentered(canvas, style.labels.date, right, topRowCenter(0))
        drawCentered(canvas, style.labels.cycleDay, right, topRowCenter(1))
        drawCentered(canvas, style.labels.phase, right, bandTop() + bandHeight / 2)

        var value = model.yMax
        while (value >= model.yMin) {
            if (value % TENTH == 0) {
                text.color = c.text
                drawCentered(canvas, Formats.temperatureValue(Temperature(value)), right, y(model, value))
            }
            value -= STEP
        }
        model.coverline?.let {
            text.color = c.coverline
            text.isFakeBoldText = true
            drawCentered(canvas, style.labels.coverline, 14f * u, y(model, it.hundredths))
            text.isFakeBoldText = false
        }

        text.color = c.textSecondary
        bottomRows(model).forEachIndexed { index, label ->
            drawCentered(canvas, label, right, rowTop(model, index) + rowHeight / 2)
        }
        text.textAlign = Paint.Align.CENTER
    }

    // ---------------------------------------------------------------- body

    fun drawBody(canvas: Canvas, model: ChartModel) {
        val width = bodyWidth(model)
        val height = totalHeight(model)
        fill.color = c.background
        canvas.drawRect(0f, 0f, width, height, fill)

        model.columns.forEachIndexed { index, column -> drawColumnBackground(canvas, model, index, column) }
        model.columns.forEachIndexed { index, column -> drawPhaseCell(canvas, index, column) }
        drawGrid(canvas, model, width)
        drawForecastSeparator(canvas, model)
        drawCoverline(canvas, model)
        drawTemperatures(canvas, model)
        drawTopRows(canvas, model)
        drawRows(canvas, model)
        drawTodayOutline(canvas, model)
    }

    /** Infertile days and the first higher measurement are highlighted over the whole plot. */
    private fun drawColumnBackground(canvas: Canvas, model: ChartModel, index: Int, column: ChartColumn) {
        val top = plotTop()
        val bottom = plotBottom(model)
        if (column.isToday) {
            fill.color = c.today
            canvas.drawRect(left(index), 0f, left(index + 1), totalHeight(model), fill)
        }
        val phase = column.phase ?: column.forecastPhase
        if (phase != null) {
            fill.color = if (column.isForecast) withHalfAlpha(c.infertileColumn) else c.infertileColumn
            when {
                phase.infertileFromEvening -> canvas.drawRect(centerX(index), top, left(index + 1), bottom, fill)
                phase.phase != Phase.FERTILE -> canvas.drawRect(left(index), top, left(index + 1), bottom, fill)
            }
        }
        if (column.cycleDay == model.firstHigherDay) {
            fill.color = c.firstHigherColumn
            canvas.drawRect(left(index), top, left(index + 1), bottom, fill)
        }
    }

    private fun drawPhaseCell(canvas: Canvas, index: Int, column: ChartColumn) {
        val phase: DayPhase = column.phase ?: column.forecastPhase ?: return
        val forecast = column.phase == null
        val left = left(index)
        val right = left + style.columnWidth
        val inset = 1f * u
        val suffix = if (forecast) "?" else ""
        if (phase.infertileFromEvening) {
            val mid = left + style.columnWidth / 2
            drawBandPart(canvas, left + inset, mid, fertile = true, forecast = forecast)
            drawBandPart(canvas, mid, right - inset, fertile = false, forecast = forecast)
            text.color = c.onInfertile
            text.textSize = sp(11f)
            drawCentered(canvas, "☾$suffix", (left + right) / 2, bandTop() + bandHeight / 2)
            return
        }
        val fertile = phase.phase == Phase.FERTILE
        drawBandPart(canvas, left + inset, right - inset, fertile, forecast)
        text.color = if (fertile) c.onFertile else c.onInfertile
        if (forecast) text.color = withHalfAlpha(text.color)
        text.textSize = sp(10f)
        text.isFakeBoldText = !forecast
        drawCentered(
            canvas,
            (if (fertile) style.labels.fertileShort else style.labels.infertileShort) + suffix,
            (left + right) / 2,
            bandTop() + bandHeight / 2,
        )
        text.isFakeBoldText = false
    }

    private fun drawBandPart(canvas: Canvas, left: Float, right: Float, fertile: Boolean, forecast: Boolean) {
        val top = bandTop()
        val bottom = top + bandHeight
        fill.color = if (fertile) c.fertile else c.infertile
        if (forecast) fill.color = withHalfAlpha(fill.color)
        canvas.drawRect(left, top, right, bottom, fill)
        if (fertile) {
            canvas.save()
            canvas.clipRect(left, top, right, bottom)
            stroke.color = if (forecast) withHalfAlpha(c.fertileHatch) else c.fertileHatch
            stroke.strokeWidth = 1.5f * u
            stroke.pathEffect = null
            var x = left - bandHeight
            while (x < right) {
                canvas.drawLine(x, bottom, x + bandHeight, top, stroke)
                x += 6f * u
            }
            canvas.restore()
        }
    }

    private fun drawGrid(canvas: Canvas, model: ChartModel, width: Float) {
        stroke.pathEffect = null
        var value = model.yMax
        while (value >= model.yMin) {
            val strong = value % TENTH == 0
            stroke.color = if (strong) c.gridStrong else c.grid
            stroke.strokeWidth = (if (strong) 1f else 0.6f) * u
            val yPos = y(model, value)
            canvas.drawLine(0f, yPos, width, yPos, stroke)
            value -= STEP
        }
        stroke.color = c.grid
        stroke.strokeWidth = 0.6f * u
        val bottom = totalHeight(model)
        for (index in 0..model.columns.size) {
            canvas.drawLine(left(index), 0f, left(index), bottom, stroke)
        }
        canvas.drawLine(0f, rowHeight, width, rowHeight, stroke)
        canvas.drawLine(0f, bandTop(), width, bandTop(), stroke)
        bottomRows(model).indices.forEach { index ->
            val top = rowTop(model, index)
            canvas.drawLine(0f, top, width, top, stroke)
        }
    }

    /** F-47: dashed line between today and the forecast, with a label. */
    private fun drawForecastSeparator(canvas: Canvas, model: ChartModel) {
        val first = model.columns.indexOfFirst { it.isForecast }.takeIf { it >= 0 } ?: return
        val x = left(first)
        stroke.color = c.todayOutline
        stroke.strokeWidth = 1.5f * u
        stroke.pathEffect = DashPathEffect(floatArrayOf(5f * u, 4f * u), 0f)
        canvas.drawLine(x, bandTop(), x, totalHeight(model), stroke)
        stroke.pathEffect = null
        text.textAlign = Paint.Align.LEFT
        text.color = c.textSecondary
        text.textSize = sp(10f)
        text.isFakeBoldText = true
        drawCentered(canvas, "${style.labels.forecast} ›", x + 4f * u, plotTop() + 8f * u)
        text.isFakeBoldText = false
        text.textAlign = Paint.Align.CENTER
    }

    private fun drawCoverline(canvas: Canvas, model: ChartModel) {
        val coverline = model.coverline ?: return
        val from = model.coverlineFromDay ?: return
        val to = model.coverlineToDay ?: from
        val startX = (from - 1) * style.columnWidth
        val endX = to * style.columnWidth
        stroke.color = c.coverline
        stroke.strokeWidth = 2f * u
        stroke.pathEffect = null
        val yPos = y(model, coverline.hundredths)
        canvas.drawLine(startX, yPos, endX, yPos, stroke)
    }

    private fun drawTemperatures(canvas: Canvas, model: ChartModel) {
        // F-31: valid values connected; excluded values in brackets and not connected.
        stroke.color = c.line
        stroke.strokeWidth = 1.8f * u
        var previous: Pair<Int, Float>? = null
        model.columns.forEachIndexed { index, column ->
            val value = column.plotted ?: return@forEachIndexed
            if (column.excluded) return@forEachIndexed
            val point = index to y(model, value.hundredths)
            previous?.let { (prevIndex, prevY) ->
                val gap = index - prevIndex > 1
                stroke.pathEffect = if (gap) DashPathEffect(floatArrayOf(4f * u, 4f * u), 0f) else null
                canvas.drawLine(centerX(prevIndex), prevY, centerX(index), point.second, stroke)
            }
            previous = point
        }
        stroke.pathEffect = null

        model.columns.forEachIndexed { index, column ->
            val value = column.plotted ?: return@forEachIndexed
            val x = centerX(index)
            val yPos = y(model, value.hundredths)
            if (column.excluded) {
                stroke.color = c.excluded
                stroke.strokeWidth = 1.2f * u
                canvas.drawCircle(x, yPos, pointRadius, stroke)
                text.color = c.excluded
                text.textSize = sp(14f)
                drawCentered(canvas, "(", x - 7f * u, yPos)
                drawCentered(canvas, ")", x + 7f * u, yPos)
                return@forEachIndexed
            }
            fill.color = when {
                column.higherNumber != null && column.higherCounts -> c.higher
                column.referenceNumber != null -> c.reference
                else -> c.line
            }
            canvas.drawCircle(x, yPos, pointRadius, fill)
            column.referenceNumber?.let {
                stroke.color = c.reference
                stroke.strokeWidth = 1.2f * u
                canvas.drawCircle(x, yPos, pointRadius + 2.5f * u, stroke)
                text.color = c.reference
                text.textSize = sp(8f)
                drawCentered(canvas, it.toString(), x, yPos + 13f * u)
            }
            column.higherNumber?.let {
                // The higher measurements are circled like on the paper chart.
                if (column.higherCounts) {
                    stroke.color = c.higher
                    stroke.strokeWidth = 1.6f * u
                    canvas.drawCircle(x, yPos, pointRadius + 4f * u, stroke)
                }
                text.color = if (column.higherCounts) c.higher else c.excluded
                text.textSize = sp(10f)
                text.isFakeBoldText = true
                text.isStrikeThruText = !column.higherCounts
                drawCentered(canvas, it.toString(), x, yPos - 15f * u)
                text.isFakeBoldText = false
                text.isStrikeThruText = false
            }
        }
    }

    /** Date and cycle day above the chart. Today shows "Heute" instead of the date. */
    private fun drawTopRows(canvas: Canvas, model: ChartModel) {
        model.columns.forEachIndexed { index, column ->
            val x = centerX(index)
            if (column.isToday) {
                fill.color = c.todayOutline
                canvas.drawRect(left(index), 0f, left(index + 1), rowHeight, fill)
                text.color = c.onTodayOutline
                text.textSize = sp(8.5f)
                text.isFakeBoldText = true
                drawCentered(canvas, style.labels.today, x, topRowCenter(0))
                text.isFakeBoldText = false
            } else {
                text.color = if (column.isForecast) withHalfAlpha(c.textSecondary) else c.textSecondary
                text.textSize = sp(9f)
                drawCentered(canvas, column.date.dayOfMonth.toString() + ".", x, topRowCenter(0))
            }
            text.color = if (column.isForecast) withHalfAlpha(c.text) else c.text
            text.textSize = sp(10f)
            text.isFakeBoldText = column.isToday
            drawCentered(canvas, column.cycleDay.toString(), x, topRowCenter(1))
            text.isFakeBoldText = false
        }
    }

    private fun drawRows(canvas: Canvas, model: ChartModel) {
        fun center(index: Int) = rowTop(model, index) + rowHeight / 2

        model.columns.forEachIndexed { index, column ->
            val x = centerX(index)
            column.bleeding?.let { drawBleeding(canvas, it, x, center(0)) }
            if (column.bleeding == null && column.expectedPeriod) {
                stroke.color = withHalfAlpha(c.bleeding)
                stroke.strokeWidth = 1.2f * u
                stroke.pathEffect = DashPathEffect(floatArrayOf(2.5f * u, 2f * u), 0f)
                canvas.drawCircle(x, center(0), 5f * u, stroke)
                stroke.pathEffect = null
            }
            column.mucus?.let {
                text.color = c.text
                text.textSize = sp(11f)
                text.isFakeBoldText = it.isFertileQuality
                drawCentered(canvas, it.symbol, x, center(1))
                text.isFakeBoldText = false
            }
            column.peakMarker?.let {
                text.color = c.higher
                text.textSize = sp(11f)
                text.isFakeBoldText = true
                drawCentered(canvas, it, x, center(2))
                text.isFakeBoldText = false
            }
            if (column.hasDisturbance) {
                text.color = c.disturbance
                text.textSize = sp(12f)
                text.isFakeBoldText = true
                drawCentered(canvas, "!", x, center(3))
                text.isFakeBoldText = false
            }
            if (model.showIntercourse) {
                column.intercourse?.let {
                    text.color = c.text
                    text.textSize = sp(11f)
                    drawCentered(canvas, if (it == Intercourse.UNPROTECTED) "♥" else "♡", x, center(4))
                }
            }
        }
    }

    /** F-47: today stays clearly visible, so the forecast is not mistaken for evaluated days. */
    private fun drawTodayOutline(canvas: Canvas, model: ChartModel) {
        val index = model.columns.indexOfFirst { it.isToday }.takeIf { it >= 0 } ?: return
        val half = 1.25f * u
        stroke.color = c.todayOutline
        stroke.strokeWidth = 2.5f * u
        stroke.pathEffect = null
        canvas.drawRect(left(index) + half, half, left(index + 1) - half, totalHeight(model) - half, stroke)
    }

    private fun drawBleeding(canvas: Canvas, bleeding: Bleeding, x: Float, y: Float) {
        val radius = when (bleeding) {
            Bleeding.SPOTTING -> 2.5f
            Bleeding.LIGHT -> 3.5f
            Bleeding.MEDIUM -> 5f
            Bleeding.HEAVY -> 6.5f
        } * u
        if (bleeding == Bleeding.SPOTTING) {
            stroke.color = c.bleeding
            stroke.strokeWidth = 1.2f * u
            stroke.pathEffect = null
            canvas.drawCircle(x, y, radius, stroke)
        } else {
            fill.color = c.bleeding
            canvas.drawCircle(x, y, radius, fill)
        }
    }

    private fun drawCentered(canvas: Canvas, value: String, x: Float, y: Float) {
        canvas.drawText(value, x, y - (text.descent() + text.ascent()) / 2, text)
    }

    private fun withHalfAlpha(color: Int): Int =
        (((color ushr ALPHA_SHIFT) / 2) shl ALPHA_SHIFT) or (color and RGB_MASK)

    private companion object {
        const val STEP = 5
        const val TENTH = 10
        const val TOP_ROWS = 2
        const val ALPHA_SHIFT = 24
        const val RGB_MASK = 0x00FFFFFF
    }
}
