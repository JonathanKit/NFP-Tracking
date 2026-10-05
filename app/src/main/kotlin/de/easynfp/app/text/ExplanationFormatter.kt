package de.easynfp.app.text

import android.content.res.Resources
import de.easynfp.app.R
import de.easynfp.rules.CycleEvaluation
import de.easynfp.rules.ExplanationStep
import de.easynfp.rules.Phase
import de.easynfp.rules.mucus.MucusRestartReason
import de.easynfp.rules.startphase.StartPhaseRule
import de.easynfp.rules.temperature.DiscardReason
import de.easynfp.rules.temperature.NotEvaluableReason
import de.easynfp.rules.temperature.PendingStep
import de.easynfp.rules.temperature.TemperatureEvaluation
import de.easynfp.rules.temperature.TemperatureRule

/** Turns the structured explanation of the rule engine into German sentences (F-41). */
class ExplanationFormatter(private val res: Resources) {

    /** One line such as "Temperatur ausgewertet am Tag 17 nach der regulären Regel; Schleimhöhepunkt Tag 15; …". */
    fun summary(evaluation: CycleEvaluation): String {
        val parts = mutableListOf<String>()
        val temperature = evaluation.temperature
        parts += if (temperature is TemperatureEvaluation.Completed) {
            res.getString(R.string.summary_temperature_done, temperature.completionDay, ruleName(temperature.rule))
        } else {
            res.getString(R.string.summary_temperature_open)
        }
        val mucus = evaluation.mucusCompleted
        parts += if (mucus != null) {
            res.getString(R.string.summary_peak, mucus.peakDay)
        } else {
            res.getString(R.string.summary_peak_open)
        }
        val from = evaluation.infertileFromEveningOfDay
        parts += if (from != null) {
            res.getString(R.string.summary_infertile_from, from)
        } else {
            res.getString(R.string.summary_fertile)
        }
        return parts.joinToString("; ")
    }

    fun steps(evaluation: CycleEvaluation): List<Line> = evaluation.explanation.map { step ->
        Line(ruleCode = step.ruleId.code, text = format(step))
    }

    data class Line(val ruleCode: String, val text: String)

    fun ruleName(rule: TemperatureRule): String = res.getString(
        when (rule) {
            TemperatureRule.REGULAR -> R.string.rule_regular
            TemperatureRule.EXCEPTION_1 -> R.string.rule_exception_1
            TemperatureRule.EXCEPTION_2 -> R.string.rule_exception_2
        },
    )

