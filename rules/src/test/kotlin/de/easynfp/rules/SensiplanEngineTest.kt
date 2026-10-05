package de.easynfp.rules

import de.easynfp.rules.TestCycles.START
import de.easynfp.rules.TestCycles.TYPICAL_BLEEDING
import de.easynfp.rules.TestCycles.TYPICAL_MUCUS
import de.easynfp.rules.TestCycles.TYPICAL_TEMPS
import de.easynfp.rules.TestCycles.cycle
import de.easynfp.rules.model.CycleBuilder
import de.easynfp.rules.model.CycleOverrides
import de.easynfp.rules.model.CycleStart
import de.easynfp.rules.model.DayObservation
import de.easynfp.rules.startphase.PriorCycleSummary
import de.easynfp.rules.startphase.StartPhaseRule
import de.easynfp.rules.temperature.TemperatureEvaluation
import de.easynfp.rules.temperature.TemperatureRule
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class SensiplanEngineTest {

    private val evaluatedBefore = listOf(PriorCycleSummary(true, 16))
    private val farFuture: LocalDate = START.plusYears(1)

    private fun evaluate(
        temps: String,
        mucus: String,
        length: Int? = null,
        prior: List<PriorCycleSummary> = evaluatedBefore,
        today: LocalDate = farFuture,
        overrides: CycleOverrides = CycleOverrides(),
    ): CycleEvaluation = SensiplanEngine.evaluateCycle(
        cycle(temps = temps, mucus = mucus, length = length, overrides = overrides),
        today,
        prior,
    )

    @Test
    fun `typical cycle reproduces the example of F-41`() {
        val result = evaluate(TYPICAL_TEMPS, TYPICAL_MUCUS, length = 28)
        val temperature = assertIs<TemperatureEvaluation.Completed>(result.temperature)
        assertEquals(15, temperature.shift.firstHigherDay)
        assertEquals(17, temperature.completionDay)
        assertEquals(TemperatureRule.REGULAR, temperature.rule)
        assertEquals(15, result.mucusCompleted?.peakDay)
        assertEquals(18, result.infertileFromEveningOfDay)

        val phases = result.dayPhases
        assertEquals(28, phases.size)
        assertTrue(phases.take(5).all { it.phase == Phase.INFERTILE_START })
        assertTrue(phases.subList(5, 18).all { it.phase == Phase.FERTILE })
        assertTrue(phases[17].infertileFromEvening)
        assertTrue(phases.drop(18).all { it.phase == Phase.INFERTILE_END })

        val final = result.explanation.last()
        val expected = ExplanationStep.InfertileAfterOvulation(18, decidedByTemperature = false, decidedByMucus = true)
        assertEquals(expected, final)
        // The discarded candidate on day 12 (both following values fell) is part of the explanation.
        assertTrue(result.explanation.any { it is ExplanationStep.TemperatureCandidateDiscarded })
    }

    @Test
    fun `R-20 temperature evaluated later than mucus decides`() {
        val result = evaluate(TYPICAL_TEMPS, "- - - - - t t f S S+ f t t t t t t t t t", length = 28)
        assertEquals(13, result.mucusCompleted?.completionDay)
        assertEquals(17, result.temperatureCompleted?.completionDay)
        assertEquals(17, result.infertileFromEveningOfDay)
        val step = result.explanation.filterIsInstance<ExplanationStep.InfertileAfterOvulation>().single()
        assertTrue(step.decidedByTemperature)
        assertFalse(step.decidedByMucus)
    }

    @Test
    fun `R-21 the infertile time lasts until the end of the cycle`() {
        val result = evaluate(TYPICAL_TEMPS, TYPICAL_MUCUS, length = 28)
        assertEquals(Phase.INFERTILE_END, result.phaseOf(28).phase)
    }

    @Test
    fun `R-22 without mucus evaluation the cycle stays fertile`() {
        val mucusWithGap = "- - - - - t t ø f S S S+ S+ S+ S+ f - t t t t t t t t t t t"
        val result = evaluate(TYPICAL_TEMPS, mucusWithGap, length = 28)
        assertNull(result.infertileFromEveningOfDay)
        assertTrue(result.dayPhases.drop(5).all { it.phase == Phase.FERTILE })
        assertEquals(
            ExplanationStep.FertileUntilBothEvaluated(temperatureDone = true, mucusDone = false),
            result.explanation.last(),
        )
        assertTrue(result.explanation.any { it is ExplanationStep.MucusMissingEntries })
    }

    @Test
    fun `R-22 without temperature evaluation the cycle stays fertile`() {
        val result = evaluate("36.40 36.45 36.40", TYPICAL_MUCUS, length = 28)
        assertNull(result.infertileFromEveningOfDay)
        assertEquals(Phase.FERTILE, result.phaseOf(28).phase)
    }

    @Test
    fun `current cycle is evaluated up to today only`() {
        val today = START.plusDays(15) // cycle day 16
        val result = evaluate(TYPICAL_TEMPS, TYPICAL_MUCUS, today = today)
        assertEquals(16, result.dayCount)
        val pending = assertIs<TemperatureEvaluation.Pending>(result.temperature)
        assertEquals(15, pending.shift.firstHigherDay)
        assertEquals(Phase.FERTILE, result.phaseOn(today)?.phase)
        assertNull(result.phaseOn(today.plusDays(1)))
    }

    @Test
    fun `R-34 start phase ends with mucus inside the engine`() {
        val result = evaluate(TYPICAL_TEMPS, "- - - f", length = 28)
        assertEquals(3, result.startPhase.lastInfertileDay)
        assertEquals(Phase.FERTILE, result.phaseOf(4).phase)
        assertTrue(result.explanation.any { it is ExplanationStep.StartPhaseEndedByMucus })
    }

    @Test
    fun `F-42 overrides are applied`() {
        val overrides = CycleOverrides(mucusPeak = START.plusDays(12)) // day 13
        val result = evaluate(TYPICAL_TEMPS, TYPICAL_MUCUS, length = 28, overrides = overrides)
        val mucus = assertIs<de.easynfp.rules.mucus.MucusEvaluation.Completed>(result.mucus)
        assertTrue(mucus.manual)
        assertEquals(16, mucus.completionDay)
        assertEquals(17, result.infertileFromEveningOfDay)
        assertTrue(result.explanation.any { it.ruleId == RuleId.MANUAL })
    }

    @Test
    fun `cycles are chained, the start phase depends on earlier cycles`() {
        val starts = (0 until 3).map { CycleStart(START.plusDays(28L * it)) }
        val observations = starts.flatMap { start ->
            TestCycles.observations(TYPICAL_TEMPS, TYPICAL_MUCUS, TYPICAL_BLEEDING, start.startDate)
        }
        val cycles = CycleBuilder.build(starts, observations)
        val today = START.plusDays(28L * 2 + 9)
        val results = SensiplanEngine.evaluate(cycles, today)

        assertEquals(StartPhaseRule.NO_PREVIOUS_CYCLE, results[0].startPhase.rule)
        assertEquals(StartPhaseRule.FIVE_DAY_RULE, results[1].startPhase.rule)
        assertEquals(StartPhaseRule.FIVE_DAY_RULE, results[2].startPhase.rule)
        assertEquals(2, results[2].startPhase.evaluatedCurveCount)
        assertEquals(10, results[2].dayCount)
        assertEquals(28, results[0].cycle.length)
        assertNull(results[2].cycle.length)
    }

    @Test
    fun `early first higher measurement in an earlier cycle switches later cycles to minus 8`() {
        // First higher measurement on day 12: six low values from day 6 on.
        val early = "36.40 36.40 36.40 36.40 36.40 36.40 36.45 36.40 36.50 36.45 36.40 36.55 36.65 36.75 36.80"
        val starts = listOf(CycleStart(START), CycleStart(START.plusDays(26)), CycleStart(START.plusDays(54)))
        val observations = TestCycles.observations(early, "", "", START) +
            TestCycles.observations(TYPICAL_TEMPS, TYPICAL_MUCUS, "", START.plusDays(26))
        val results = SensiplanEngine.evaluate(CycleBuilder.build(starts, observations), START.plusDays(60))
        assertEquals(12, results[0].temperatureCompleted?.shift?.firstHigherDay)
        assertEquals(StartPhaseRule.MINUS_EIGHT_EARLY, results[1].startPhase.rule)
        assertEquals(4, results[1].startPhase.lastInfertileDay)
        assertEquals(StartPhaseRule.MINUS_EIGHT_EARLY, results[2].startPhase.rule)
    }

    @Test
    fun `excluded temperature is ignored by the engine`() {
        val obs = listOf(DayObservation(START, TestCycles.t("37.50"), temperatureExcluded = true))
        val cycleData = de.easynfp.rules.model.CycleData(START, null, obs)
        val result = SensiplanEngine.evaluateCycle(cycleData, START, emptyList())
        assertEquals(0, assertIs<TemperatureEvaluation.Searching>(result.temperature).validValueCount)
    }

    @Test
    fun `cycle builder splits observations by period start`() {
        val starts = listOf(CycleStart(START.plusDays(10)), CycleStart(START))
        val obs = (0 until 15).map { DayObservation(START.plusDays(it.toLong())) }
        val cycles = CycleBuilder.build(starts, obs)
        assertEquals(2, cycles.size)
        assertEquals(START, cycles[0].startDate)
        assertEquals(START.plusDays(9), cycles[0].endDate)
        assertEquals(10, cycles[0].observations.size)
        assertEquals(5, cycles[1].observations.size)
        assertEquals(10, cycles[0].length)
    }
}
