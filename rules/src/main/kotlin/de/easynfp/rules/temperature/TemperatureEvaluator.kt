package de.easynfp.rules.temperature

import de.easynfp.rules.model.Temperature

/** A temperature that takes part in the evaluation: not excluded and already rounded (R-01, R-02). */
public data class ValidTemperature(val cycleDay: Int, val value: Temperature)

/**
 * Sensiplan temperature evaluation (R-03 to R-07).
 *
 * Input are only the valid values of one cycle. Excluded values and days without measurement are
 * not part of the list, so they are skipped "as if they did not exist" (R-02).
 */
public object TemperatureEvaluator {
    /** R-03: number of reference values before the first higher measurement. */
    public const val REFERENCE_COUNT: Int = 6

    public fun evaluate(
        values: List<ValidTemperature>,
        cycleCompleted: Boolean,
        manualFirstHigherDay: Int? = null,
    ): TemperatureEvaluation {
        val sorted = values.sortedBy { it.cycleDay }
        if (manualFirstHigherDay != null) return evaluateManual(sorted, manualFirstHigherDay, cycleCompleted)

        val discarded = mutableListOf<DiscardedCandidate>()
        for (index in REFERENCE_COUNT until sorted.size) {
            val reference = sorted.subList(index - REFERENCE_COUNT, index)
            val coverline = reference.maxOf { it.value }
            // R-03: higher than each of the six preceding valid values.
            if (sorted[index].value <= coverline) continue

            val outcome = assess(sorted, index, coverline)
            val shift = TemperatureShift(
                firstHigherDay = sorted[index].cycleDay,
                referenceDays = reference.map { it.cycleDay },
                coverline = coverline,
                measurements = outcome.measurements,
            )
            when (outcome) {
                is Outcome.Complete -> return TemperatureEvaluation.Completed(
                    shift,
                    outcome.rule,
                    outcome.completionDay,
                    discarded.toList(),
                )
                is Outcome.Pending -> return if (cycleCompleted) {
                    TemperatureEvaluation.NotEvaluable(
                        NotEvaluableReason.CYCLE_ENDED_WHILE_PENDING,
                        shift,
                        discarded.toList(),
                    )
                } else {
                    TemperatureEvaluation.Pending(shift, outcome.step, discarded.toList())
                }
                // R-07: no rule applies, look for the next possible first higher measurement.
                is Outcome.Fail -> discarded += DiscardedCandidate(
                    firstHigherDay = shift.firstHigherDay,
                    coverline = coverline,
                    reason = outcome.reason,
                    failedOnDay = outcome.failedOnDay,
                )
            }
        }
        return if (cycleCompleted) {
            TemperatureEvaluation.NotEvaluable(
                NotEvaluableReason.CYCLE_ENDED_WITHOUT_SHIFT,
                null,
                discarded.toList(),
            )
        } else {
            TemperatureEvaluation.Searching(sorted.size, discarded.toList())
        }
    }

    private fun evaluateManual(
        sorted: List<ValidTemperature>,
        day: Int,
        cycleCompleted: Boolean,
    ): TemperatureEvaluation {
        val index = sorted.indexOfFirst { it.cycleDay == day }
        if (index < 0) return TemperatureEvaluation.NotEvaluable(NotEvaluableReason.MANUAL_DAY_WITHOUT_VALUE)
        if (index < REFERENCE_COUNT) {
            return TemperatureEvaluation.NotEvaluable(NotEvaluableReason.MANUAL_DAY_TOO_FEW_REFERENCE_VALUES)
        }
        val reference = sorted.subList(index - REFERENCE_COUNT, index)
        val coverline = reference.maxOf { it.value }
        val baseShift = TemperatureShift(
            firstHigherDay = day,
            referenceDays = reference.map { it.cycleDay },
            coverline = coverline,
            measurements = listOf(HigherMeasurement(day, 1, sorted[index].value, counts = false)),
            manual = true,
        )
        if (sorted[index].value <= coverline) {
            return TemperatureEvaluation.NotEvaluable(NotEvaluableReason.MANUAL_DAY_NOT_HIGHER, baseShift)
        }
        val outcome = assess(sorted, index, coverline)
        val shift = baseShift.copy(measurements = outcome.measurements)
        return when (outcome) {
            is Outcome.Complete -> TemperatureEvaluation.Completed(shift, outcome.rule, outcome.completionDay)
            is Outcome.Pending -> if (cycleCompleted) {
                TemperatureEvaluation.NotEvaluable(NotEvaluableReason.CYCLE_ENDED_WHILE_PENDING, shift)
            } else {
                TemperatureEvaluation.Pending(shift, outcome.step)
            }
            is Outcome.Fail -> TemperatureEvaluation.NotEvaluable(NotEvaluableReason.MANUAL_SHIFT_FAILED, shift)
        }
    }

