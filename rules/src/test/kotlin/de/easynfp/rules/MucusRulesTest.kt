package de.easynfp.rules

import de.easynfp.rules.TestCycles.categories
import de.easynfp.rules.TestCycles.tokens
import de.easynfp.rules.model.MucusAppearance
import de.easynfp.rules.model.MucusCategory
import de.easynfp.rules.model.MucusSensation
import de.easynfp.rules.mucus.MucusEvaluation
import de.easynfp.rules.mucus.MucusEvaluator
import de.easynfp.rules.mucus.MucusRestartReason
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class MucusRulesTest {

    private fun evaluate(
        mucus: String,
        ongoing: Boolean = false,
        lastDay: Int = tokens(mucus).size,
        temperatureDay: Int? = null,
        manualPeak: Int? = null,
    ) = MucusEvaluator.evaluate(categories(mucus), lastDay, ongoing, temperatureDay, manualPeak)

    @Test
    fun `R-10 categories are ordered t, ø, f, S, S+`() {
        val ordered = listOf(
            MucusCategory.DRY,
            MucusCategory.NOTHING,
            MucusCategory.MOIST,
            MucusCategory.S,
            MucusCategory.S_PLUS,
        )
        assertEquals(ordered, MucusCategory.entries.sorted())
        assertEquals(listOf("t", "ø", "f", "S", "S+"), ordered.map { it.symbol })
    }

    @Test
    fun `R-11 derives the category from sensation and appearance`() {
        val seen = MucusAppearance.NOTHING_SEEN
        assertEquals(MucusCategory.DRY, MucusCategory.derive(MucusSensation.DRY, seen))
        assertEquals(MucusCategory.NOTHING, MucusCategory.derive(MucusSensation.NOTHING_FELT, seen))
        assertEquals(MucusCategory.MOIST, MucusCategory.derive(MucusSensation.MOIST, seen))
        assertEquals(MucusCategory.S, MucusCategory.derive(MucusSensation.MOIST, MucusAppearance.THICK))
        assertEquals(MucusCategory.S, MucusCategory.derive(MucusSensation.NOTHING_FELT, MucusAppearance.THICK))
        assertEquals(MucusCategory.S_PLUS, MucusCategory.derive(MucusSensation.WET_SLIPPERY, seen))
        assertEquals(MucusCategory.S_PLUS, MucusCategory.derive(MucusSensation.WET_SLIPPERY, MucusAppearance.THICK))
        assertEquals(MucusCategory.S_PLUS, MucusCategory.derive(MucusSensation.DRY, MucusAppearance.CLEAR_STRETCHY))
        assertEquals(MucusCategory.S_PLUS, MucusCategory.derive(MucusSensation.MOIST, MucusAppearance.CLEAR_STRETCHY))
        // The better observation counts: dry sensation with thick appearance is S.
        assertEquals(MucusCategory.S, MucusCategory.derive(MucusSensation.DRY, MucusAppearance.THICK))
        // A missing appearance means nothing was seen.
        assertEquals(MucusCategory.MOIST, MucusCategory.derive(MucusSensation.MOIST, null))
        assertEquals(MucusCategory.S, MucusCategory.derive(null, MucusAppearance.THICK))
        assertNull(MucusCategory.derive(null, seen))
        assertNull(MucusCategory.derive(null, null))
    }

    @Test
    fun `R-11 finer observation steps map to the same categories`() {
        val seen = MucusAppearance.NOTHING_SEEN
        assertEquals(MucusCategory.S_PLUS, MucusCategory.derive(MucusSensation.WET, seen))
        assertEquals(MucusCategory.S_PLUS, MucusCategory.derive(MucusSensation.WET, null))
        listOf(MucusAppearance.WHITISH_CLOUDY, MucusAppearance.THICK, MucusAppearance.LUMPY_YELLOWISH).forEach {
            assertEquals(MucusCategory.S, MucusCategory.derive(MucusSensation.DRY, it), "$it")
            assertEquals(MucusCategory.S, MucusCategory.derive(null, it), "$it")
            assertEquals(MucusCategory.S_PLUS, MucusCategory.derive(MucusSensation.WET, it), "$it")
        }
        listOf(MucusAppearance.GLASSY, MucusAppearance.CLEAR_STRETCHY, MucusAppearance.LIQUID).forEach {
            assertEquals(MucusCategory.S_PLUS, MucusCategory.derive(MucusSensation.DRY, it), "$it")
            assertEquals(MucusCategory.S_PLUS, MucusCategory.derive(null, it), "$it")
        }
        // Both scales are ordered from worst to best quality.
        val bySensation = MucusSensation.entries.map { MucusCategory.derive(it, seen)!! }
        assertEquals(bySensation.sorted(), bySensation)
        val byAppearance = MucusAppearance.entries.drop(1).map { MucusCategory.derive(null, it)!! }
        assertEquals(byAppearance.sorted(), byAppearance)
    }

    @Test
    fun `R-12 peak is the last day of best quality followed by three lower days`() {
        val result = assertIs<MucusEvaluation.Completed>(evaluate("t t f S S+ S+ f t t"))
        assertEquals(6, result.peakDay)
        assertEquals(MucusCategory.S_PLUS, result.category)
        assertTrue(result.restarts.isEmpty())
    }

    @Test
    fun `R-12 the individually best quality may be S`() {
        val result = assertIs<MucusEvaluation.Completed>(evaluate("t f S S f t t"))
        assertEquals(4, result.peakDay)
        assertEquals(MucusCategory.S, result.category)
    }

    @Test
    fun `R-12 no peak without f, S or S+`() {
        assertIs<MucusEvaluation.NoCandidate>(evaluate("t t ø ø t t t"))
    }

    @Test
    fun `R-13 evaluation is complete on the third day after the peak`() {
        val result = assertIs<MucusEvaluation.Completed>(evaluate("t S+ f t t t t"))
        assertEquals(2, result.peakDay)
        assertEquals(5, result.completionDay)
    }

    @Test
    fun `R-13 counting is in progress before the third day`() {
        val counting = assertIs<MucusEvaluation.Counting>(evaluate("t S+ f t", ongoing = true))
        assertEquals(2, counting.candidateDay)
        assertEquals(2, counting.countedDays)
    }

    @Test
    fun `R-14 same category during the count restarts the evaluation`() {
        val result = assertIs<MucusEvaluation.Completed>(evaluate("S+ f S+ t t t"))
        assertEquals(3, result.peakDay)
        assertEquals(6, result.completionDay)
        val restart = result.restarts.single()
        assertEquals(3, restart.cycleDay)
        assertEquals(MucusRestartReason.SAME_QUALITY_DURING_COUNT, restart.reason)
    }

    @Test
    fun `R-14 consecutive days of best quality are no restart`() {
        val result = assertIs<MucusEvaluation.Completed>(evaluate("S+ S+ t t t"))
        assertEquals(2, result.peakDay)
        assertTrue(result.restarts.isEmpty())
    }

    @Test
    fun `R-12 better quality during the count moves the peak`() {
        val result = assertIs<MucusEvaluation.Completed>(evaluate("S f S+ t t t"))
        assertEquals(3, result.peakDay)
        assertEquals(MucusRestartReason.HIGHER_QUALITY_DURING_COUNT, result.restarts.single().reason)
    }

    @Test
    fun `R-14 lower quality during the count does not restart`() {
        val result = assertIs<MucusEvaluation.Completed>(evaluate("S+ S f t"))
        assertEquals(1, result.peakDay)
        assertEquals(4, result.completionDay)
    }

    @Test
    fun `R-15 mucus returns after the mucus evaluation but before the temperature evaluation`() {
        val result = assertIs<MucusEvaluation.Completed>(evaluate("f S f t t t S+ t t t", temperatureDay = 8))
        assertEquals(7, result.peakDay)
        assertEquals(10, result.completionDay)
        val restart = result.restarts.single()
        assertEquals(MucusRestartReason.RETURN_AFTER_COMPLETION, restart.reason)
        assertEquals(7, restart.cycleDay)
    }

    @Test
    fun `R-15 mucus returns while the temperature evaluation is still open`() {
        val result = assertIs<MucusEvaluation.Counting>(evaluate("S t t t S", ongoing = true, temperatureDay = null))
        assertEquals(5, result.candidateDay)
        assertEquals(MucusRestartReason.RETURN_AFTER_COMPLETION, result.restarts.single().reason)
    }

    @Test
    fun `R-15 nothing changes once both evaluations are complete`() {
        val result = assertIs<MucusEvaluation.Completed>(evaluate("f S f t t t S+ t t t", temperatureDay = 6))
        assertEquals(2, result.peakDay)
        assertEquals(5, result.completionDay)
        assertTrue(result.restarts.isEmpty())
    }

    @Test
    fun `R-15 lower quality after the mucus evaluation does not restart`() {
        val result = assertIs<MucusEvaluation.Completed>(evaluate("S+ t t t S f", temperatureDay = null))
        assertEquals(1, result.peakDay)
    }

    @Test
    fun `R-16 missing entry inside the count prevents the evaluation`() {
        val result = assertIs<MucusEvaluation.MissingEntries>(evaluate("t S+ t - t t t"))
        assertEquals(2, result.candidateDay)
        assertEquals(listOf(4), result.missingDays)
    }

    @Test
    fun `R-16 missing entry is reported as soon as it is in the past`() {
        val result = assertIs<MucusEvaluation.MissingEntries>(evaluate("t S+ - t", ongoing = true))
        assertEquals(listOf(3), result.missingDays)
    }

    @Test
    fun `R-16 a later peak can still be evaluated`() {
        val result = assertIs<MucusEvaluation.Completed>(evaluate("S+ t - t t S+ t t t"))
        assertEquals(6, result.peakDay)
    }

    @Test
    fun `today without entry is not a gap in the current cycle`() {
        val counting = assertIs<MucusEvaluation.Counting>(evaluate("S+ t t -", ongoing = true))
        assertEquals(2, counting.countedDays)
        // In a completed cycle the same day is missing.
        assertIs<MucusEvaluation.MissingEntries>(evaluate("S+ t t -", ongoing = false))
    }

    @Test
    fun `F-42 manual peak`() {
        val completed = assertIs<MucusEvaluation.Completed>(evaluate("t f S S S t t t", manualPeak = 3))
        assertEquals(3, completed.peakDay)
        assertEquals(6, completed.completionDay)
        assertTrue(completed.manual)

        val counting = assertIs<MucusEvaluation.Counting>(evaluate("t f S S", ongoing = true, manualPeak = 3))
        assertEquals(1, counting.countedDays)
        assertTrue(counting.manual)
    }
}
