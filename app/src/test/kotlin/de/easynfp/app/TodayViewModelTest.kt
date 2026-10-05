package de.easynfp.app

import de.easynfp.app.ui.today.TodayViewModel
import de.easynfp.app.ui.today.Todo
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class TodayViewModelTest {
    @Test
    fun `F-36 open entries`() {
        val snapshot = TestData.snapshot(currentDay = 10)
        // Day 10 has an entry with temperature; remove it to see the missing temperature.
        val withoutToday = snapshot.copy(entries = snapshot.entries - snapshot.today)
        val todos = TodayViewModel.todosFor(withoutToday)
        assertTrue(Todo.TemperatureMissing in todos)
        assertTrue(TodayViewModel.todosFor(snapshot).none { it is Todo.TemperatureMissing })
    }

    @Test
    fun `F-45 forecast needs three completed cycles`() {
        assertNull(TodayViewModel.forecastFor(TestData.snapshot()))
    }

    @Test
    fun `F-08 new cycle is suggested on bleeding after the high phase`() {
        val snapshot = TestData.snapshot(currentDay = 28)
        val today = snapshot.entries.getValue(snapshot.today).copy(bleeding = de.easynfp.rules.model.Bleeding.HEAVY)
        val todos = TodayViewModel.todosFor(snapshot.copy(entries = snapshot.entries + (snapshot.today to today)))
        assertEquals(Todo.NewCycleSuggested(snapshot.today), todos.first())
    }
}
