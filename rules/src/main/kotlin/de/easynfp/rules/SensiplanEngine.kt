package de.easynfp.rules

import de.easynfp.rules.model.CycleData
import de.easynfp.rules.model.MucusCategory
import de.easynfp.rules.mucus.MucusEvaluation
import de.easynfp.rules.mucus.MucusEvaluator
import de.easynfp.rules.startphase.PriorCycleSummary
import de.easynfp.rules.startphase.StartPhaseEvaluation
import de.easynfp.rules.startphase.StartPhaseEvaluator
import de.easynfp.rules.temperature.TemperatureEvaluation
import de.easynfp.rules.temperature.TemperatureEvaluator
import de.easynfp.rules.temperature.ValidTemperature
import java.time.LocalDate

public enum class Phase {
    /** Infertile time at the beginning of the cycle (R-30 to R-34). */
    INFERTILE_START,

    /** Fertile, or not (yet) evaluable. "Im Zweifel fruchtbar." */
    FERTILE,

    /** Infertile time after ovulation (R-20, R-21). */
    INFERTILE_END,
}

/**
 * Status of one cycle day.
 * [infertileFromEvening] marks the day on whose evening the infertile time after ovulation begins (R-20);
 * the day itself counts as fertile until then.
 */
public data class DayPhase(val cycleDay: Int, val phase: Phase, val infertileFromEvening: Boolean = false)

/** Complete evaluation of one cycle. */
public data class CycleEvaluation(
    val cycle: CycleData,
    /** Number of days that are evaluated: up to today for the current cycle, otherwise the cycle length. */
    val dayCount: Int,
    val temperature: TemperatureEvaluation,
    val mucus: MucusEvaluation,
    val startPhase: StartPhaseEvaluation,
    /** R-20: the infertile time after ovulation starts on the evening of this cycle day. */
    val infertileFromEveningOfDay: Int?,
    val explanation: List<ExplanationStep>,
) {
    val temperatureCompleted: TemperatureEvaluation.Completed? get() = temperature as? TemperatureEvaluation.Completed

    val mucusCompleted: MucusEvaluation.Completed? get() = mucus as? MucusEvaluation.Completed

    /** Status of [cycleDay]. Days after [dayCount] are reported as well, but only for display of the past. */
    public fun phaseOf(cycleDay: Int): DayPhase {
        val lastStartDay = startPhase.lastInfertileDay
        val postDay = infertileFromEveningOfDay
        return when {
            lastStartDay != null && cycleDay <= lastStartDay -> DayPhase(cycleDay, Phase.INFERTILE_START)
            postDay != null && cycleDay > postDay -> DayPhase(cycleDay, Phase.INFERTILE_END)
            else -> DayPhase(cycleDay, Phase.FERTILE, infertileFromEvening = cycleDay == postDay)
        }
    }

    val dayPhases: List<DayPhase> get() = (1..dayCount).map(::phaseOf)

    public fun phaseOn(date: LocalDate): DayPhase? =
        if (date in cycle) phaseOf(cycle.cycleDayOf(date)).takeIf { it.cycleDay <= dayCount } else null
}

/**
 * Evaluates cycles according to Sensiplan (docs/rules.md).
 *
 * The engine is stateless and deterministic: the same input always gives the same result, so it is
 * simply re-run after every data change (F-40, F-13).
 */
public object SensiplanEngine {

    /**
     * @param cycles all cycles; they are evaluated in chronological order because the beginning of a
     *   cycle depends on the earlier ones (R-30 to R-33).
     * @param today observations after today are ignored for the current cycle.
     */
    public fun evaluate(cycles: List<CycleData>, today: LocalDate): List<CycleEvaluation> {
        val priorSummaries = mutableListOf<PriorCycleSummary>()
        return cycles.sortedBy { it.startDate }.map { cycle ->
            val evaluation = evaluateCycle(cycle, today, priorSummaries.toList())
            val completed = evaluation.temperatureCompleted
            priorSummaries += PriorCycleSummary(
                temperatureEvaluated = completed != null,
                firstHigherDay = completed?.shift?.firstHigherDay,
            )
            evaluation
        }
    }

