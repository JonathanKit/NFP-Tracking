package de.easynfp.rules

import de.easynfp.rules.TestCycles.categories
import de.easynfp.rules.model.MucusCategory
import de.easynfp.rules.startphase.PriorCycleSummary
import de.easynfp.rules.startphase.StartPhaseEvaluator
import de.easynfp.rules.startphase.StartPhaseRule
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class StartPhaseRulesTest {

    private fun evaluated(firstHigherDay: Int) = PriorCycleSummary(true, firstHigherDay)
    private val notEvaluated = PriorCycleSummary(false, null)

    private fun evaluate(prior: List<PriorCycleSummary>, mucus: String = "t t t t t t t t t t") =
        StartPhaseEvaluator.evaluate(prior, categories(mucus))

    @Test
    fun `R-30 first recorded cycle has no infertile start phase`() {
        val result = evaluate(emptyList())
        assertEquals(StartPhaseRule.NO_PREVIOUS_CYCLE, result.rule)
        assertNull(result.lastInfertileDay)
    }

    @Test
    fun `R-30 previous cycle without evaluated temperature means fertile from day 1`() {
        val result = evaluate(listOf(evaluated(15), notEvaluated))
        assertEquals(StartPhaseRule.PREVIOUS_CYCLE_NOT_EVALUATED, result.rule)
        assertNull(result.lastInfertileDay)
    }

    @Test
    fun `R-31 five day rule with fewer than 12 evaluated curves`() {
        val result = evaluate(listOf(evaluated(16), evaluated(15)))
        assertEquals(StartPhaseRule.FIVE_DAY_RULE, result.rule)
        assertEquals(5, result.lastInfertileDay)
        assertEquals(2, result.evaluatedCurveCount)
    }

    @Test
    fun `R-31 an earlier unevaluated cycle does not matter if the previous one is evaluated`() {
        val result = evaluate(listOf(notEvaluated, evaluated(15)))
        assertEquals(StartPhaseRule.FIVE_DAY_RULE, result.rule)
        assertEquals(1, result.evaluatedCurveCount)
    }

    @Test
    fun `R-32 early first higher measurement switches to minus 8 during the first 12 cycles`() {
        val result = evaluate(listOf(evaluated(15), evaluated(12), evaluated(16)))
        assertEquals(StartPhaseRule.MINUS_EIGHT_EARLY, result.rule)
        assertEquals(12, result.earliestFirstHigherDay)
        assertEquals(4, result.lastInfertileDay)
    }

    @Test
    fun `R-32 first higher measurement on day 13 keeps the five day rule`() {
        val result = evaluate(listOf(evaluated(13), evaluated(16)))
        assertEquals(StartPhaseRule.FIVE_DAY_RULE, result.rule)
    }

    @Test
    fun `R-32 minus 8 can leave no infertile days`() {
        val result = evaluate(listOf(evaluated(8)))
        assertEquals(StartPhaseRule.MINUS_EIGHT_EARLY, result.rule)
        assertNull(result.lastInfertileDay)
    }

    @Test
    fun `R-33 minus 8 rule from 12 evaluated curves on, may extend the phase`() {
        val prior = (1..12).map { evaluated(if (it == 4) 15 else 17) }
        val result = evaluate(prior)
        assertEquals(StartPhaseRule.MINUS_EIGHT, result.rule)
        assertEquals(15, result.earliestFirstHigherDay)
        assertEquals(7, result.lastInfertileDay)
    }

    @Test
    fun `R-33 minus 8 rule may shorten the phase`() {
        val prior = (1..14).map { evaluated(if (it == 13) 11 else 16) }
        val result = evaluate(prior)
        assertEquals(StartPhaseRule.MINUS_EIGHT, result.rule)
        assertEquals(3, result.lastInfertileDay)
    }

    @Test
    fun `R-34 mucus f, S or S+ ends the start phase immediately`() {
        val result = evaluate(listOf(evaluated(16)), mucus = "- - t f t")
        assertEquals(5, result.lastInfertileDayByRule)
        assertEquals(4, result.endedByMucusOnDay)
        assertEquals(MucusCategory.MOIST, result.endedByMucusCategory)
        assertEquals(3, result.lastInfertileDay)
    }

    @Test
    fun `R-34 mucus on day 1 leaves no infertile day`() {
        val result = evaluate(listOf(evaluated(16)), mucus = "S")
        assertNull(result.lastInfertileDay)
    }

    @Test
    fun `R-34 t and ø do not end the start phase`() {
        val result = evaluate(listOf(evaluated(16)), mucus = "t ø ø t ø")
        assertNull(result.endedByMucusOnDay)
        assertEquals(5, result.lastInfertileDay)
    }

    @Test
    fun `R-34 mucus after the start phase is irrelevant for it`() {
        val result = evaluate(listOf(evaluated(16)), mucus = "t t t t t f")
        assertNull(result.endedByMucusOnDay)
        assertEquals(5, result.lastInfertileDay)
    }
}
