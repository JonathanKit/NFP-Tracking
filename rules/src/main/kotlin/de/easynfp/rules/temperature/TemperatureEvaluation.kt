package de.easynfp.rules.temperature

import de.easynfp.rules.RuleId
import de.easynfp.rules.model.Temperature

/** The rule that completed the temperature evaluation. */
public enum class TemperatureRule(public val ruleId: RuleId) {
    /** R-04 */
    REGULAR(RuleId.R04),

    /** R-05: third measurement above the coverline but less than 0.2 °C, fourth above the coverline. */
    EXCEPTION_1(RuleId.R05),

    /** R-06: second or third measurement on or below the coverline, fourth at least 0.2 °C above. */
    EXCEPTION_2(RuleId.R06),
}

/**
 * One measurement after the six reference values.
 * [number] is 1 for the first higher measurement up to 4 for a fourth measurement awaited by an exception rule.
 * [counts] is false for a second or third measurement that fell on or below the coverline (R-06).
 */
public data class HigherMeasurement(
    val cycleDay: Int,
    val number: Int,
    val temperature: Temperature,
    val counts: Boolean,
)

/** A (possible) temperature shift: first higher measurement, its six reference values and the coverline. */
public data class TemperatureShift(
    /** R-03: cycle day of the first higher measurement. */
    val firstHigherDay: Int,
    /** R-03: cycle days of the six valid values directly before the first higher measurement. */
    val referenceDays: List<Int>,
    /** R-03: "Hilfslinie", the highest of the six reference values (rounded per R-01). */
    val coverline: Temperature,
    /** The first higher measurement and the following measurements that are known so far. */
    val measurements: List<HigherMeasurement>,
    /** F-42: set by the user instead of found automatically. */
    val manual: Boolean = false,
)

/** What a pending evaluation is waiting for. */
public enum class PendingStep {
    /** The first higher measurement is known, the second is missing. */
    SECOND_MEASUREMENT,

    /** First and second are known, the third is missing. */
    THIRD_MEASUREMENT,

    /** R-05: waiting for a fourth measurement above the coverline. */
    FOURTH_ABOVE_COVERLINE,

    /** R-06: waiting for a fourth measurement at least 0.2 °C above the coverline. */
    FOURTH_TWO_TENTHS_ABOVE,
}

/** Why a possible first higher measurement was discarded (R-07). */
public enum class DiscardReason {
    /** Second and third measurement were both on or below the coverline. */
    SECOND_AND_THIRD_NOT_ABOVE,

    /** R-05 applied, but the fourth measurement was not above the coverline. */
    FOURTH_NOT_ABOVE_COVERLINE,

    /** R-06 applied, but the fourth measurement was not at least 0.2 °C above the coverline. */
    FOURTH_NOT_TWO_TENTHS_ABOVE,
}

public data class DiscardedCandidate(
    val firstHigherDay: Int,
    val coverline: Temperature,
    val reason: DiscardReason,
    /** Cycle day of the measurement that made the candidate fail. */
    val failedOnDay: Int,
)

public enum class NotEvaluableReason {
    /** The cycle ended without any first higher measurement that fulfilled a rule. */
    CYCLE_ENDED_WITHOUT_SHIFT,

    /** The cycle ended while the evaluation was still waiting for measurements. */
    CYCLE_ENDED_WHILE_PENDING,

    /** F-42: the manually chosen day has no valid temperature. */
    MANUAL_DAY_WITHOUT_VALUE,

    /** F-42: fewer than six valid values precede the manually chosen day. */
    MANUAL_DAY_TOO_FEW_REFERENCE_VALUES,

    /** F-42: the manually chosen value is not higher than the six values before it. */
    MANUAL_DAY_NOT_HIGHER,

    /** F-42: the measurements after the manually chosen day fulfil no rule. */
    MANUAL_SHIFT_FAILED,
}

/** Result of the temperature evaluation of one cycle (R-01 to R-07). */
public sealed interface TemperatureEvaluation {
    /** Candidates that fulfilled no rule (R-07), in chronological order. */
    public val discarded: List<DiscardedCandidate>

    /** The shift to draw in the chart, if any. */
    public val shift: TemperatureShift?

    /** No first higher measurement yet. [validValueCount] counts the usable values so far. */
    public data class Searching(
        val validValueCount: Int,
        override val discarded: List<DiscardedCandidate> = emptyList(),
    ) : TemperatureEvaluation {
        override val shift: TemperatureShift? get() = null
    }

    /** A first higher measurement exists, further measurements are needed. */
    public data class Pending(
        override val shift: TemperatureShift,
        val waitingFor: PendingStep,
        override val discarded: List<DiscardedCandidate> = emptyList(),
    ) : TemperatureEvaluation

    /** The temperature is evaluated on [completionDay]. */
    public data class Completed(
        override val shift: TemperatureShift,
        val rule: TemperatureRule,
        val completionDay: Int,
        override val discarded: List<DiscardedCandidate> = emptyList(),
    ) : TemperatureEvaluation

    /** The cycle cannot be evaluated; it stays fertile (R-22). */
    public data class NotEvaluable(
        val reason: NotEvaluableReason,
        override val shift: TemperatureShift? = null,
        override val discarded: List<DiscardedCandidate> = emptyList(),
    ) : TemperatureEvaluation
}
