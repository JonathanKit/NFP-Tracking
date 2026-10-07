package de.easynfp.app

import de.easynfp.app.ui.chart.ChartModel
import de.easynfp.rules.Phase
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class ChartModelTest {
    @Test
    fun `F-31 marks reference values, higher measurements and the mucus peak`() {
        val snapshot = TestData.snapshot()
        val model = ChartModel.build(snapshot.evaluations[1], snapshot.entries, snapshot.today, showIntercourse = true)
        assertEquals(28, model.columns.size)
        assertEquals((9..14).toList(), model.columns.filter { it.referenceNumber != null }.map { it.cycleDay })
        assertEquals(
            listOf(15 to 1, 16 to 2, 17 to 3),
            model.columns.mapNotNull { c ->
                c.higherNumber?.let {
                    c.cycleDay to it
                }
            },
        )
        assertEquals("H", model.columns[14].peakMarker)
        assertEquals("3", model.columns[17].peakMarker)
        assertEquals(3650, model.coverline?.hundredths)
        assertEquals(9, model.coverlineFromDay)
        assertEquals(17, model.coverlineToDay)
        assertTrue(model.yMin % 5 == 0 && model.yMax % 5 == 0)
        assertTrue(model.yMin < 3635 && model.yMax > 3685)
    }

    @Test
    fun `current cycle shows at least 28 columns, future days without phase`() {
        val snapshot = TestData.snapshot(currentDay = 10)
        val model = ChartModel.build(
            snapshot.evaluations.last(),
            snapshot.entries,
            snapshot.today,
            showIntercourse = false,
        )
        assertEquals(28, model.columns.size)
        assertTrue(model.columns[9].isToday)
        assertNull(model.columns[10].phase)
        assertTrue(model.columns.none { it.isForecast })
    }

    @Test
    fun `F-31 first higher measurement column is marked`() {
        val snapshot = TestData.snapshot()
        val model = ChartModel.build(snapshot.evaluations[1], snapshot.entries, snapshot.today, showIntercourse = false)
        assertEquals(15, model.firstHigherDay)
    }

    @Test
    fun `F-47 forecast fills only future days of the current cycle`() {
        val snapshot = TestData.snapshot(currentDay = 10)
        val forecast = snapshot.forecast
        val current = ChartModel.build(
            snapshot.evaluations.last(),
            snapshot.entries,
            snapshot.today,
            showIntercourse = false,
            forecast = forecast,
        )
        assertTrue(current.columns.take(10).none { it.isForecast })
        assertTrue(current.columns.drop(10).all { it.isForecast && it.phase == null })
        assertEquals(Phase.INFERTILE_END, current.columns[19].forecastPhase?.phase)
        // Completed cycles never show a forecast.
        val past = ChartModel.build(snapshot.evaluations[0], snapshot.entries, snapshot.today, false, forecast)
        assertTrue(past.columns.none { it.isForecast })
    }
}