    public fun evaluateCycle(
        cycle: CycleData,
        today: LocalDate,
        priorCycles: List<PriorCycleSummary>,
    ): CycleEvaluation {
        val dayCount = cycle.length ?: cycle.cycleDayOf(today).coerceAtLeast(0)
        val observations = cycle.observations
            .filter { it.date in cycle }
            .associateBy { cycle.cycleDayOf(it.date) }
            .filterKeys { it in 1..dayCount }

        val values = observations.mapNotNull { (day, obs) ->
            obs.evaluationTemperature?.let { ValidTemperature(day, it) }
        }
        val categories: Map<Int, MucusCategory> = observations.mapNotNull { (day, obs) ->
            obs.mucusCategory?.let { day to it }
        }.toMap()

        val manualHigherDay = cycle.overrides.firstHigherMeasurement?.takeIf { it in cycle }?.let(cycle::cycleDayOf)
        val manualPeakDay = cycle.overrides.mucusPeak?.takeIf { it in cycle }?.let(cycle::cycleDayOf)

        val temperature = TemperatureEvaluator.evaluate(values, cycle.isCompleted, manualHigherDay)
        val temperatureDay = (temperature as? TemperatureEvaluation.Completed)?.completionDay
        val mucus = MucusEvaluator.evaluate(
            categories = categories,
            lastDay = dayCount,
            lastDayMayStillBeEntered = !cycle.isCompleted,
            temperatureCompletionDay = temperatureDay,
            manualPeakDay = manualPeakDay,
        )
        val mucusDay = (mucus as? MucusEvaluation.Completed)?.completionDay
        val startPhase = StartPhaseEvaluator.evaluate(priorCycles, categories)

        // R-20: double check, the later of both evaluations decides.
        val infertileFrom = if (temperatureDay != null && mucusDay != null) maxOf(temperatureDay, mucusDay) else null

        val explanation = buildExplanation(cycle, startPhase, temperature, mucus, temperatureDay, mucusDay)
        return CycleEvaluation(cycle, dayCount, temperature, mucus, startPhase, infertileFrom, explanation)
    }

    private fun buildExplanation(
        cycle: CycleData,
        startPhase: StartPhaseEvaluation,
        temperature: TemperatureEvaluation,
        mucus: MucusEvaluation,
        temperatureDay: Int?,
        mucusDay: Int?,
    ): List<ExplanationStep> = buildList {
        add(
            ExplanationStep.StartPhase(
                rule = startPhase.rule,
                lastInfertileDayByRule = startPhase.lastInfertileDayByRule,
                evaluatedCurveCount = startPhase.evaluatedCurveCount,
                earliestFirstHigherDay = startPhase.earliestFirstHigherDay,
            ),
        )
        val mucusEndDay = startPhase.endedByMucusOnDay
        val mucusEndCategory = startPhase.endedByMucusCategory
        if (mucusEndDay != null && mucusEndCategory != null) {
            add(ExplanationStep.StartPhaseEndedByMucus(mucusEndDay, mucusEndCategory))
        }

        temperature.discarded.forEach { add(ExplanationStep.TemperatureCandidateDiscarded(it)) }
        when (temperature) {
            is TemperatureEvaluation.Searching -> add(ExplanationStep.TemperatureSearching(temperature.validValueCount))
            is TemperatureEvaluation.Pending -> add(
                ExplanationStep.TemperaturePending(temperature.shift, temperature.waitingFor),
            )
            is TemperatureEvaluation.Completed -> add(
                ExplanationStep.TemperatureCompleted(temperature.shift, temperature.rule, temperature.completionDay),
            )
            is TemperatureEvaluation.NotEvaluable -> add(
                ExplanationStep.TemperatureNotEvaluable(temperature.reason, temperature.shift),
            )
        }

        mucus.restarts.forEach { add(ExplanationStep.MucusRestarted(it)) }
        when (mucus) {
            is MucusEvaluation.NoCandidate -> add(ExplanationStep.MucusNoCandidate)
            is MucusEvaluation.Counting -> add(
                ExplanationStep.MucusCounting(
                    candidateDay = mucus.candidateDay,
                    category = mucus.category,
                    countedDays = mucus.countedDays,
                    manual = mucus.manual,
                    cycleEnded = cycle.isCompleted,
                ),
            )
            is MucusEvaluation.MissingEntries -> add(
                ExplanationStep.MucusMissingEntries(mucus.candidateDay, mucus.missingDays),
            )
            is MucusEvaluation.Completed -> add(
                ExplanationStep.MucusCompleted(mucus.peakDay, mucus.category, mucus.completionDay, mucus.manual),
            )
        }

        if (temperatureDay != null && mucusDay != null) {
            val day = maxOf(temperatureDay, mucusDay)
            add(
                ExplanationStep.InfertileAfterOvulation(
                    day = day,
                    decidedByTemperature = temperatureDay == day,
                    decidedByMucus = mucusDay == day,
                ),
            )
        } else {
            add(ExplanationStep.FertileUntilBothEvaluated(temperatureDay != null, mucusDay != null))
        }
    }
}
