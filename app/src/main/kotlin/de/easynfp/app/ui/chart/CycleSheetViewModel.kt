package de.easynfp.app.ui.chart

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.easynfp.data.DataContainer
import de.easynfp.data.EvaluationSnapshot
import de.easynfp.data.model.CycleRecord
import de.easynfp.rules.CycleEvaluation
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CycleSheetUiState(
    val loading: Boolean = true,
    val snapshot: EvaluationSnapshot? = null,
    /** Page to show first: the requested cycle or the current one. */
    val initialPage: Int = 0,
)

class CycleSheetViewModel(
    private val container: DataContainer,
    handle: SavedStateHandle,
) : ViewModel() {
    private val requestedStart: LocalDate? = handle.get<String>("start")?.let(LocalDate::parse)

    val state: StateFlow<CycleSheetUiState> = container.evaluations.snapshot.map { snapshot ->
        val requested = requestedStart?.let { start ->
            snapshot.evaluations.indexOfFirst { it.cycle.startDate == start }
        }
        CycleSheetUiState(
            loading = false,
            snapshot = snapshot,
            initialPage = requested?.takeIf { it >= 0 } ?: snapshot.evaluations.lastIndex.coerceAtLeast(0),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CycleSheetUiState())

    /** F-42 */
    fun setFirstHigherOverride(evaluation: CycleEvaluation, date: LocalDate?) = updateRecord(evaluation) {
        it.copy(overrides = it.overrides.copy(firstHigherMeasurement = date))
    }

    /** F-42 */
    fun setPeakOverride(evaluation: CycleEvaluation, date: LocalDate?) = updateRecord(evaluation) {
        it.copy(overrides = it.overrides.copy(mucusPeak = date))
    }

    /** F-46 */
    fun setExcludedFromStatistics(evaluation: CycleEvaluation, excluded: Boolean) = updateRecord(evaluation) {
        it.copy(excludedFromStatistics = excluded)
    }

    private fun updateRecord(evaluation: CycleEvaluation, transform: (CycleRecord) -> CycleRecord) {
        val record = state.value.snapshot?.recordFor(evaluation) ?: return
        viewModelScope.launch { container.repository.updateCycle(transform(record)) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
