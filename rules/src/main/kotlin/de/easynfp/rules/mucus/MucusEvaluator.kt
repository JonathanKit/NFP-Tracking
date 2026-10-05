package de.easynfp.rules.mucus

import de.easynfp.rules.RuleId
import de.easynfp.rules.model.MucusCategory

public enum class MucusRestartReason(public val ruleId: RuleId) {
    /** R-14: mucus of the peak's category returned during the count 1–2–3. */
    SAME_QUALITY_DURING_COUNT(RuleId.R14),

    /** R-12: a better category appeared during the count, so the peak moves. */
    HIGHER_QUALITY_DURING_COUNT(RuleId.R12),

    /** R-15: mucus of the same or better category after the mucus evaluation, before the temperature evaluation. */
    RETURN_AFTER_COMPLETION(RuleId.R15),
}

public data class MucusRestart(val cycleDay: Int, val category: MucusCategory, val reason: MucusRestartReason)

/** Result of the mucus evaluation of one cycle (R-12 to R-16). */
public sealed interface MucusEvaluation {
    public val restarts: List<MucusRestart>

    /** No f, S or S+ observed yet, so there is no peak candidate. */
    public data class NoCandidate(override val restarts: List<MucusRestart> = emptyList()) : MucusEvaluation

    /**
     * [candidateDay] is the last day of best quality so far. [countedDays] days with lower quality
     * followed it (0–2, or 3 for a manual peak whose count is still running).
     */
    public data class Counting(
        val candidateDay: Int,
        val category: MucusCategory?,
        val countedDays: Int,
        override val restarts: List<MucusRestart> = emptyList(),
        val manual: Boolean = false,
    ) : MucusEvaluation

    /** R-16: days without mucus entry inside the count 1–2–3. The peak cannot be determined. */
    public data class MissingEntries(
        val candidateDay: Int,
        val category: MucusCategory,
        val missingDays: List<Int>,
        override val restarts: List<MucusRestart> = emptyList(),
    ) : MucusEvaluation

    /** R-12, R-13: peak found, the mucus evaluation is complete on the evening of [completionDay]. */
    public data class Completed(
        val peakDay: Int,
        /** Null only for a manual peak on a day without mucus entry. */
        val category: MucusCategory?,
        val completionDay: Int,
        override val restarts: List<MucusRestart> = emptyList(),
        val manual: Boolean = false,
    ) : MucusEvaluation
}

/** Sensiplan cervical mucus evaluation. */
public object MucusEvaluator {
    /** R-12, R-13: number of days with lower quality that must follow the peak. */
    public const val COUNT_DAYS: Int = 3

    /**
     * @param categories mucus category per cycle day; days without entry are absent.
     * @param lastDay last cycle day to consider (today for the current cycle, otherwise the cycle length).
     * @param lastDayMayStillBeEntered true for the current cycle: a missing entry for today is not a gap yet.
     * @param temperatureCompletionDay day of the completed temperature evaluation, needed for R-15.
     * @param manualPeakDay F-42: peak day chosen by the user.
     */
    public fun evaluate(
        categories: Map<Int, MucusCategory>,
        lastDay: Int,
        lastDayMayStillBeEntered: Boolean,
        temperatureCompletionDay: Int?,
        manualPeakDay: Int? = null,
    ): MucusEvaluation {
        if (manualPeakDay != null) return evaluateManual(categories, manualPeakDay, lastDay)
        val search = PeakSearch(categories, temperatureCompletionDay)
        for (day in 1..lastDay) search.observe(day)
        return search.result(lastDay, lastDayMayStillBeEntered)
    }

    private fun evaluateManual(categories: Map<Int, MucusCategory>, peakDay: Int, lastDay: Int): MucusEvaluation {
        val completionDay = peakDay + COUNT_DAYS
        return if (completionDay <= lastDay) {
            MucusEvaluation.Completed(peakDay, categories[peakDay], completionDay, manual = true)
        } else {
            MucusEvaluation.Counting(
                candidateDay = peakDay,
                category = categories[peakDay],
                countedDays = (lastDay - peakDay).coerceAtLeast(0),
                manual = true,
            )
        }
    }

