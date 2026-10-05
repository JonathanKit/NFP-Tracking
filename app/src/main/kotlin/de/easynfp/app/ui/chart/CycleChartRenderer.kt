package de.easynfp.app.ui.chart

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import de.easynfp.app.text.Formats
import de.easynfp.data.model.Intercourse
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
    val stepHeight: Float = 11f * unit,
)

/**
 * Draws the cycle sheet (F-30 to F-32) onto an [android.graphics.Canvas].
 * Used by the screen (via Compose `drawIntoCanvas`) and by the PDF export (F-52).
 *
 * N-05: phases are shown with color, hatching (fertile) and a letter in the phase band.
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

    private fun rows(model: ChartModel): List<String> = buildList {
        add(style.labels.date)
        add(style.labels.cycleDay)
        add(style.labels.bleeding)
        add(style.labels.mucus)
        add(style.labels.peak)
        add(style.labels.disturbance)
        if (model.showIntercourse) add(style.labels.intercourse)
    }

    fun totalHeight(model: ChartModel): Float = plotTop() + plotHeight(model) + gap + rows(model).size * rowHeight

    fun bodyWidth(model: ChartModel): Float = model.columns.size * style.columnWidth

    private fun plotTop() = bandHeight + gap

    private fun y(model: ChartModel, hundredths: Int): Float =
        plotTop() + (model.yMax - hundredths) / STEP.toFloat() * style.stepHeight

    private fun rowTop(model: ChartModel, index: Int) = plotTop() + plotHeight(model) + gap + index * rowHeight

    private fun centerX(index: Int) = index * style.columnWidth + style.columnWidth / 2

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
        drawCentered(canvas, style.labels.phase, right, bandHeight / 2)

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
        rows(model).forEachIndexed { index, label ->
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

        model.columns.forEachIndexed { index, column ->
            if (column.isToday) {
                fill.color = c.today
                canvas.drawRect(index * style.columnWidth, 0f, (index + 1) * style.columnWidth, height, fill)
            }
            drawPhaseCell(canvas, index, column)
        }
        drawGrid(canvas, model, width)
        drawCoverline(canvas, model)
        drawTemperatures(canvas, model)
        drawRows(canvas, model)
    }

    private fun drawPhaseCell(canvas: Canvas, index: Int, column: ChartColumn) {
        val phase = column.phase ?: return
        val left = index * style.columnWidth
        val right = left + style.columnWidth
        val inset = 1f * u
        if (phase.infertileFromEvening) {
            val mid = left + style.columnWidth / 2
            drawBandPart(canvas, left + inset, mid, fertile = true)
            drawBandPart(canvas, mid, right - inset, fertile = false)
            text.color = c.onInfertile
            text.textSize = sp(11f)
            drawCentered(canvas, "☾", (left + right) / 2, bandHeight / 2)
            return
        }
        val fertile = phase.phase == Phase.FERTILE
        drawBandPart(canvas, left + inset, right - inset, fertile)
        text.color = if (fertile) c.onFertile else c.onInfertile
        text.textSize = sp(10f)
        text.isFakeBoldText = true
        drawCentered(
            canvas,
            if (fertile) style.labels.fertileShort else style.labels.infertileShort,
            (left + right) / 2,
            bandHeight / 2,
        )
        text.isFakeBoldText = false
    }

    private fun drawBandPart(canvas: Canvas, left: Float, right: Float, fertile: Boolean) {
        fill.color = if (fertile) c.fertile else c.infertile
        canvas.drawRect(left, 0f, right, bandHeight, fill)
        if (fertile) {
            canvas.save()
            canvas.clipRect(left, 0f, right, bandHeight)
            stroke.color = c.fertileHatch
            stroke.strokeWidth = 1.5f * u
            stroke.pathEffect = null
            var x = left - bandHeight
            while (x < right) {
                canvas.drawLine(x, bandHeight, x + bandHeight, 0f, stroke)
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
            val x = index * style.columnWidth
            canvas.drawLine(x, plotTop(), x, bottom, stroke)
        }
        rows(model).indices.forEach { index ->
            val top = rowTop(model, index)
            canvas.drawLine(0f, top, width, top, stroke)
        }
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
                drawCentered(canvas, it.toString(), x, yPos + 11f * u)
            }
            column.higherNumber?.let {
                text.color = if (column.higherCounts) c.higher else c.excluded
                text.textSize = sp(10f)
                text.isFakeBoldText = true
                text.isStrikeThruText = !column.higherCounts
                drawCentered(canvas, it.toString(), x, yPos - 11f * u)
                text.isFakeBoldText = false
                text.isStrikeThruText = false
            }
        }
    }

    private fun drawRows(canvas: Canvas, model: ChartModel) {
        fun center(index: Int) = rowTop(model, index) + rowHeight / 2

        model.columns.forEachIndexed { index, column ->
            val x = centerX(index)
            text.color = c.textSecondary
            text.textSize = sp(9f)
            drawCentered(canvas, column.date.dayOfMonth.toString() + ".", x, center(0))
            text.color = c.text
            text.textSize = sp(10f)
            drawCentered(canvas, column.cycleDay.toString(), x, center(1))
            column.bleeding?.let { drawBleeding(canvas, it, x, center(2)) }
            column.mucus?.let {
                text.color = c.text
                text.textSize = sp(11f)
                text.isFakeBoldText = it.isFertileQuality
                drawCentered(canvas, it.symbol, x, center(3))
                text.isFakeBoldText = false
            }
            column.peakMarker?.let {
                text.color = c.higher
                text.textSize = sp(11f)
                text.isFakeBoldText = true
                drawCentered(canvas, it, x, center(4))
                text.isFakeBoldText = false
            }
            if (column.hasDisturbance) {
                text.color = c.disturbance
                text.textSize = sp(12f)
                text.isFakeBoldText = true
                drawCentered(canvas, "!", x, center(5))
                text.isFakeBoldText = false
            }
            if (model.showIntercourse) {
                column.intercourse?.let {
                    text.color = c.text
                    text.textSize = sp(11f)
                    drawCentered(canvas, if (it == Intercourse.UNPROTECTED) "♥" else "♡", x, center(6))
                }
            }
        }
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

    private companion object {
        const val STEP = 5
        const val TENTH = 10
    }
}
