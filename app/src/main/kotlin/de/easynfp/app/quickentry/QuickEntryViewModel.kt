package de.easynfp.app.quickentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.easynfp.data.DataContainer
import de.easynfp.rules.model.Temperature
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuickEntryUiState(
    val loading: Boolean = true,
    val date: LocalDate = LocalDate.MIN,
    val value: Temperature = DEFAULT,
    /** Value already stored for today, if any. */
    val existing: Temperature? = null,
    /** F-21: null means "time of saving"; set when the user changes the time explicitly. */
    val manualTime: LocalTime? = null,
    val saved: Boolean = false,
) {
    companion object {
        val DEFAULT = Temperature(3650)
    }
}

/** F-21: today's temperature in at most two taps (open + save) if the value matches the day before. */
class QuickEntryViewModel(private val container: DataContainer) : ViewModel() {
    private val mutableState = MutableStateFlow(QuickEntryUiState())
    val state: StateFlow<QuickEntryUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val today = LocalDate.now(container.clock)
            val repository = container.repository
            val existing = repository.getDay(today)
            val preset = existing?.temperature ?: repository.latestTemperatureBefore(today) ?: QuickEntryUiState.DEFAULT
            mutableState.value = QuickEntryUiState(
                loading = false,
                date = today,
                value = preset,
                existing = existing?.temperature,
                manualTime = if (existing?.temperature != null) existing.measurementTime else null,
            )
        }
    }

    fun setValue(value: Temperature) = mutableState.update { it.copy(value = value) }

    fun setTime(time: LocalTime) = mutableState.update { it.copy(manualTime = time) }

    fun save() {
        val current = mutableState.value
        if (current.loading || current.saved) return
        viewModelScope.launch {
            val time = current.manualTime ?: LocalTime.now(container.clock).withSecond(0).withNano(0)
            container.repository.saveTemperature(current.date, current.value, time)
            mutableState.update { it.copy(saved = true) }
        }
    }
}
