package de.easynfp.app

import de.easynfp.app.ui.statistics.IntercourseStatistics
import de.easynfp.data.model.Intercourse
import kotlin.test.assertEquals
import org.junit.Test

class IntercourseStatisticsTest {
    @Test
    fun `F-49 days with intercourse per cycle, kind and phase`() {
        val base = TestData.snapshot(currentDay = 10)
        val marks = mapOf(
            // Cycle 1: day 14 (fertile), day 18 (infertile only from the evening).
            TestData.START.plusDays(13) to Intercourse.UNPROTECTED,
            TestData.START.plusDays(17) to Intercourse.PROTECTED,
            // Cycle 2: day 2 (infertile start), day 22 (infertile end).
            TestData.START.plusDays(28 + 1L) to Intercourse.PROTECTED,
            TestData.START.plusDays(28 + 21L) to Intercourse.UNPROTECTED,
            // Current cycle: day 3.
            TestData.START.plusDays(56 + 2L) to Intercourse.PROTECTED,
        )
        val snapshot = base.copy(
            entries = base.entries.mapValues { (date, entry) -> entry.copy(intercourse = marks[date]) },
        )
        val completed = snapshot.evaluations.filter { it.cycle.isCompleted }

        val summary = IntercourseStatistics.summarize(snapshot, completed)

        assertEquals(listOf(2, 2), summary.perCycle)
        assertEquals(4, summary.totalDays)
        assertEquals(2.0, summary.meanPerCycle)
        assertEquals(2, summary.protectedDays)
        assertEquals(2, summary.unprotectedDays)
        assertEquals(1, summary.infertileStartDays)
        assertEquals(2, summary.fertileDays)
        assertEquals(1, summary.fertileUnprotectedDays)
        assertEquals(1, summary.infertileEndDays)
        assertEquals(1, summary.currentCycle)
    }

    @Test
    fun `F-49 without cycles there is nothing to count`() {
        val summary = IntercourseStatistics.summarize(TestData.snapshot(), emptyList())
        assertEquals(0, summary.totalDays)
        assertEquals(0.0, summary.meanPerCycle)
    }
}
