package de.easynfp.rules.statistics

import de.easynfp.rules.CycleEvaluation
import de.easynfp.rules.Phase
import kotlin.math.sqrt

/** Mean, minimum, maximum and (population) standard deviation of a series of day counts. */
public data class NumberSummary(
    val count: Int,
    val mean: Double,
    val min: Int,
    val max: Int,
    val standardDeviation: Double,
) {
    public companion object {
        public fun of(values: List<Int>): NumberSummary? {
            if (values.isEmpty()) return null
            val mean = values.average()
            val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
            return NumberSummary(values.size, mean, values.min(), values.max(), sqrt(variance))
        }
    }
}

/** Key figures of one completed cycle, used for statistics and charts (F-43, F-44). */
public data class CycleFigures(
    val evaluation: CycleEvaluation,
    val length: Int,
    val firstHigherDay: Int?,
    /** Days from the first higher measurement up to the end of the cycle. */
    val highPhaseLength: Int?,
    /** Fertile days, only if the cycle has an infertile time after ovulation. */
    val fertileDays: Int?,
    val infertileStartDays: Int,
    /** Days after the evening on which the infertile time after ovulation started. */
    val infertileEndDays: Int?,
)

/** F-43: statistics over a selection of cycles. */
public data class StatisticsSummary(
    val cycleCount: Int,
    val cycleLength: NumberSummary?,
    val firstHigherDay: NumberSummary?,
    val highPhaseLength: NumberSummary?,
    val fertilePhaseLength: NumberSummary?,
    val infertileStartDays: NumberSummary?,
    val infertileEndDays: NumberSummary?,
)

public object CycleStatistics {
    /** Key figures for a completed cycle, null for the current cycle. */
    public fun figuresOf(evaluation: CycleEvaluation): CycleFigures? {
        val length = evaluation.cycle.length ?: return null
        val firstHigher = evaluation.temperatureCompleted?.shift?.firstHigherDay
        val infertileFrom = evaluation.infertileFromEveningOfDay
        val phases = (1..length).map(evaluation::phaseOf)
        return CycleFigures(
            evaluation = evaluation,
            length = length,
            firstHigherDay = firstHigher,
            highPhaseLength = firstHigher?.let { length - it + 1 },
            fertileDays = infertileFrom?.let { phases.count { it.phase == Phase.FERTILE } },
            infertileStartDays = phases.count { it.phase == Phase.INFERTILE_START },
            infertileEndDays = infertileFrom?.let { length - it },
        )
    }

    /** Statistics for the given completed cycles; the current cycle is ignored. */
    public fun summarize(evaluations: List<CycleEvaluation>): StatisticsSummary {
        val figures = evaluations.mapNotNull(::figuresOf)
        return StatisticsSummary(
            cycleCount = figures.size,
            cycleLength = NumberSummary.of(figures.map { it.length }),
            firstHigherDay = NumberSummary.of(figures.mapNotNull { it.firstHigherDay }),
            highPhaseLength = NumberSummary.of(figures.mapNotNull { it.highPhaseLength }),
            fertilePhaseLength = NumberSummary.of(figures.mapNotNull { it.fertileDays }),
            infertileStartDays = NumberSummary.of(figures.map { it.infertileStartDays }),
            infertileEndDays = NumberSummary.of(figures.mapNotNull { it.infertileEndDays }),
        )
    }
}
