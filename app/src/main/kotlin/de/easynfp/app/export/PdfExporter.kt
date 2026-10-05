package de.easynfp.app.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import de.easynfp.app.R
import de.easynfp.app.text.ExplanationFormatter
import de.easynfp.app.text.Formats
import de.easynfp.app.ui.chart.ChartModel
import de.easynfp.app.ui.chart.ChartStyle
import de.easynfp.app.ui.chart.CycleChartRenderer
import de.easynfp.app.ui.chart.chartColors
import de.easynfp.app.ui.chart.chartLabels
import de.easynfp.app.ui.theme.PrintPhaseColors
import de.easynfp.data.EvaluationSnapshot
import de.easynfp.rules.CycleEvaluation
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** F-52: the cycle sheet as PDF (A4 landscape), e.g. for the gynaecologist or NFP counselling. */
object PdfExporter {
    private const val PAGE_WIDTH = 842
    private const val PAGE_HEIGHT = 595
    private const val MARGIN = 28f
    private const val HEADER_HEIGHT = 44f
    private const val LINE_HEIGHT = 14f
    private const val BASE_COLUMN = 34f
    private const val BASE_AXIS = 70f

    suspend fun export(
        context: Context,
        uri: Uri,
        evaluation: CycleEvaluation,
        snapshot: EvaluationSnapshot,
        cycleNumber: Int,
        showIntercourse: Boolean,
    ): Boolean = withContext(Dispatchers.IO) {
        val res = context.resources
        val model = ChartModel.build(evaluation, snapshot.entries, snapshot.today, showIntercourse)
        val document = PdfDocument()
        try {
            val colors = chartColors(PrintPhaseColors, Color.WHITE, Color.BLACK, Color.DKGRAY)
            val labels = chartLabels(res)
            // Scale so the whole cycle fits on the page.
            val probe = CycleChartRenderer(ChartStyle(1f, 1f, colors, labels))
            val availableWidth = PAGE_WIDTH - 2 * MARGIN
            val availableHeight = PAGE_HEIGHT - 2 * MARGIN - HEADER_HEIGHT
            val unit = minOf(
                1f,
                availableWidth / (BASE_AXIS + BASE_COLUMN * model.columns.size),
                availableHeight / probe.totalHeight(model),
            )
            val renderer = CycleChartRenderer(ChartStyle(unit, 1f, colors, labels))

            val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
            val canvas = page.canvas
            val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 16f
                isFakeBoldText = true
            }
            val small = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 9f
                color = Color.DKGRAY
            }
            val cycle = evaluation.cycle
            canvas.drawText(
                res.getString(R.string.pdf_title, cycleNumber, Formats.dateMedium(cycle.startDate)),
                MARGIN,
                MARGIN + 16f,
                title,
            )
            canvas.drawText(ExplanationFormatter(res).summary(evaluation), MARGIN, MARGIN + 32f, small)
            canvas.save()
            canvas.translate(MARGIN, MARGIN + HEADER_HEIGHT)
            renderer.drawAxis(canvas, model)
            canvas.translate(BASE_AXIS * unit, 0f)
            renderer.drawBody(canvas, model)
            canvas.restore()
            document.finishPage(page)

            writeExplanationPage(document, context, evaluation)

            val stream = context.contentResolver.openOutputStream(uri, "wt") ?: return@withContext false
            stream.use { document.writeTo(it) }
            true
        } catch (e: IOException) {
            false
        } catch (e: SecurityException) {
            false
        } finally {
            document.close()
        }
    }

    private fun writeExplanationPage(document: PdfDocument, context: Context, evaluation: CycleEvaluation) {
        val res = context.resources
        val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create())
        val canvas = page.canvas
        val heading = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 13f
            isFakeBoldText = true
        }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f }
        var y = MARGIN + 14f
        canvas.drawText(res.getString(R.string.explanation_title), MARGIN, y, heading)
        y += LINE_HEIGHT * 2
        val maxWidth = PAGE_WIDTH - 2 * MARGIN
        ExplanationFormatter(res).steps(evaluation).forEach { line ->
            wrap("${line.ruleCode}  ${line.text}", body, maxWidth).forEach { text ->
                if (y < PAGE_HEIGHT - MARGIN) canvas.drawText(text, MARGIN, y, body)
                y += LINE_HEIGHT
            }
            y += LINE_HEIGHT / 2
        }
        y += LINE_HEIGHT
        wrap(res.getString(R.string.chart_legend), body, maxWidth).forEach { text ->
            if (y < PAGE_HEIGHT - MARGIN) canvas.drawText(text, MARGIN, y, body)
            y += LINE_HEIGHT
        }
        y += LINE_HEIGHT
        wrap(res.getString(R.string.disclaimer_short), body, maxWidth).forEach { text ->
            if (y < PAGE_HEIGHT - MARGIN) canvas.drawText(text, MARGIN, y, body)
            y += LINE_HEIGHT
        }
        document.finishPage(page)
    }

    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        var current = ""
        text.split(' ').forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = candidate
            } else {
                if (current.isNotEmpty()) lines += current
                current = word
            }
        }
        if (current.isNotEmpty()) lines += current
        return lines
    }
}
