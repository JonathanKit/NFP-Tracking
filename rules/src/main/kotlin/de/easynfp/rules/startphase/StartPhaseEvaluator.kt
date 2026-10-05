package de.easynfp.rules.startphase

import de.easynfp.rules.RuleId
import de.easynfp.rules.model.MucusCategory

/** What the start phase evaluation needs to know about an earlier cycle. */
public data class PriorCycleSummary(
    /** True if the temperature evaluation of that cycle was completed (R-04 to R-06). */
    val temperatureEvaluated: Boolean,
    /** Cycle day of the first higher measurement of the completed evaluation. */
    val firstHigherDay: Int?,
)

public enum class StartPhaseRule(public val ruleId: RuleId) {
    /** R-30: no earlier cycle is known, so the cycle is fertile from day 1. */
    NO_PREVIOUS_CYCLE(RuleId.R30),

    /** R-30: the previous cycle had no evaluated temperature shift. */
    PREVIOUS_CYCLE_NOT_EVALUATED(RuleId.R30),

    /** R-31: first 5 days. */
    FIVE_DAY_RULE(RuleId.R31),

    /** R-32: earliest first higher measurement minus 8, triggered during the first 12 cycles. */
    MINUS_EIGHT_EARLY(RuleId.R32),

    /** R-33: earliest first higher measurement minus 8, from 12 evaluated cycles on. */
    MINUS_EIGHT(RuleId.R33),
}

public data class StartPhaseEvaluation(
    val rule: StartPhaseRule,
    /** Last infertile day according to [rule], before looking at mucus. Null if there is none. */
    val lastInfertileDayByRule: Int?,
    /** R-34: first day with mucus f, S or S+ inside the phase, which ends it. */
    val endedByMucusOnDay: Int?,
    val endedByMucusCategory: MucusCategory?,
    /** Number of earlier cycles with evaluated temperature curve. */
    val evaluatedCurveCount: Int,
    /** Earliest first higher measurement of all earlier evaluated cycles. */
    val earliestFirstHigherDay: Int?,
) {
    /** Last day of the infertile phase at the beginning of the cycle, or null if there is none. */
    val lastInfertileDay: Int?
        get() {
            val byRule = lastInfertileDayByRule ?: return null
            val limit = endedByMucusOnDay?.let { it - 1 } ?: byRule
            return minOf(byRule, limit).takeIf { it >= 1 }
        }
}

/** Sensiplan rules for the infertile time at the beginning of the cycle (R-30 to R-34). */
public object StartPhaseEvaluator {
    /** R-31 */
    public const val FIVE_DAYS: Int = 5

    /** R-31, R-33: number of evaluated curves after which the minus-8 rule applies. */
    public const val CURVES_FOR_MINUS_EIGHT: Int = 12

    /** R-32: a first higher measurement on this day or earlier switches to the minus-8 rule. */
    public const val EARLY_FIRST_HIGHER_DAY: Int = 12

    /** R-32, R-33 */
    public const val MINUS_EIGHT: Int = 8

    /**
     * @param priorCycles all earlier cycles in chronological order; the last one is the previous cycle.
     * @param categories mucus category per cycle day of the cycle being evaluated.
     */
    public fun evaluate(
        priorCycles: List<PriorCycleSummary>,
        categories: Map<Int, MucusCategory>,
    ): StartPhaseEvaluation {
        val evaluated = priorCycles.filter { it.temperatureEvaluated && it.firstHigherDay != null }
        val earliest = evaluated.minOfOrNull { requireNotNull(it.firstHigherDay) }
        val count = evaluated.size

        val (rule, lastDayByRule) = when {
            priorCycles.isEmpty() -> StartPhaseRule.NO_PREVIOUS_CYCLE to null
            !priorCycles.last().temperatureEvaluated -> StartPhaseRule.PREVIOUS_CYCLE_NOT_EVALUATED to null
            count >= CURVES_FOR_MINUS_EIGHT -> StartPhaseRule.MINUS_EIGHT to requireNotNull(earliest) - MINUS_EIGHT
            earliest != null && earliest <= EARLY_FIRST_HIGHER_DAY ->
                StartPhaseRule.MINUS_EIGHT_EARLY to earliest - MINUS_EIGHT
            else -> StartPhaseRule.FIVE_DAY_RULE to FIVE_DAYS
        }
        val lastByRule = lastDayByRule?.takeIf { it >= 1 }

        // R-34: f, S or S+ ends the phase immediately.
        val mucusDay = lastByRule?.let { last ->
            (1..last).firstOrNull { day -> categories[day]?.isFertileQuality == true }
        }
        return StartPhaseEvaluation(
            rule = rule,
            lastInfertileDayByRule = lastByRule,
            endedByMucusOnDay = mucusDay,
            endedByMucusCategory = mucusDay?.let { categories[it] },
            evaluatedCurveCount = count,
            earliestFirstHigherDay = earliest,
        )
    }
}
