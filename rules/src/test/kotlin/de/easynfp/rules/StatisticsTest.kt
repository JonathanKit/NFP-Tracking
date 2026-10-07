package de.easynfp.rules

import de.easynfp.rules.TestCycles.START
import de.easynfp.rules.TestCycles.TYPICAL_BLEEDING
import de.easynfp.rules.TestCycles.TYPICAL_MUCUS
import de.easynfp.rules.TestCycles.TYPICAL_TEMPS
import de.easynfp.rules.model.Bleeding
import de.easynfp.rules.model.CycleBuilder
import de.easynfp.rules.model.CycleStart
import de.easynfp.rules.statistics.CycleForecaster
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
    fun `F-47 forecast continues the current cycle and the following ones`() {
        val evaluations = typicalCycles(4, currentDay = 10)
        val current = evaluations.last()
        val today = current.cycle.dateOf(10)
        val forecast = assertNotNull(CycleForecaster.forecast(evaluations, today))
        assertEquals(28, forecast.cycleLength)
        assertEquals(5, forecast.periodDays)
        assertEquals(3, forecast.basedOnCycles)
        assertEquals(current.cycle.dateOf(29), forecast.nextPeriodStart)
        assertEquals(28, forecast.currentCycleLength(current.cycle.startDate))
        // Today and the past are never forecast.
        assertNull(forecast.dayOn(today))
        assertNull(forecast.dayOn(current.cycle.dateOf(3)))
        // Current cycle: fertile until the evening of day 18 as in the past cycles.
        assertEquals(Phase.FERTILE, forecast.dayOn(current.cycle.dateOf(11))?.phase)
        assertTrue(forecast.dayOn(current.cycle.dateOf(18))?.infertileFromEvening == true)
        assertEquals(Phase.INFERTILE_END, forecast.dayOn(current.cycle.dateOf(19))?.phase)
        // Next cycle: period, five-day rule, fertile time, infertile time.
        val next = { day: Int -> assertNotNull(forecast.dayOn(forecast.nextPeriodStart.plusDays(day - 1L))) }
        assertTrue(next(1).expectedPeriod)
        assertEquals(1, next(1).cycleDay)
        assertEquals(Phase.INFERTILE_START, next(5).phase)
        assertTrue(next(5).expectedPeriod)
        assertFalse(next(6).expectedPeriod)
        assertEquals(Phase.FERTILE, next(6).phase)
        assertEquals(Phase.INFERTILE_END, next(19).phase)
        assertEquals(forecast.nextPeriodStart.plusDays(28), next(28).date.plusDays(1))
        assertNull(forecast.dayOn(today.plusDays(CycleForecaster.DEFAULT_HORIZON_DAYS + 1L)))
    }

    @Test
    fun `F-47 forecast stays fertile when the usual evaluation day passed without evaluation`() {
        val evaluations = typicalCycles(3, currentDay = 20)
        val current = evaluations.last()
        // Remove the higher measurements of the current cycle: it is not evaluated on day 20.
        val unevaluated = SensiplanEngine.evaluate(
            CycleBuilder.build(
                evaluations.map { CycleStart(it.cycle.startDate) },
                evaluations.flatMap { evaluation ->
                    evaluation.cycle.observations.filterNot {
                        evaluation === current && it.date > current.cycle.dateOf(14)
                    }
                },
            ),
            current.cycle.dateOf(20),
        )
        val forecast = assertNotNull(CycleForecaster.forecast(unevaluated, current.cycle.dateOf(20)))
        assertEquals(Phase.FERTILE, forecast.dayOn(current.cycle.dateOf(25))?.phase)
    }

    @Test
    fun `F-47 no forecast without a completed cycle`() {
        val evaluations = typicalCycles(1, currentDay = 10)
        assertNull(CycleForecaster.forecast(evaluations, evaluations.last().cycle.dateOf(10)))
        // Cycles excluded from statistics (F-46) are not used.
        val more = typicalCycles(3, currentDay = 10)
        assertNull(CycleForecaster.forecast(more, more.last().cycle.dateOf(10), countsForStatistics = { false }))
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
