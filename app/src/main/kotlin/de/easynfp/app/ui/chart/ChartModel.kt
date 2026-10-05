package de.easynfp.app.ui.chart

import de.easynfp.data.model.DayEntry
import de.easynfp.data.model.Intercourse
import de.easynfp.rules.CycleEvaluation
import de.easynfp.rules.DayPhase
import de.easynfp.rules.model.Bleeding
import de.easynfp.rules.model.MucusCategory
import de.easynfp.rules.model.Temperature
import de.easynfp.rules.mucus.MucusEvaluation
import java.time.LocalDate

/** One day column of the cycle sheet (F-30, F-31). */
data class ChartColumn(
    val cycleDay: Int,
    val date: LocalDate,
    val temperature: Temperature?,
    /** Value as plotted: rounded to 0.05 °C like on the paper chart (R-01). */
    val plotted: Temperature?,
    val excluded: Boolean,
    val mucus: MucusCategory?,
    val bleeding: Bleeding?,
    val hasDisturbance: Boolean,
    val intercourse: Intercourse?,
    /** Null for days in the future. */
    val phase: DayPhase?,
    val isToday: Boolean,
    /** 1–6 for the six reference values of the first higher measurement (R-03). */
    val referenceNumber: Int? = null,
    /** 1–4 for the higher measurements (R-04 to R-06). */
    val higherNumber: Int? = null,
    val higherCounts: Boolean = true,
    /** "H" for the mucus peak and "1", "2", "3" for the following days (R-12, R-13). */
    val peakMarker: String? = null,
)

data class ChartModel(
    val columns: List<ChartColumn>,
    val coverline: Temperature?,
    val coverlineFromDay: Int?,
    val coverlineToDay: Int?,
    /** Plot range in hundredths, multiples of 5. */
    val yMin: Int,
    val yMax: Int,
    val showIntercourse: Boolean,
) {
    companion object {
        private const val STEP = 5
        private const val MARGIN = 10
        private const val MIN_RANGE = 80
        private const val DEFAULT_MIN = 3620
        private const val DEFAULT_MAX = 3700

        /** Minimum number of columns shown for the current cycle, like a printed chart. */
        const val MIN_COLUMNS_CURRENT = 28

        fun build(
            evaluation: CycleEvaluation,
            entries: Map<LocalDate, DayEntry>,
            today: LocalDate,
            showIntercourse: Boolean,
        ): ChartModel {
            val cycle = evaluation.cycle
            val columnCount = cycle.length ?: maxOf(evaluation.dayCount, MIN_COLUMNS_CURRENT)
            val shift = evaluation.temperature.shift
            val references = shift?.referenceDays.orEmpty()
            val higher = shift?.measurements.orEmpty().associateBy { it.cycleDay }
            val peakMarkers = peakMarkers(evaluation.mucus)

            val columns = (1..columnCount).map { day ->
                val date = cycle.dateOf(day)
                val entry = entries[date]
                val measurement = higher[day]
                ChartColumn(
                    cycleDay = day,
                    date = date,
                    temperature = entry?.temperature,
                    plotted = entry?.temperature?.roundedForEvaluation(),
                    excluded = entry?.temperatureExcluded == true,
                    mucus = entry?.mucusCategory,
                    bleeding = entry?.bleeding,
                    hasDisturbance = entry?.disturbanceIds?.isNotEmpty() == true,
                    intercourse = entry?.intercourse,
                    phase = if (day <= evaluation.dayCount) evaluation.phaseOf(day) else null,
                    isToday = date == today,
                    referenceNumber = references.indexOf(day).takeIf { it >= 0 }?.plus(1),
                    higherNumber = measurement?.number,
                    higherCounts = measurement?.counts ?: true,
                    peakMarker = peakMarkers[day],
                )
            }

            val values = columns.mapNotNull { it.plotted?.hundredths }
            val (yMin, yMax) = if (values.isEmpty()) {
                DEFAULT_MIN to DEFAULT_MAX
            } else {
                var low = floorTo(values.min() - MARGIN)
                var high = ceilTo(values.max() + MARGIN)
                val missing = MIN_RANGE - (high - low)
                if (missing > 0) {
                    low -= ceilTo(missing / 2)
                    high = maxOf(high, low + MIN_RANGE)
                }
                low to high
            }
            val lastMeasurement = shift?.measurements?.maxOfOrNull { it.cycleDay }
            return ChartModel(
                columns = columns,
                coverline = shift?.coverline,
                coverlineFromDay = references.firstOrNull(),
                coverlineToDay = lastMeasurement,
                yMin = yMin,
                yMax = yMax,
                showIntercourse = showIntercourse,
            )
        }

        private fun floorTo(value: Int): Int = Math.floorDiv(value, STEP) * STEP

        private fun ceilTo(value: Int): Int = -Math.floorDiv(-value, STEP) * STEP

        private fun peakMarkers(mucus: MucusEvaluation): Map<Int, String> = when (mucus) {
            is MucusEvaluation.Completed -> mapOf(
                mucus.peakDay to "H",
                mucus.peakDay + 1 to "1",
                mucus.peakDay + 2 to "2",
                mucus.peakDay + 3 to "3",
            )
            is MucusEvaluation.Counting -> buildMap {
                put(mucus.candidateDay, "H?")
                (1..mucus.countedDays).forEach { put(mucus.candidateDay + it, it.toString()) }
            }
            else -> emptyMap()
        }
    }
}
