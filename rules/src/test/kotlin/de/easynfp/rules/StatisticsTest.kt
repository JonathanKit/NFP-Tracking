package de.easynfp.rules

import de.easynfp.rules.TestCycles.START
import de.easynfp.rules.TestCycles.TYPICAL_BLEEDING
import de.easynfp.rules.TestCycles.TYPICAL_MUCUS
import de.easynfp.rules.TestCycles.TYPICAL_TEMPS
import de.easynfp.rules.model.Bleeding
import de.easynfp.rules.model.CycleBuilder
import de.easynfp.rules.model.CycleStart
import de.easynfp.rules.statistics.CycleStatistics
import de.easynfp.rules.statistics.NumberSummary
import de.easynfp.rules.statistics.PeriodForecaster
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class StatisticsTest {

    private fun typicalCycles(count: Int, currentDay: Int = 10): List<CycleEvaluation> {
        val starts = (0 until count).map { CycleStart(START.plusDays(28L * it)) }
        val observations = starts.flatMap {
            TestCycles.observations(TYPICAL_TEMPS, TYPICAL_MUCUS, TYPICAL_BLEEDING, it.startDate)
        }
        val today = starts.last().startDate.plusDays(currentDay - 1L)
        return SensiplanEngine.evaluate(CycleBuilder.build(starts, observations), today)
    }

    @Test
    fun `number summary`() {
        val summary = requireNotNull(NumberSummary.of(listOf(26, 28, 30)))
        assertEquals(28.0, summary.mean, 0.0001)
        assertEquals(26, summary.min)
        assertEquals(30, summary.max)
        assertEquals(1.633, summary.standardDeviation, 0.001)
        assertNull(NumberSummary.of(emptyList()))
    }

    @Test
    fun `F-43 statistics ignore the current cycle`() {
        val summary = CycleStatistics.summarize(typicalCycles(4))
        assertEquals(3, summary.cycleCount)
        assertEquals(28.0, summary.cycleLength?.mean)
        assertEquals(15, summary.firstHigherDay?.min)
        assertEquals(14, summary.highPhaseLength?.max)
        // First cycle: no start phase (R-30), fertile days 1-18. Later cycles: fertile days 6-18.
        assertEquals(18, summary.fertilePhaseLength?.max)
        assertEquals(13, summary.fertilePhaseLength?.min)
        assertEquals(0, summary.infertileStartDays?.min)
        assertEquals(5, summary.infertileStartDays?.max)
        assertEquals(10, summary.infertileEndDays?.mean?.toInt())
    }

    @Test
    fun `F-45 forecast needs three cycles and gives a range`() {
        assertNull(PeriodForecaster.forecast(listOf(28, 29), START))
        val forecast = assertNotNull(PeriodForecaster.forecast(listOf(40, 26, 28, 29, 27, 30, 28), START))
        assertEquals(6, forecast.basedOnCycles)
        assertEquals(26, forecast.shortestCycle)
        assertEquals(30, forecast.longestCycle)
        assertEquals(START.plusDays(26), forecast.earliest)
        assertEquals(START.plusDays(30), forecast.latest)
    }

    @Test
    fun `F-08 new cycle is suggested on the first bleeding day after the high phase`() {
        val current = typicalCycles(2, currentDay = 28).last()
        val day = { n: Int -> current.cycle.dateOf(n) }
        assertTrue(NewCycleSuggestion.shouldSuggest(current, day(28), Bleeding.MEDIUM))
        assertFalse(NewCycleSuggestion.shouldSuggest(current, day(28), Bleeding.SPOTTING))
        assertFalse(NewCycleSuggestion.shouldSuggest(current, day(28), null))
        // Before the temperature evaluation is complete.
        assertFalse(NewCycleSuggestion.shouldSuggest(current, day(10), Bleeding.HEAVY))
        // On the first day of the cycle itself.
        assertFalse(NewCycleSuggestion.shouldSuggest(current, day(1), Bleeding.HEAVY))
        // Without any recorded cycle.
        assertTrue(NewCycleSuggestion.shouldSuggest(null, START, Bleeding.LIGHT))
    }
}