    /**
     * Applies R-04, R-05 and R-06 to the measurements following the first higher measurement at [index].
     * The numbers 2 to 4 are the "2., 3., 4. Messung" of the rule text.
     */
    @Suppress("ReturnCount", "MagicNumber")
    private fun assess(values: List<ValidTemperature>, index: Int, coverline: Temperature): Outcome {
        val first = values[index]
        val measured = mutableListOf(HigherMeasurement(first.cycleDay, 1, first.value, counts = true))
        val twoTenthsAbove = coverline + Temperature.TWO_TENTHS

        val second = values.getOrNull(index + 1) ?: return Outcome.Pending(PendingStep.SECOND_MEASUREMENT, measured)
        val secondAbove = second.value > coverline
        measured += HigherMeasurement(second.cycleDay, 2, second.value, secondAbove)

        val third = values.getOrNull(index + 2) ?: return Outcome.Pending(PendingStep.THIRD_MEASUREMENT, measured)
        val thirdAbove = third.value > coverline
        measured += HigherMeasurement(third.cycleDay, 3, third.value, thirdAbove)

        if (!secondAbove && !thirdAbove) {
            return Outcome.Fail(DiscardReason.SECOND_AND_THIRD_NOT_ABOVE, third.cycleDay, measured)
        }
        // R-04: second and third above the coverline, the third at least 0.2 °C above.
        if (secondAbove && thirdAbove && third.value >= twoTenthsAbove) {
            return Outcome.Complete(TemperatureRule.REGULAR, third.cycleDay, measured)
        }

        // Either R-05 (third above, but less than 0.2 °C) or R-06 (second or third fell). Never both (R-07).
        val exceptionOne = secondAbove && thirdAbove
        val fourth = values.getOrNull(index + 3)
            ?: return Outcome.Pending(
                if (exceptionOne) PendingStep.FOURTH_ABOVE_COVERLINE else PendingStep.FOURTH_TWO_TENTHS_ABOVE,
                measured,
            )

        return if (exceptionOne) {
            val fulfilled = fourth.value > coverline
            measured += HigherMeasurement(fourth.cycleDay, 4, fourth.value, fulfilled)
            if (fulfilled) {
                Outcome.Complete(TemperatureRule.EXCEPTION_1, fourth.cycleDay, measured)
            } else {
                Outcome.Fail(DiscardReason.FOURTH_NOT_ABOVE_COVERLINE, fourth.cycleDay, measured)
            }
        } else {
            val fulfilled = fourth.value >= twoTenthsAbove
            measured += HigherMeasurement(fourth.cycleDay, 4, fourth.value, fulfilled)
            if (fulfilled) {
                Outcome.Complete(TemperatureRule.EXCEPTION_2, fourth.cycleDay, measured)
            } else {
                Outcome.Fail(DiscardReason.FOURTH_NOT_TWO_TENTHS_ABOVE, fourth.cycleDay, measured)
            }
        }
    }

    private sealed interface Outcome {
        val measurements: List<HigherMeasurement>

        data class Complete(
            val rule: TemperatureRule,
            val completionDay: Int,
            override val measurements: List<HigherMeasurement>,
        ) : Outcome

        data class Pending(val step: PendingStep, override val measurements: List<HigherMeasurement>) : Outcome

        data class Fail(
            val reason: DiscardReason,
            val failedOnDay: Int,
            override val measurements: List<HigherMeasurement>,
        ) : Outcome
    }
}
