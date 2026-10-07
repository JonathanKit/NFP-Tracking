package de.easynfp.rules.statistics

import de.easynfp.rules.CycleEvaluation
import de.easynfp.rules.Phase
import de.easynfp.rules.startphase.PriorCycleSummary
import de.easynfp.rules.startphase.StartPhaseEvaluator
import java.time.LocalDate
import kotlin.math.roundToInt

/** F-47: forecast status of one future day. Never an evaluation result. */
public data class ForecastDay(
    val date: LocalDate,
    /** First day of the (current or forecast) cycle this day belongs to. */
    val cycleStart: LocalDate,
    val cycleDay: Int,
    val phase: Phase,
    /** The forecast infertile time after ovulation begins on the evening of this day. */
    val infertileFromEvening: Boolean = false,
    /** A period is expected on this day. */
    val expectedPeriod: Boolean = false,
)

/**
 * F-47: forecast of the coming days from the past cycles: fertile and infertile days and the next
 * period starts.
 *
 * This is a planning aid the user explicitly asked for, it is **not** part of the Sensiplan evaluation
 * (requirements, section 5: "bewusst nicht empfohlen"). It never changes an evaluation and every
 * screen must show it clearly distinguished from the evaluated days.
 */
public data class CycleForecast(
    val today: LocalDate,
    /** Forecast cycle length (rounded mean of the recent cycles). */
    val cycleLength: Int,
    /** Expected first day of the next period. */
    val nextPeriodStart: LocalDate,
    /** Expected number of bleeding days at the start of a cycle. */
    val periodDays: Int,
    /** Number of past cycles the forecast is based on. */
    val basedOnCycles: Int,
    private val days: Map<LocalDate, ForecastDay>,
) {
    /** Forecast for [date]; null for today, the past and dates beyond the forecast horizon. */
    public fun dayOn(date: LocalDate): ForecastDay? = days[date]

    /** Length of the current cycle according to the forecast. */
    public fun currentCycleLength(currentStart: LocalDate): Int =
        java.time.temporal.ChronoUnit.DAYS.between(currentStart, nextPeriodStart).toInt()
}

public object CycleForecaster {
    /** Number of most recent cycles the forecast uses. */
    public const val MAX_CYCLES: Int = 6

    /** Default horizon: about four cycles, enough for the calendar. */
    public const val DEFAULT_HORIZON_DAYS: Int = 120

    /**
     * @param evaluations all cycles in chronological order, as returned by the engine.
     * @param countsForStatistics false for cycles the user excluded from statistics (F-46).
     * @return null if there is no current cycle or no completed cycle to learn from.
     */
    public fun forecast(
        evaluations: List<CycleEvaluation>,
        today: LocalDate,
        countsForStatistics: (CycleEvaluation) -> Boolean = { true },
        horizonDays: Int = DEFAULT_HORIZON_DAYS,
    ): CycleForecast? {
        val current = evaluations.lastOrNull() ?: return null
        if (current.cycle.isCompleted || today !in current.cycle) return null
        val history = evaluations.filter { it.cycle.isCompleted && countsForStatistics(it) }.takeLast(MAX_CYCLES)
        if (history.isEmpty()) return null

        val cycleLength = history.map { requireNotNull(it.cycle.length) }.average().roundToInt()
        val periodDays = history.map(::bleedingDaysAtStart).average().roundToInt().coerceAtLeast(1)
        // Conservative: the latest day on which the infertile time after ovulation began.
        val infertileFrom = history.mapNotNull { it.infertileFromEveningOfDay }.maxOrNull()
        val todayDay = current.cycle.cycleDayOf(today)
        val currentLength = maxOf(cycleLength, todayDay)
        val nextStart = current.cycle.dateOf(currentLength + 1)
        val horizon = today.plusDays(horizonDays.toLong())

        val days = mutableMapOf<LocalDate, ForecastDay>()
        for (day in todayDay + 1..currentLength) {
            val date = current.cycle.dateOf(day)
            days[date] = currentCycleDay(current, date, day, todayDay, infertileFrom)
        }

        val startPhaseDay = forecastStartPhase(evaluations, history)
        var start = nextStart
        while (!start.isAfter(horizon)) {
            for (day in 1..cycleLength) {
                val date = start.plusDays(day - 1L)
                if (date.isAfter(horizon)) break
                days[date] = ForecastDay(
                    date = date,
                    cycleStart = start,
                    cycleDay = day,
                    phase = phaseOf(day, startPhaseDay, infertileFrom),
                    infertileFromEvening = day == infertileFrom,
                    expectedPeriod = day <= periodDays,
                )
            }
            start = start.plusDays(cycleLength.toLong())
        }
        return CycleForecast(today, cycleLength, nextStart, periodDays, history.size, days)
    }

    private fun currentCycleDay(
        current: CycleEvaluation,
        date: LocalDate,
        day: Int,
        todayDay: Int,
        forecastInfertileFrom: Int?,
    ): ForecastDay {
        // Already evaluated: the infertile time lasts until the next period (R-21).
        if (current.infertileFromEveningOfDay != null) {
            val phase = current.phaseOf(day)
            return ForecastDay(date, current.cycle.startDate, day, phase.phase, phase.infertileFromEvening)
        }
        // Not evaluated although the usual day has passed: stay fertile ("im Zweifel fruchtbar").
        val infertileFrom = forecastInfertileFrom?.takeIf { it > todayDay }
        val phase = phaseOf(day, current.startPhase.lastInfertileDay, infertileFrom)
        return ForecastDay(date, current.cycle.startDate, day, phase, infertileFromEvening = day == infertileFrom)
    }

    private fun phaseOf(day: Int, lastStartDay: Int?, infertileFrom: Int?): Phase = when {
        lastStartDay != null && day <= lastStartDay -> Phase.INFERTILE_START
        infertileFrom != null && day > infertileFrom -> Phase.INFERTILE_END
        else -> Phase.FERTILE
    }

    /**
     * Last infertile day at the start of future cycles, assuming the coming cycles are evaluated like
     * the recent ones (R-30 to R-33, without mucus).
     */
    private fun forecastStartPhase(evaluations: List<CycleEvaluation>, history: List<CycleEvaluation>): Int? {
        val latestFirstHigher = history.mapNotNull { it.temperatureCompleted?.shift?.firstHigherDay }.maxOrNull()
            ?: return null
        val priors = evaluations.map { evaluation ->
            val completed = evaluation.temperatureCompleted
            if (completed != null || evaluation.cycle.isCompleted) {
                PriorCycleSummary(completed != null, completed?.shift?.firstHigherDay)
            } else {
                // The current cycle, assumed to be evaluated with a typical first higher measurement.
                PriorCycleSummary(temperatureEvaluated = true, firstHigherDay = latestFirstHigher)
            }
        }
        return StartPhaseEvaluator.evaluate(priors, emptyMap()).lastInfertileDayByRule
    }

    /** Consecutive days with bleeding from cycle day 1. */
    private fun bleedingDaysAtStart(evaluation: CycleEvaluation): Int {
        val bleedingDays = evaluation.cycle.observations
            .filter { it.bleeding != null }
            .map { evaluation.cycle.cycleDayOf(it.date) }
            .toSet()
        return generateSequence(1) { it + 1 }.takeWhile { it in bleedingDays }.count()
    }
}
