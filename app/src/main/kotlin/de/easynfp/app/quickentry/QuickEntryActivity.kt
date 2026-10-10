package de.easynfp.app.quickentry

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.easynfp.app.LockScreen
import de.easynfp.app.R
import de.easynfp.app.lock.AppLock
import de.easynfp.app.text.Formats
import de.easynfp.app.ui.components.TemperatureWheel
import de.easynfp.app.ui.components.TimePickerDialogFor
import de.easynfp.app.ui.theme.QuickEntryTheme
import de.easynfp.data.DataContainer
import de.easynfp.rules.model.Temperature
import java.time.LocalTime

/**
 * F-21 to F-23: opened from the morning reminder or from "Heute". Dark and dim, independent of the
 * system theme, and closes itself after saving.
 */
class QuickEntryActivity : FragmentActivity() {

    private val viewModel: QuickEntryViewModel by viewModels {
        viewModelFactory { initializer { QuickEntryViewModel(DataContainer.get(applicationContext)) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // F-22: dark colors only. The screen brightness is left as the user set it (never raised or lowered).
        val lockEnabled = DataContainer.get(this).settings.settings
        setContent {
            QuickEntryTheme {
                val settings by lockEnabled.collectAsState(initial = null)
                val unlocked by AppLock.unlocked.collectAsState()
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when {
                        settings == null -> Unit
                        settings?.appLockEnabled == true && !unlocked -> LockScreen(this)
                        else -> QuickEntryScreen(viewModel, onDone = ::finish, onCancel = ::finish)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickEntryScreen(viewModel: QuickEntryViewModel, onDone: () -> Unit, onCancel: () -> Unit) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    if (state.loading) return
    QuickEntryContent(
        state = state,
        onValueChange = viewModel::setValue,
        onTimeChange = viewModel::setTime,
        onSave = viewModel::save,
        onCancel = onCancel,
    )
}

/** Stateless quick entry UI (tested in QuickEntryContentTest). */
@Composable
fun QuickEntryContent(
    state: QuickEntryUiState,
    onValueChange: (Temperature) -> Unit,
    onTimeChange: (LocalTime) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                Formats.dateLong(context.resources, state.date),
                style = MaterialTheme.typography.titleMedium,
            )
            val existing = state.existing
            Text(
                if (existing != null) {
                    stringResource(R.string.quick_entry_existing, Formats.temperature(existing))
                } else {
                    stringResource(R.string.quick_entry_hint)
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        TemperatureWheel(value = state.value, onValueChange = onValueChange, itemHeight = 64.dp, large = true)

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val time = state.manualTime
                Text(
                    if (time != null) {
                        stringResource(R.string.measurement_time, Formats.time(time))
                    } else {
                        stringResource(R.string.quick_entry_time_now)
                    },
                )
                TextButton(onClick = { showTimePicker = true }) { Text(stringResource(R.string.change)) }
            }
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
            ) {
                Text(stringResource(R.string.save), fontSize = 22.sp)
            }
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        }
    }
    if (showTimePicker) {
        TimePickerDialogFor(
            initial = state.manualTime ?: LocalTime.now(),
            onConfirm = {
                showTimePicker = false
                onTimeChange(it)
            },
            onDismiss = { showTimePicker = false },
        )
    }
}
