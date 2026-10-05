package de.easynfp.rules

import de.easynfp.rules.model.MucusCategory
import de.easynfp.rules.mucus.MucusRestart
import de.easynfp.rules.startphase.StartPhaseRule
import de.easynfp.rules.temperature.DiscardedCandidate
import de.easynfp.rules.temperature.NotEvaluableReason
import de.easynfp.rules.temperature.PendingStep
import de.easynfp.rules.temperature.TemperatureRule
import de.easynfp.rules.temperature.TemperatureShift

/**
 * One step of the plain text justification of an evaluation (F-41).
 *
 * The rule module only produces structured steps; the UI turns them into localized sentences.
 * All day numbers are cycle days.
 */
public sealed interface ExplanationStep {
    public val ruleId: RuleId

    // --- Beginning of the cycle (R-30 to R-34) ---

    public data class StartPhase(
        val rule: StartPhaseRule,
        val lastInfertileDayByRule: Int?,
        val evaluatedCurveCount: Int,
        val earliestFirstHigherDay: Int?,
    ) : ExplanationStep {
        override val ruleId: RuleId get() = rule.ruleId
    }

    public data class StartPhaseEndedByMucus(val day: Int, val category: MucusCategory) : ExplanationStep {
        override val ruleId: RuleId get() = RuleId.R34
    }

    // --- Temperature (R-03 to R-07) ---

    public data class TemperatureCandidateDiscarded(val candidate: DiscardedCandidate) : ExplanationStep {
        override val ruleId: RuleId get() = RuleId.R07
    }

    public data class TemperatureSearching(val validValueCount: Int) : ExplanationStep {
        override val ruleId: RuleId get() = RuleId.R03
    }

    public data class TemperaturePending(val shift: TemperatureShift, val waitingFor: PendingStep) : ExplanationStep {
        override val ruleId: RuleId get() = if (shift.manual) RuleId.MANUAL else RuleId.R03
    }

    public data class TemperatureCompleted(
        val shift: TemperatureShift,
        val rule: TemperatureRule,
        val completionDay: Int,
    ) : ExplanationStep {
        override val ruleId: RuleId get() = rule.ruleId
    }

    public data class TemperatureNotEvaluable(val reason: NotEvaluableReason, val shift: TemperatureShift?) :
        ExplanationStep {
        override val ruleId: RuleId get() = RuleId.R22
    }

    // --- Cervical mucus (R-12 to R-16) ---

    public data object MucusNoCandidate : ExplanationStep {
        override val ruleId: RuleId get() = RuleId.R12
    }

    public data class MucusRestarted(val restart: MucusRestart) : ExplanationStep {
        override val ruleId: RuleId get() = restart.reason.ruleId
    }

    public data class MucusCounting(
        val candidateDay: Int,
        val category: MucusCategory?,
        val countedDays: Int,
        val manual: Boolean,
        val cycleEnded: Boolean,
    ) : ExplanationStep {
        override val ruleId: RuleId get() = if (manual) RuleId.MANUAL else RuleId.R12
    }

    public data class MucusMissingEntries(val candidateDay: Int, val missingDays: List<Int>) : ExplanationStep {
        override val ruleId: RuleId get() = RuleId.R16
    }

    public data class MucusCompleted(
        val peakDay: Int,
        val category: MucusCategory?,
        val completionDay: Int,
        val manual: Boolean,
    ) : ExplanationStep {
        override val ruleId: RuleId get() = if (manual) RuleId.MANUAL else RuleId.R13
    }

    // --- Combined result (R-20 to R-22) ---

    /** R-20: infertile from the evening of [day]; [decidedByTemperature]/[decidedByMucus] tell which ended later. */
    public data class InfertileAfterOvulation(
        val day: Int,
        val decidedByTemperature: Boolean,
        val decidedByMucus: Boolean,
    ) : ExplanationStep {
        override val ruleId: RuleId get() = RuleId.R20
    }

    /** R-22: fertile until both signs are evaluated. */
    public data class FertileUntilBothEvaluated(val temperatureDone: Boolean, val mucusDone: Boolean) :
        ExplanationStep {
        override val ruleId: RuleId get() = RuleId.R22
    }
}