    /** Walks through the cycle day by day, the way the user would evaluate the paper chart. */
    private class PeakSearch(
        private val categories: Map<Int, MucusCategory>,
        private val temperatureCompletionDay: Int?,
    ) {
        private val restarts = mutableListOf<MucusRestart>()
        private var best: MucusCategory? = null
        private var candidate: Int? = null
        private var worseDaysSinceCandidate = 0
        private var completed: MucusEvaluation.Completed? = null

        fun observe(day: Int) {
            val category = categories[day]
            val peak = completed
            if (peak != null) {
                if (category != null && returnsAfterCompletion(day, category, peak)) {
                    restarts += MucusRestart(day, category, MucusRestartReason.RETURN_AFTER_COMPLETION)
                    completed = null
                    newCandidate(day, category)
                }
                return
            }
            if (category != null && isAtLeastBest(category)) {
                recordRestartDuringCount(day, category)
                newCandidate(day, category)
            } else if (candidate != null && category != null) {
                worseDaysSinceCandidate++
            }
            checkCompletion(day)
        }

        /** R-15: same or better quality after the mucus evaluation, before the temperature evaluation. */
        private fun returnsAfterCompletion(
            day: Int,
            category: MucusCategory,
            peak: MucusEvaluation.Completed,
        ): Boolean {
            val peakCategory = peak.category ?: return false
            val temperatureOpen = temperatureCompletionDay == null || day <= temperatureCompletionDay
            return category >= peakCategory && temperatureOpen
        }

        private fun isAtLeastBest(category: MucusCategory): Boolean {
            val currentBest = best
            return category.isFertileQuality && (currentBest == null || category >= currentBest)
        }

        /** R-14: the count 1–2–3 had started and mucus of (at least) the same quality returned. */
        private fun recordRestartDuringCount(day: Int, category: MucusCategory) {
            val previous = candidate ?: return
            if (worseDaysSinceCandidate == 0 || day > previous + COUNT_DAYS) return
            val reason = if (category == best) {
                MucusRestartReason.SAME_QUALITY_DURING_COUNT
            } else {
                MucusRestartReason.HIGHER_QUALITY_DURING_COUNT
            }
            restarts += MucusRestart(day, category, reason)
        }

        private fun newCandidate(day: Int, category: MucusCategory) {
            best = best?.let { maxOf(it, category) } ?: category
            candidate = day
            worseDaysSinceCandidate = 0
        }

        /** R-12, R-13: three days of lower quality, all of them recorded (R-16). */
        private fun checkCompletion(day: Int) {
            val current = candidate ?: return
            if (day != current + COUNT_DAYS) return
            val countDays = (current + 1)..(current + COUNT_DAYS)
            if (countDays.all { categories[it] != null }) {
                completed = MucusEvaluation.Completed(peakDay = current, category = best, completionDay = day)
            }
        }

        fun result(lastDay: Int, lastDayMayStillBeEntered: Boolean): MucusEvaluation {
            completed?.let { return it.copy(restarts = restarts.toList()) }
            val finalCandidate = candidate ?: return MucusEvaluation.NoCandidate(restarts.toList())
            val finalBest = requireNotNull(best)
            val window = (finalCandidate + 1)..minOf(lastDay, finalCandidate + COUNT_DAYS)
            val missing = window.filter { day ->
                categories[day] == null && !(day == lastDay && lastDayMayStillBeEntered)
            }
            return if (missing.isNotEmpty()) {
                MucusEvaluation.MissingEntries(finalCandidate, finalBest, missing, restarts.toList())
            } else {
                MucusEvaluation.Counting(
                    candidateDay = finalCandidate,
                    category = finalBest,
                    countedDays = window.count { categories[it] != null },
                    restarts = restarts.toList(),
                )
            }
        }
    }
}
