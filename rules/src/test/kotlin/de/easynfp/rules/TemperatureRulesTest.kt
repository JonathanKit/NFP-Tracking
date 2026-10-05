package de.easynfp.rules

import de.easynfp.rules.TestCycles.t
import de.easynfp.rules.TestCycles.values
import de.easynfp.rules.model.DayObservation
import de.easynfp.rules.model.Temperature
import de.easynfp.rules.temperature.DiscardReason
import de.easynfp.rules.temperature.NotEvaluableReason
import de.easynfp.rules.temperature.PendingStep
import de.easynfp.rules.temperature.TemperatureEvaluation
import de.easynfp.rules.temperature.TemperatureEvaluator
import de.easynfp.rules.temperature.TemperatureRule
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class TemperatureRulesTest {

    private fun evaluate(temps: String, completed: Boolean = false, manualDay: Int? = null) =
        TemperatureEvaluator.evaluate(values(temps), completed, manualDay)

    // Six low values at 36.40/36.45/36.50, coverline 36.50.
    private val low = "36.40 36.45 36.40 36.50 36.45 36.40"

    @Test
    fun `R-01 rounds to five hundredths`() {
        assertEquals(t("36.20"), t("36.21").roundedForEvaluation())
        assertEquals(t("36.20"), t("36.22").roundedForEvaluation())
        assertEquals(t("36.25"), t("36.23").roundedForEvaluation())
        assertEquals(t("36.25"), t("36.24").roundedForEvaluation())
        assertEquals(t("36.25"), t("36.25").roundedForEvaluation())
        assertEquals(t("36.25"), t("36.26").roundedForEvaluation())
        assertEquals(t("36.25"), t("36.27").roundedForEvaluation())
        assertEquals(t("36.30"), t("36.28").roundedForEvaluation())
        assertEquals(t("36.30"), t("36.29").roundedForEvaluation())
        assertEquals(t("37.00"), t("36.98").roundedForEvaluation())
    }

    @Test
    fun `R-01 keeps the original value and rounds only for evaluation`() {
        val obs = DayObservation(TestCycles.START, temperature = t("36.53"))
        assertEquals(t("36.53"), obs.temperature)
        assertEquals(t("36.55"), obs.evaluationTemperature)
    }

    @Test
    fun `R-01 rounding decides about higher measurements`() {
        // 36.52 rounds to 36.50 which is not higher than the coverline 36.50; 36.53 rounds to 36.55.
        assertIs<TemperatureEvaluation.Searching>(evaluate("$low 36.52"))
        assertIs<TemperatureEvaluation.Pending>(evaluate("$low 36.53"))
    }

    @Test
    fun `temperature parsing and range`() {
        assertEquals(Temperature(3655), Temperature.parse("36,55"))
        assertEquals(Temperature(3650), Temperature.parse("36.5"))
        assertEquals(Temperature(3700), Temperature.parse("37"))
        assertNull(Temperature.parse("abc"))
        assertNull(Temperature.parse("36.555"))
        assertTrue(t("34.00").isInPlausibleRange)
        assertTrue(t("42.00").isInPlausibleRange)
        assertTrue(!t("33.99").isInPlausibleRange)
        assertTrue(!t("42.01").isInPlausibleRange)
        assertEquals("36.05", t("36.05").toString())
    }

    @Test
    fun `R-02 excluded values are skipped`() {
        // Day 7 is excluded; the six reference values are days 1-6, first higher measurement day 8.
        val result = evaluate("$low (37.20) 36.60 36.65 36.75")
        val completed = assertIs<TemperatureEvaluation.Completed>(result)
        assertEquals(8, completed.shift.firstHigherDay)
        assertEquals(listOf(1, 2, 3, 4, 5, 6), completed.shift.referenceDays)
        assertEquals(10, completed.completionDay)
    }

    @Test
    fun `R-02 days without measurement are skipped`() {
        val result = evaluate("$low - 36.60 - 36.65 36.75")
        val completed = assertIs<TemperatureEvaluation.Completed>(result)
        assertEquals(8, completed.shift.firstHigherDay)
        assertEquals(listOf(8, 10, 11), completed.shift.measurements.map { it.cycleDay })
        assertEquals(11, completed.completionDay)
    }

    @Test
    fun `R-02 an excluded high value does not become a reference value`() {
        // Without exclusion day 3 (36.90) would be in the reference values and raise the coverline.
        val result = evaluate("36.40 36.45 (36.90) 36.40 36.50 36.45 36.40 36.40 36.60 36.65 36.75")
        val completed = assertIs<TemperatureEvaluation.Completed>(result)
        assertEquals(t("36.50"), completed.shift.coverline)
        assertEquals(9, completed.shift.firstHigherDay)
    }

    @Test
    fun `R-03 first higher measurement needs six preceding valid values`() {
        val result = evaluate("36.40 36.45 36.40 36.50 36.45 36.80 36.85 36.90")
        // Day 6 has only five predecessors; day 7 compares days 1-6 (max 36.80): 36.85 is higher.
        val pending = assertIs<TemperatureEvaluation.Pending>(result)
        assertEquals(7, pending.shift.firstHigherDay)
        assertEquals(t("36.80"), pending.shift.coverline)
    }

    @Test
    fun `R-03 coverline is the highest of the six reference values`() {
        val result = evaluate("$low 36.55")
        val pending = assertIs<TemperatureEvaluation.Pending>(result)
        assertEquals(7, pending.shift.firstHigherDay)
        assertEquals(listOf(1, 2, 3, 4, 5, 6), pending.shift.referenceDays)
        assertEquals(t("36.50"), pending.shift.coverline)
        assertEquals(PendingStep.SECOND_MEASUREMENT, pending.waitingFor)
    }

    @Test
    fun `R-03 equal value is not higher`() {
        assertIs<TemperatureEvaluation.Searching>(evaluate("$low 36.50 36.50"))
    }

    @Test
    fun `R-04 regular rule completes on the third measurement`() {
        val result = evaluate("$low 36.55 36.60 36.70")
        val completed = assertIs<TemperatureEvaluation.Completed>(result)
        assertEquals(TemperatureRule.REGULAR, completed.rule)
        assertEquals(9, completed.completionDay)
        assertEquals(listOf(1, 2, 3), completed.shift.measurements.map { it.number })
        assertTrue(completed.shift.measurements.all { it.counts })
    }

    @Test
    fun `R-04 waits for the third measurement`() {
        val pending = assertIs<TemperatureEvaluation.Pending>(evaluate("$low 36.55 36.60"))
        assertEquals(PendingStep.THIRD_MEASUREMENT, pending.waitingFor)
    }

    @Test
    fun `R-05 third measurement less than two tenths above needs a fourth above the coverline`() {
        val pending = assertIs<TemperatureEvaluation.Pending>(evaluate("$low 36.55 36.60 36.65"))
        assertEquals(PendingStep.FOURTH_ABOVE_COVERLINE, pending.waitingFor)

        val completed = assertIs<TemperatureEvaluation.Completed>(evaluate("$low 36.55 36.60 36.65 36.55"))
        assertEquals(TemperatureRule.EXCEPTION_1, completed.rule)
        assertEquals(10, completed.completionDay)
        assertEquals(4, completed.shift.measurements.size)
    }

    @Test
    fun `R-05 fourth measurement on the coverline fails`() {
        val result = evaluate("$low 36.55 36.60 36.65 36.50", completed = true)
        val notEvaluable = assertIs<TemperatureEvaluation.NotEvaluable>(result)
        val discarded = notEvaluable.discarded.first()
        assertEquals(7, discarded.firstHigherDay)
        assertEquals(DiscardReason.FOURTH_NOT_ABOVE_COVERLINE, discarded.reason)
        assertEquals(10, discarded.failedOnDay)
    }

    @Test
    fun `R-06 second measurement falls, fourth at least two tenths above`() {
        val pending = assertIs<TemperatureEvaluation.Pending>(evaluate("$low 36.55 36.50 36.60"))
        assertEquals(PendingStep.FOURTH_TWO_TENTHS_ABOVE, pending.waitingFor)

        val completed = assertIs<TemperatureEvaluation.Completed>(evaluate("$low 36.55 36.50 36.60 36.70"))
        assertEquals(TemperatureRule.EXCEPTION_2, completed.rule)
        assertEquals(10, completed.completionDay)
        assertEquals(listOf(true, false, true, true), completed.shift.measurements.map { it.counts })
    }

    @Test
    fun `R-06 third measurement falls, fourth at least two tenths above`() {
        val completed = assertIs<TemperatureEvaluation.Completed>(evaluate("$low 36.55 36.60 36.45 36.70"))
        assertEquals(TemperatureRule.EXCEPTION_2, completed.rule)
        assertEquals(listOf(true, true, false, true), completed.shift.measurements.map { it.counts })
    }

    @Test
    fun `R-06 third measurement falls, fourth less than two tenths above fails`() {
        val result = evaluate("$low 36.55 36.60 36.45 36.65", completed = true)
        val notEvaluable = assertIs<TemperatureEvaluation.NotEvaluable>(result)
        assertEquals(DiscardReason.FOURTH_NOT_TWO_TENTHS_ABOVE, notEvaluable.discarded.first().reason)
    }

    @Test
    fun `R-06 even a high third measurement needs a fourth two tenths above when the second fell`() {
        val result = evaluate("$low 36.55 36.50 36.80 36.65", completed = true)
        assertIs<TemperatureEvaluation.NotEvaluable>(result)
    }

    @Test
    fun `R-07 second and third both falling discards the candidate`() {
        val result = evaluate("$low 36.55 36.50 36.45", completed = true)
        val notEvaluable = assertIs<TemperatureEvaluation.NotEvaluable>(result)
        assertEquals(NotEvaluableReason.CYCLE_ENDED_WITHOUT_SHIFT, notEvaluable.reason)
        assertEquals(DiscardReason.SECOND_AND_THIRD_NOT_ABOVE, notEvaluable.discarded.single().reason)
    }

    @Test
    fun `R-07 exception rules are not combined`() {
        // Candidate day 7: third less than 0.2 above (R-05 path) and the fourth falls. R-06 must not be
        // applied on top, so day 7 is discarded. The next candidate (day 8) then fulfils R-06.
        val result = evaluate("$low 36.55 36.60 36.65 36.45 36.80", completed = true)
        val completed = assertIs<TemperatureEvaluation.Completed>(result)
        val discarded = completed.discarded.single()
        assertEquals(7, discarded.firstHigherDay)
        assertEquals(DiscardReason.FOURTH_NOT_ABOVE_COVERLINE, discarded.reason)
        assertEquals(8, completed.shift.firstHigherDay)
        assertEquals(TemperatureRule.EXCEPTION_2, completed.rule)
    }

    @Test
    fun `R-07 after a failed candidate the next possible first higher measurement is searched`() {
        // Candidate day 7 fails (both following fall). Day 10 (36.65) is higher than days 4-9 (max 36.55).
        val result = evaluate("$low 36.55 36.50 36.45 36.65 36.70 36.80")
        val completed = assertIs<TemperatureEvaluation.Completed>(result)
        assertEquals(10, completed.shift.firstHigherDay)
        assertEquals(t("36.55"), completed.shift.coverline)
        assertEquals(12, completed.completionDay)
        assertEquals(7, completed.discarded.single().firstHigherDay)
    }

    @Test
    fun `searching while no candidate exists`() {
        val result = assertIs<TemperatureEvaluation.Searching>(evaluate("$low 36.45"))
        assertEquals(7, result.validValueCount)
    }

    @Test
    fun `cycle ending while pending is not evaluable`() {
        val result = evaluate("$low 36.55 36.60", completed = true)
        val notEvaluable = assertIs<TemperatureEvaluation.NotEvaluable>(result)
        assertEquals(NotEvaluableReason.CYCLE_ENDED_WHILE_PENDING, notEvaluable.reason)
    }

    @Test
    fun `F-42 manual first higher measurement`() {
        // Automatic evaluation would take day 7; the user sets day 8.
        val temps = "$low 36.55 36.60 36.70 36.85"
        val auto = assertIs<TemperatureEvaluation.Completed>(evaluate(temps))
        assertEquals(7, auto.shift.firstHigherDay)

        val manual = assertIs<TemperatureEvaluation.Completed>(evaluate(temps, manualDay = 8))
        assertTrue(manual.shift.manual)
        assertEquals(8, manual.shift.firstHigherDay)
        assertEquals(t("36.55"), manual.shift.coverline)
        assertEquals(10, manual.completionDay)
    }

    @Test
    fun `F-42 invalid manual first higher measurement is not evaluable`() {
        assertEquals(
            NotEvaluableReason.MANUAL_DAY_WITHOUT_VALUE,
            assertIs<TemperatureEvaluation.NotEvaluable>(evaluate("$low - 36.60", manualDay = 7)).reason,
        )
        assertEquals(
            NotEvaluableReason.MANUAL_DAY_TOO_FEW_REFERENCE_VALUES,
            assertIs<TemperatureEvaluation.NotEvaluable>(evaluate(low, manualDay = 5)).reason,
        )
        assertEquals(
            NotEvaluableReason.MANUAL_DAY_NOT_HIGHER,
            assertIs<TemperatureEvaluation.NotEvaluable>(evaluate("$low 36.50 36.70", manualDay = 7)).reason,
        )
        assertEquals(
            NotEvaluableReason.MANUAL_SHIFT_FAILED,
            assertIs<TemperatureEvaluation.NotEvaluable>(evaluate("$low 36.55 36.45 36.45", manualDay = 7)).reason,
        )
    }
}
