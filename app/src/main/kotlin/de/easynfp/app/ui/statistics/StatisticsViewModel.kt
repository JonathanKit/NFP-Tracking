package de.easynfp.app.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.easynfp.app.ui.today.TodayViewModel
import de.easynfp.data.DataContainer
import de.easynfp.data.EvaluationSnapshot
import de.easynfp.rules.CycleEvaluation
import de.easynfp.rules.statistics.CycleFigures
import de.easynfp.rules.statistics.CycleStatistics
import de.easynfp.rules.statistics.PeriodForecast
import de.easynfp.rules.statistics.StatisticsSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** F-43: selectable time range. */
enum class StatisticsRange(val cycles: Int?) { LAST_6(6), LAST_12(12), ALL(null) }

data class CycleRow(
    val number: Int,
    val evaluation: CycleEvaluation,
    val figures: CycleFigures?,
    val excluded: Boolean,
)

data class StatisticsUiState(
    val loading: Boolean = true,
    val range: StatisticsRange = StatisticsRange.LAST_12,
    val summary: StatisticsSummary? = null,
    /** Completed cycles in the range, oldest first, including excluded ones (F-46). */
    val rows: List<CycleRow> = emptyList(),
    val forecast: PeriodForecast? = null,
    /** F-49, null when intercourse is switched off in the settings (F-14). */
    val intercourse: IntercourseSummary? = null,
)

class StatisticsViewModel(private val container: DataContainer) : ViewModel() {
    private val range = MutableStateFlow(StatisticsRange.LAST_12)

    val state: StateFlow<StatisticsUiState> = combine(
        container.evaluations.snapshot,
        container.settings.settings,
        range,
    ) { snapshot, settings, range ->
        build(snapshot, range, settings.showIntercourse)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), StatisticsUiState())

    fun setRange(value: StatisticsRange) {
        range.value = value
    }

    /** F-46 */
    fun setExcluded(row: CycleRow, excluded: Boolean) {
        viewModelScope.launch {
            val startDate = row.evaluation.cycle.startDate
            val cycle = container.repository.cycles.first().firstOrNull { it.startDate == startDate } ?: return@launch
            container.repository.updateCycle(cycle.copy(excludedFromStatistics = excluded))
        }
    }

    private fun build(
        snapshot: EvaluationSnapshot,
        range: StatisticsRange,
        showIntercourse: Boolean,
    ): StatisticsUiState {
        val completed = snapshot.evaluations.withIndex().filter { it.value.cycle.isCompleted }
        val selected = range.cycles?.let { completed.takeLast(it) } ?: completed
        val rows = selected.map { (index, evaluation) ->
            CycleRow(
                number = index + 1,
                evaluation = evaluation,
                figures = CycleStatistics.figuresOf(evaluation),
                excluded = snapshot.recordFor(evaluation)?.excludedFromStatistics == true,
            )
        }
        val included = rows.filterNot { it.excluded }.map { it.evaluation }
        return StatisticsUiState(
            loading = false,
            range = range,
            summary = CycleStatistics.summarize(included),
            rows = rows,
            forecast = TodayViewModel.forecastFor(snapshot),
            intercourse = if (showIntercourse) IntercourseStatistics.summarize(snapshot, included) else null,
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