    fun phaseName(phase: Phase): String = res.getString(
        when (phase) {
            Phase.INFERTILE_START -> R.string.phase_infertile_start
            Phase.FERTILE -> R.string.phase_fertile
            Phase.INFERTILE_END -> R.string.phase_infertile_end
        },
    )

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun format(step: ExplanationStep): String = when (step) {
        is ExplanationStep.StartPhase -> startPhase(step)
        is ExplanationStep.StartPhaseEndedByMucus ->
            res.getString(R.string.explain_start_ended_by_mucus, step.day, step.category.symbol)
        is ExplanationStep.TemperatureCandidateDiscarded -> discarded(step)
        is ExplanationStep.TemperatureSearching ->
            res.getString(R.string.explain_temp_searching, step.validValueCount)
        is ExplanationStep.TemperaturePending -> {
            val text = res.getString(
                when (step.waitingFor) {
                    PendingStep.SECOND_MEASUREMENT -> R.string.explain_temp_pending_second
                    PendingStep.THIRD_MEASUREMENT -> R.string.explain_temp_pending_third
                    PendingStep.FOURTH_ABOVE_COVERLINE -> R.string.explain_temp_pending_fourth_above
                    PendingStep.FOURTH_TWO_TENTHS_ABOVE -> R.string.explain_temp_pending_fourth_two_tenths
                },
                step.shift.firstHigherDay,
                Formats.temperature(step.shift.coverline),
            )
            withManual(text, step.shift.manual)
        }
        is ExplanationStep.TemperatureCompleted -> withManual(
            res.getString(
                R.string.explain_temp_completed,
                step.completionDay,
                ruleName(step.rule),
                step.shift.firstHigherDay,
                Formats.temperature(step.shift.coverline),
                step.rule.ruleId.code,
            ),
            step.shift.manual,
        )
        is ExplanationStep.TemperatureNotEvaluable -> res.getString(
            when (step.reason) {
                NotEvaluableReason.CYCLE_ENDED_WITHOUT_SHIFT -> R.string.explain_temp_ended_without_shift
                NotEvaluableReason.CYCLE_ENDED_WHILE_PENDING -> R.string.explain_temp_ended_while_pending
                NotEvaluableReason.MANUAL_DAY_WITHOUT_VALUE -> R.string.explain_temp_manual_no_value
                NotEvaluableReason.MANUAL_DAY_TOO_FEW_REFERENCE_VALUES -> R.string.explain_temp_manual_too_few
                NotEvaluableReason.MANUAL_DAY_NOT_HIGHER -> R.string.explain_temp_manual_not_higher
                NotEvaluableReason.MANUAL_SHIFT_FAILED -> R.string.explain_temp_manual_failed
            },
        )
        ExplanationStep.MucusNoCandidate -> res.getString(R.string.explain_mucus_no_candidate)
        is ExplanationStep.MucusRestarted -> res.getString(
            when (step.restart.reason) {
                MucusRestartReason.SAME_QUALITY_DURING_COUNT -> R.string.explain_mucus_restart_same
                MucusRestartReason.HIGHER_QUALITY_DURING_COUNT -> R.string.explain_mucus_restart_higher
                MucusRestartReason.RETURN_AFTER_COMPLETION -> R.string.explain_mucus_restart_after
            },
            step.restart.cycleDay,
            step.restart.category.symbol,
        )
        is ExplanationStep.MucusCounting -> withManual(
            if (step.cycleEnded) {
                res.getString(R.string.explain_mucus_counting_ended, step.candidateDay)
            } else {
                res.getString(
                    R.string.explain_mucus_counting,
                    step.candidateDay,
                    step.category?.symbol ?: "–",
                    step.countedDays,
                )
            },
            step.manual,
        )
        is ExplanationStep.MucusMissingEntries -> res.getString(
            R.string.explain_mucus_missing,
            step.candidateDay,
            step.missingDays.joinToString(", "),
        )
        is ExplanationStep.MucusCompleted -> withManual(
            res.getString(
                R.string.explain_mucus_completed,
                step.peakDay,
                step.category?.symbol ?: "–",
                step.completionDay,
            ),
            step.manual,
        )
        is ExplanationStep.InfertileAfterOvulation -> res.getString(
            R.string.explain_infertile_after,
            step.day,
            res.getString(
                when {
                    step.decidedByTemperature && step.decidedByMucus -> R.string.decided_by_both
                    step.decidedByTemperature -> R.string.decided_by_temperature
                    else -> R.string.decided_by_mucus
                },
            ),
        )
        is ExplanationStep.FertileUntilBothEvaluated -> {
            val open = buildList {
                if (!step.temperatureDone) add(res.getString(R.string.sign_temperature))
                if (!step.mucusDone) add(res.getString(R.string.sign_mucus))
            }
            res.getString(R.string.explain_fertile_until_both, open.joinToString(", "))
        }
    }

    private fun startPhase(step: ExplanationStep.StartPhase): String {
        val last = step.lastInfertileDayByRule
        return when (step.rule) {
            StartPhaseRule.NO_PREVIOUS_CYCLE -> res.getString(R.string.explain_start_no_previous)
            StartPhaseRule.PREVIOUS_CYCLE_NOT_EVALUATED -> res.getString(R.string.explain_start_previous_not_evaluated)
            StartPhaseRule.FIVE_DAY_RULE -> res.getString(R.string.explain_start_five_day, step.evaluatedCurveCount)
            StartPhaseRule.MINUS_EIGHT_EARLY -> if (last != null) {
                res.getString(R.string.explain_start_minus_eight_early, step.earliestFirstHigherDay ?: 0, last)
            } else {
                res.getString(R.string.explain_start_minus_eight_none, step.earliestFirstHigherDay ?: 0)
            }
            StartPhaseRule.MINUS_EIGHT -> if (last != null) {
                res.getString(
                    R.string.explain_start_minus_eight,
                    step.evaluatedCurveCount,
                    step.earliestFirstHigherDay ?: 0,
                    last,
                )
            } else {
                res.getString(R.string.explain_start_minus_eight_none, step.earliestFirstHigherDay ?: 0)
            }
        }
    }

    private fun discarded(step: ExplanationStep.TemperatureCandidateDiscarded): String {
        val candidate = step.candidate
        return res.getString(
            when (candidate.reason) {
                DiscardReason.SECOND_AND_THIRD_NOT_ABOVE -> R.string.explain_temp_discarded_both
                DiscardReason.FOURTH_NOT_ABOVE_COVERLINE -> R.string.explain_temp_discarded_fourth_above
                DiscardReason.FOURTH_NOT_TWO_TENTHS_ABOVE -> R.string.explain_temp_discarded_fourth_two_tenths
            },
            candidate.firstHigherDay,
            Formats.temperature(candidate.coverline),
            candidate.failedOnDay,
        )
    }

    private fun withManual(text: String, manual: Boolean): String =
        if (manual) "$text ${res.getString(R.string.explain_manual_suffix)}" else text
}
