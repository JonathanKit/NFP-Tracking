package de.easynfp.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.easynfp.app.quickentry.QuickEntryContent
import de.easynfp.app.quickentry.QuickEntryUiState
import de.easynfp.app.ui.theme.QuickEntryTheme
import de.easynfp.rules.model.Temperature
import java.time.LocalDate
import java.util.Locale
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** N-08: UI test of the quick entry (F-21: preset value, one tap to save). */
@RunWith(AndroidJUnit4::class)
class QuickEntryContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Before
    fun germanLocale() {
        Locale.setDefault(Locale.GERMANY)
    }

    @Test
    fun `preset value is shown and saved with one tap`() {
        var saved = 0
        var state by mutableStateOf(
            QuickEntryUiState(loading = false, date = LocalDate.of(2026, 10, 5), value = Temperature(3655)),
        )
        compose.setContent {
            QuickEntryTheme {
                QuickEntryContent(
                    state = state,
                    onValueChange = { state = state.copy(value = it) },
                    onTimeChange = {},
                    onSave = { saved++ },
                    onCancel = {},
                )
            }
        }
        compose.onNodeWithText("36,55").assertIsDisplayed()
        compose.onNodeWithText("Speichern").performClick()
        assertEquals(1, saved)
        assertEquals(Temperature(3655), state.value)
    }

    @Test
    fun `stepper buttons change the value`() {
        var state by mutableStateOf(
            QuickEntryUiState(loading = false, date = LocalDate.of(2026, 10, 5), value = Temperature(3655)),
        )
        compose.setContent {
            QuickEntryTheme {
                QuickEntryContent(state, { state = state.copy(value = it) }, {}, {}, {})
            }
        }
        compose.onNodeWithText("+0,01").performClick()
        compose.waitForIdle()
        assertEquals(Temperature(3656), state.value)
        compose.onNodeWithText("−0,10").performClick()
        compose.waitForIdle()
        assertEquals(Temperature(3646), state.value)
    }
}
