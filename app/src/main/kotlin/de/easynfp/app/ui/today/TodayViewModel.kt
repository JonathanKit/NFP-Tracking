package de.easynfp.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.easynfp.data.DataContainer
import de.easynfp.data.EvaluationSnapshot
import de.easynfp.data.settings.AppSettings
import de.easynfp.rules.NewCycleSuggestion
import de.easynfp.rules.mucus.MucusEvaluation
import de.easynfp.rules.statistics.PeriodForecast
import de.easynfp.rules.statistics.PeriodForecaster
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Open tasks shown on the start page (F-36). */
sealed interface Todo {
    data object TemperatureMissing : Todo

    data class MucusMissing(val date: LocalDate) : Todo

    /** R-16 */
    data class MucusGap(val days: List<LocalDate>) : Todo

    /** F-08 */
    data class NewCycleSuggested(val date: LocalDate) : Todo
}

data class TodayUiState(
    val loading: Boolean = true,
    val snapshot: EvaluationSnapshot? = null,
    val settings: AppSettings = AppSettings(),
    val todos: List<Todo> = emptyList(),
    val forecast: PeriodForecast? = null,
)

class TodayViewModel(private val container: DataContainer) : ViewModel() {

    val state: StateFlow<TodayUiState> =
        combine(container.evaluations.snapshot, container.settings.settings) { snapshot, settings ->
            TodayUiState(
                loading = false,
                snapshot = snapshot,
                settings = settings,
                todos = todosFor(snapshot),
                forecast = forecastFor(snapshot),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), TodayUiState())

    /** First use: the user picks the first day of her last period. */
    fun setPeriodStart(date: LocalDate) {
        viewModelScope.launch { container.repository.setPeriodStart(date, true) }
    }

    fun dismissHints() {
        viewModelScope.launch { container.settings.update { it.copy(measuringHintsDismissed = true) } }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun todosFor(snapshot: EvaluationSnapshot): List<Todo> {
            val current = snapshot.current ?: return emptyList()
            val today = snapshot.today
            val todayEntry = snapshot.entries[today]
            return buildList {
                if (NewCycleSuggestion.shouldSuggest(current, today, todayEntry?.bleeding)) {
                    add(Todo.NewCycleSuggested(today))
                }
                if (todayEntry?.temperature == null) add(Todo.TemperatureMissing)
                val yesterday = today.minusDays(1)
                val yesterdayEntry = snapshot.entries[yesterday]
                if (yesterday in current.cycle && yesterdayEntry?.hasMucusEntry != true) {
                    add(Todo.MucusMissing(yesterday))
                }
                val mucus = current.mucus
                if (mucus is MucusEvaluation.MissingEntries) {
                    add(Todo.MucusGap(mucus.missingDays.map(current.cycle::dateOf)))
                }
            }
        }

        /** F-45: estimate from completed cycles that are not excluded from statistics (F-46). */
        fun forecastFor(snapshot: EvaluationSnapshot): PeriodForecast? {
            val current = snapshot.current ?: return null
            val lengths = snapshot.evaluations
                .filter { evaluation -> snapshot.recordFor(evaluation)?.excludedFromStatistics != true }
                .mapNotNull { it.cycle.length }
            return PeriodForecaster.forecast(lengths, current.cycle.startDate)
        }
    }
}
