package de.easynfp.app.ui.statistics

import de.easynfp.data.EvaluationSnapshot
import de.easynfp.data.model.Intercourse
import de.easynfp.rules.CycleEvaluation
import de.easynfp.rules.Phase

/**
 * F-49: days with intercourse (F-14) in the selected cycles. Only counts entries; the evaluation
 * is not affected. Days without a clear status count as fertile ("Im Zweifel fruchtbar").
 */
data class IntercourseSummary(
    /** Days with intercourse per cycle, oldest first. */
    val perCycle: List<Int>,
    val protectedDays: Int,
    val unprotectedDays: Int,
    val infertileStartDays: Int,
    val fertileDays: Int,
    val fertileUnprotectedDays: Int,
    val infertileEndDays: Int,
    /** Days with intercourse in the current cycle so far, null without a current cycle. */
    val currentCycle: Int?,
) {
    val totalDays: Int get() = protectedDays + unprotectedDays
    val meanPerCycle: Double get() = if (perCycle.isEmpty()) 0.0 else totalDays.toDouble() / perCycle.size
}

object IntercourseStatistics {
    fun summarize(snapshot: EvaluationSnapshot, cycles: List<CycleEvaluation>): IntercourseSummary {
        val days = cycles.map { daysOf(snapshot, it) }
        val all = days.flatten()
        fun count(phase: Phase, kind: Intercourse? = null) =
            all.count { (p, i) -> p == phase && (kind == null || i == kind) }
        return IntercourseSummary(
            perCycle = days.map { it.size },
            protectedDays = all.count { it.second == Intercourse.PROTECTED },
            unprotectedDays = all.count { it.second == Intercourse.UNPROTECTED },
            infertileStartDays = count(Phase.INFERTILE_START),
            fertileDays = count(Phase.FERTILE),
            fertileUnprotectedDays = count(Phase.FERTILE, Intercourse.UNPROTECTED),
            infertileEndDays = count(Phase.INFERTILE_END),
            currentCycle = snapshot.current?.let { daysOf(snapshot, it).size },
        )
    }

    private fun daysOf(snapshot: EvaluationSnapshot, evaluation: CycleEvaluation): List<Pair<Phase, Intercourse>> =
        snapshot.entries.values
            .filter { it.intercourse != null && it.date in evaluation.cycle && !it.date.isAfter(snapshot.today) }
            .map { (evaluation.phaseOn(it.date)?.phase ?: Phase.FERTILE) to checkNotNull(it.intercourse) }
}
