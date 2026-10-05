package de.easynfp.app.ui.day

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.easynfp.data.DataContainer
import de.easynfp.data.model.DayEntry
import de.easynfp.data.model.Disturbance
import de.easynfp.data.model.Intercourse
import de.easynfp.rules.NewCycleSuggestion
import de.easynfp.rules.model.Bleeding
import de.easynfp.rules.model.MucusAppearance
import de.easynfp.rules.model.MucusSensation
import de.easynfp.rules.model.Temperature
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The editable state of one day; only written on "Speichern". */
data class DayDraft(
    val entry: DayEntry,
    val isPeriodStart: Boolean,
    /** F-11: the exclusion question was already asked while editing this day. */
    val excludeQuestionAsked: Boolean = false,
    val showExcludeQuestion: Boolean = false,
) {
    fun differsFrom(other: DayDraft?): Boolean =
        other != null && (other.entry != entry || other.isPeriodStart != isPeriodStart)
}

data class DayEntryUiState(
    val date: LocalDate,
    val today: LocalDate,
    val loading: Boolean = true,
    val draft: DayDraft? = null,
    val dirty: Boolean = false,
    val cycleDay: Int? = null,
    val disturbances: List<Disturbance> = emptyList(),
    val temperaturePreset: Temperature = DEFAULT_PRESET,
    val suggestNewCycle: Boolean = false,
    val saved: Boolean = false,
) {
    companion object {
        val DEFAULT_PRESET = Temperature(3650)
    }
}

/** Everything the day entry form can change. */
interface DayEntryActions {
    fun setTemperature(value: Temperature?)

    fun setMeasurementTime(time: LocalTime)

    fun setExcluded(excluded: Boolean)

    fun setSensation(value: MucusSensation?)

    fun setAppearance(value: MucusAppearance?)

    fun setBleeding(value: Bleeding?)

    fun setIntercourse(value: Intercourse?)

    fun setMittelschmerz(value: Boolean)

    fun setBreastSymptom(value: Boolean)

    fun setNote(value: String)

    fun setPeriodStart(value: Boolean)

    fun toggleDisturbance(id: Long)

    fun answerExcludeQuestion(exclude: Boolean)
}

class DayEntryViewModel(
    private val container: DataContainer,
    handle: SavedStateHandle,
) : ViewModel(),
    DayEntryActions {
    val date: LocalDate = LocalDate.parse(checkNotNull(handle.get<String>("date")))

    private val draft = MutableStateFlow<DayDraft?>(null)
    private var original: DayDraft? = null
    private val saved = MutableStateFlow(false)
    private var preset: Temperature = DayEntryUiState.DEFAULT_PRESET

    init {
        viewModelScope.launch {
            val repository = container.repository
            val entry = repository.getDay(date) ?: DayEntry(date)
            val isStart = repository.cycles.first().any { it.startDate == date }
            preset = repository.latestTemperatureBefore(date) ?: DayEntryUiState.DEFAULT_PRESET
            val initial = DayDraft(entry, isStart)
            original = initial
            draft.value = initial
        }
    }

    val state: StateFlow<DayEntryUiState> = combine(
        draft,
        saved,
        container.evaluations.snapshot,
        container.repository.disturbances,
    ) { draft, saved, snapshot, disturbances ->
        val evaluation = snapshot.evaluationFor(date)
        DayEntryUiState(
            date = date,
            today = snapshot.today,
            loading = draft == null,
            draft = draft,
            dirty = draft != null && draft.differsFrom(original),
            cycleDay = evaluation?.cycle?.cycleDayOf(date),
            disturbances = disturbances.filter { !it.archived || it.id in draft?.entry?.disturbanceIds.orEmpty() },
            temperaturePreset = preset,
            suggestNewCycle = draft != null && !draft.isPeriodStart &&
                NewCycleSuggestion.shouldSuggest(evaluation, date, draft.entry.bleeding),
            saved = saved,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DayEntryUiState(date, date))

    private fun edit(transform: (DayEntry) -> DayEntry) {
        draft.update { it?.copy(entry = transform(it.entry)) }
    }

    override fun setTemperature(value: Temperature?) = edit {
        it.copy(
            temperature = value,
            measurementTime = when {
                value == null -> null
                else -> it.measurementTime ?: LocalTime.now(container.clock).withSecond(0).withNano(0)
            },
        )
    }

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
        draft.update { it?.copy(isPeriodStart = value) }
    }

    /** F-09, F-11: assigning a disturbance asks once whether the temperature should be excluded. */
    override fun toggleDisturbance(id: Long) {
        draft.update { current ->
            current ?: return@update null
            val ids = current.entry.disturbanceIds
            val adding = id !in ids
            val entry = current.entry.copy(disturbanceIds = if (adding) ids + id else ids - id)
            val ask = adding && !current.excludeQuestionAsked && entry.temperature != null && !entry.temperatureExcluded
            current.copy(
                entry = entry,
                showExcludeQuestion = ask,
                excludeQuestionAsked = current.excludeQuestionAsked || ask,
            )
        }
    }

    override fun answerExcludeQuestion(exclude: Boolean) {
        draft.update { current ->
            current?.copy(
                showExcludeQuestion = false,
                entry = if (exclude) current.entry.copy(temperatureExcluded = true) else current.entry,
            )
        }
    }

    fun save() {
        val current = draft.value ?: return
        viewModelScope.launch {
            container.repository.saveDay(current.entry)
            if (current.isPeriodStart != original?.isPeriodStart) {
                container.repository.setPeriodStart(date, current.isPeriodStart)
            }
            saved.value = true
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
