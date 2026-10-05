package de.easynfp.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.easynfp.app.ui.day.DayDraft
import de.easynfp.app.ui.day.DayEntryActions
import de.easynfp.app.ui.day.DayEntryContent
import de.easynfp.app.ui.day.DayEntryUiState
import de.easynfp.app.ui.theme.EasyNfpTheme
import de.easynfp.data.model.DayEntry
import de.easynfp.data.model.Disturbance
import de.easynfp.data.model.Intercourse
import de.easynfp.data.model.PredefinedDisturbance
import de.easynfp.data.settings.AppSettings
import de.easynfp.rules.model.Bleeding
import de.easynfp.rules.model.MucusAppearance
import de.easynfp.rules.model.MucusSensation
import de.easynfp.rules.model.Temperature
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** N-08: UI test of the day entry, with a fake implementing the form actions. */
@RunWith(AndroidJUnit4::class)
class DayEntryContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val date = LocalDate.of(2026, 10, 5)

    private class FakeActions(initial: DayDraft) : DayEntryActions {
        var draft by mutableStateOf(initial)

        private fun edit(transform: (DayEntry) -> DayEntry) {
            draft = draft.copy(entry = transform(draft.entry))
        }

        override fun setTemperature(value: Temperature?) = edit { it.copy(temperature = value) }
        override fun setMeasurementTime(time: LocalTime) = edit { it.copy(measurementTime = time) }
        override fun setExcluded(excluded: Boolean) = edit { it.copy(temperatureExcluded = excluded) }
        override fun setSensation(value: MucusSensation?) = edit { it.copy(mucusSensation = value) }
        override fun setAppearance(value: MucusAppearance?) = edit { it.copy(mucusAppearance = value) }
        override fun setBleeding(value: Bleeding?) = edit { it.copy(bleeding = value) }
        override fun setIntercourse(value: Intercourse?) = edit { it.copy(intercourse = value) }
        override fun setMittelschmerz(value: Boolean) = edit { it.copy(mittelschmerz = value) }
        override fun setBreastSymptom(value: Boolean) = edit { it.copy(breastSymptom = value) }
        override fun setNote(value: String) = edit { it.copy(note = value) }
        override fun setPeriodStart(value: Boolean) {
            draft = draft.copy(isPeriodStart = value)
        }
        override fun toggleDisturbance(id: Long) {
            val ids = draft.entry.disturbanceIds
            draft = draft.copy(
                entry = draft.entry.copy(disturbanceIds = if (id in ids) ids - id else ids + id),
                showExcludeQuestion = draft.entry.temperature != null,
            )
        }
        override fun answerExcludeQuestion(exclude: Boolean) {
            draft = draft.copy(
                showExcludeQuestion = false,
                entry = draft.entry.copy(temperatureExcluded = exclude),
            )
        }
    }

    private fun show(actions: FakeActions) {
        compose.setContent {
            EasyNfpTheme {
                DayEntryContent(
                    state = DayEntryUiState(
                        date = date,
                        today = date,
                        loading = false,
                        draft = actions.draft,
                        cycleDay = 12,
                        disturbances = listOf(Disturbance(1, PredefinedDisturbance.ALCOHOL, null, false, 0)),
                    ),
                    draft = actions.draft,
                    settings = AppSettings(),
                    actions = actions,
                    onManageDisturbances = {},
                )
            }
        }
    }

    @Test
    fun `F-05 F-06 mucus observation shows the derived category`() {
        val actions = FakeActions(DayDraft(DayEntry(date), isPeriodStart = false))
        show(actions)
        compose.onNodeWithText("feucht").performScrollTo().performClick()
        compose.onNodeWithText("Kategorie f").performScrollTo()
        compose.onNodeWithText("glasig, dehnbar, fadenziehend, flüssig").performScrollTo().performClick()
        compose.onNodeWithText("Kategorie S+").performScrollTo()
        assertEquals(MucusSensation.MOIST, actions.draft.entry.mucusSensation)
        assertEquals(MucusAppearance.CLEAR_STRETCHY, actions.draft.entry.mucusAppearance)
    }

    @Test
    fun `F-07 F-08 bleeding and period start`() {
        val actions = FakeActions(DayDraft(DayEntry(date), isPeriodStart = false))
        show(actions)
        compose.onNodeWithText("stark").performScrollTo().performClick()
        compose.onNodeWithText("Neue Periode beginnt an diesem Tag").performScrollTo().performClick()
        assertEquals(Bleeding.HEAVY, actions.draft.entry.bleeding)
        assertTrue(actions.draft.isPeriodStart)
    }

    @Test
    fun `F-11 assigning a disturbance asks whether to exclude the temperature`() {
        val actions = FakeActions(DayDraft(DayEntry(date, temperature = Temperature(3690)), isPeriodStart = false))
        show(actions)
        compose.onNodeWithText("Alkohol").performScrollTo().performClick()
        compose.onNodeWithText("Ausklammern").performClick()
        assertTrue(actions.draft.entry.temperatureExcluded)
        assertEquals(setOf(1L), actions.draft.entry.disturbanceIds)
    }
}
