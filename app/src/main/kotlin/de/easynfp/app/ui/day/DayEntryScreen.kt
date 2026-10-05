package de.easynfp.app.ui.day

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.easynfp.app.R
import de.easynfp.app.text.Formats
import de.easynfp.app.ui.components.ChoiceChips
import de.easynfp.app.ui.components.ConfirmDialog
import de.easynfp.app.ui.components.SectionCard
import de.easynfp.app.ui.components.SubScreenScaffold
import de.easynfp.app.ui.components.SwitchRow
import de.easynfp.app.ui.components.TemperatureWheel
import de.easynfp.app.ui.components.TimePickerDialogFor
import de.easynfp.app.ui.components.appViewModelFactory
import de.easynfp.app.ui.navigation.AppNavigator
import de.easynfp.data.model.Intercourse
import de.easynfp.data.settings.AppSettings
import de.easynfp.rules.model.Bleeding
import de.easynfp.rules.model.MucusAppearance
import de.easynfp.rules.model.MucusSensation
import java.time.LocalTime

@Composable
fun DayEntryScreen(
    navigator: AppNavigator,
    settings: AppSettings,
    viewModel: DayEntryViewModel = viewModel(
        factory = appViewModelFactory { c, _, handle -> DayEntryViewModel(c, handle) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.saved) { if (state.saved) navigator.back() }
    BackHandler(enabled = state.dirty) { confirmDiscard = true }

    val title = Formats.dateLong(context.resources, state.date)
    SubScreenScaffold(
        title = title,
        onBack = { if (state.dirty) confirmDiscard = true else navigator.back() },
        actions = {
            IconButton(onClick = { navigator.replaceDay(state.date.minusDays(1)) }, enabled = !state.dirty) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous_day))
            }
            IconButton(
                onClick = { navigator.replaceDay(state.date.plusDays(1)) },
                enabled = !state.dirty && state.date < state.today,
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_day))
            }
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = viewModel::save,
                    enabled = state.draft != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) { Text(stringResource(R.string.save)) }
            }
        },
    ) { padding ->
        val draft = state.draft
        if (draft == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@SubScreenScaffold
        }
        DayEntryContent(
            state = state,
            draft = draft,
            settings = settings,
            actions = viewModel,
            onManageDisturbances = navigator::openDisturbances,
            modifier = Modifier.padding(padding),
        )
    }

    if (confirmDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.discard_title),
            text = stringResource(R.string.discard_text),
            confirmLabel = stringResource(R.string.discard_confirm),
            onConfirm = {
                confirmDiscard = false
                navigator.back()
            },
            onDismiss = { confirmDiscard = false },
        )
    }
}

/** Stateless day entry form; all changes go through [actions] (tested in DayEntryContentTest). */
@Composable
fun DayEntryContent(
    state: DayEntryUiState,
    draft: DayDraft,
    settings: AppSettings,
    actions: DayEntryActions,
    onManageDisturbances: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = actions
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        state.cycleDay?.let {
            Text(stringResource(R.string.cycle_day, it), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.suggestNewCycle) {
            SectionCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                Text(stringResource(R.string.new_cycle_suggestion))
                Button(onClick = { viewModel.setPeriodStart(true) }) {
                    Text(stringResource(R.string.new_cycle_confirm))
                }
            }
        }
        TemperatureSection(state, viewModel)
        MucusSection(draft.entry.mucusSensation, draft.entry.mucusAppearance, viewModel)
        BleedingSection(draft, viewModel)
        DisturbanceSection(state, viewModel, onManage = onManageDisturbances)
        OptionalSection(settings, draft.entry, viewModel)
        SectionCard(title = stringResource(R.string.section_note)) {
            OutlinedTextField(
                value = draft.entry.note.orEmpty(),
                onValueChange = viewModel::setNote,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.note_placeholder)) },
                minLines = 2,
            )
        }
    }

    if (draft.showExcludeQuestion) {
        ConfirmDialog(
            title = stringResource(R.string.exclude_question_title),
            text = stringResource(R.string.exclude_question_text),
            confirmLabel = stringResource(R.string.exclude_question_yes),
            dismissLabel = stringResource(R.string.exclude_question_no),
            onConfirm = { viewModel.answerExcludeQuestion(true) },
            onDismiss = { viewModel.answerExcludeQuestion(false) },
        )
    }
}

@Composable
private fun TemperatureSection(state: DayEntryUiState, viewModel: DayEntryActions) {
    val entry = state.draft?.entry ?: return
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    SectionCard(title = stringResource(R.string.section_temperature)) {
        val temperature = entry.temperature
        if (temperature == null) {
            OutlinedButton(onClick = { viewModel.setTemperature(state.temperaturePreset) }) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text(stringResource(R.string.add_temperature))
            }
        } else {
            TemperatureWheel(value = temperature, onValueChange = viewModel::setTemperature, itemHeight = 40.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(
                        R.string.measurement_time,
                        entry.measurementTime?.let(Formats::time) ?: "–",
                    ),
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { showTimePicker = true }) { Text(stringResource(R.string.change)) }
            }
            SwitchRow(
                title = stringResource(R.string.exclude_temperature),
                subtitle = stringResource(R.string.exclude_temperature_hint),
                checked = entry.temperatureExcluded,
                onCheckedChange = viewModel::setExcluded,
            )
            TextButton(onClick = { viewModel.setTemperature(null) }) {
                Text(stringResource(R.string.remove_temperature))
            }
        }
    }
    if (showTimePicker) {
        TimePickerDialogFor(
            initial = entry.measurementTime ?: LocalTime.now(),
            onConfirm = {
                showTimePicker = false
                viewModel.setMeasurementTime(it)
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

@Composable
private fun MucusSection(sensation: MucusSensation?, appearance: MucusAppearance?, viewModel: DayEntryActions) {
    val resources = LocalContext.current.resources
    SectionCard(title = stringResource(R.string.section_mucus)) {
        Text(stringResource(R.string.mucus_best_hint), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.mucus_sensation), fontWeight = FontWeight.Medium)
        ChoiceChips(
            options = MucusSensation.entries,
            selected = sensation,
            label = { Formats.sensation(resources, it) },
            onSelect = viewModel::setSensation,
        )
        Text(stringResource(R.string.mucus_appearance), fontWeight = FontWeight.Medium)
        ChoiceChips(
            options = MucusAppearance.entries,
            selected = appearance,
            label = { Formats.appearance(resources, it) },
            onSelect = viewModel::setAppearance,
        )
        // F-06: derived category
        val category = de.easynfp.rules.model.MucusCategory.derive(sensation, appearance)
        Text(
            if (category != null) {
                stringResource(R.string.mucus_category, category.symbol)
            } else {
                stringResource(R.string.mucus_category_none)
            },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun BleedingSection(draft: DayDraft, viewModel: DayEntryActions) {
    val resources = LocalContext.current.resources
    SectionCard(title = stringResource(R.string.section_bleeding)) {
        ChoiceChips(
            options = Bleeding.entries,
            selected = draft.entry.bleeding,
            label = { Formats.bleeding(resources, it) },
            onSelect = viewModel::setBleeding,
        )
        SwitchRow(
            title = stringResource(R.string.period_starts_today),
            subtitle = stringResource(R.string.period_starts_today_hint),
            checked = draft.isPeriodStart,
            onCheckedChange = viewModel::setPeriodStart,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DisturbanceSection(state: DayEntryUiState, viewModel: DayEntryActions, onManage: () -> Unit) {
    val resources = LocalContext.current.resources
    val selected = state.draft?.entry?.disturbanceIds.orEmpty()
    SectionCard(title = stringResource(R.string.section_disturbances)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.disturbances.forEach { disturbance ->
                FilterChip(
                    selected = disturbance.id in selected,
                    onClick = { viewModel.toggleDisturbance(disturbance.id) },
                    label = { Text(Formats.disturbanceName(resources, disturbance)) },
                )
            }
        }
        TextButton(onClick = onManage) { Text(stringResource(R.string.manage_disturbances)) }
    }
}

@Composable
private fun OptionalSection(settings: AppSettings, entry: de.easynfp.data.model.DayEntry, viewModel: DayEntryActions) {
    if (!settings.showIntercourse && !settings.showMittelschmerz && !settings.showBreastSymptom) return
    SectionCard(title = stringResource(R.string.section_optional)) {
        if (settings.showIntercourse) {
            Text(stringResource(R.string.intercourse), fontWeight = FontWeight.Medium)
            ChoiceChips(
                options = Intercourse.entries,
                selected = entry.intercourse,
                label = {
                    stringResource(
                        if (it == Intercourse.PROTECTED) {
                            R.string.intercourse_protected
                        } else {
                            R.string.intercourse_unprotected
                        },
                    )
                },
                onSelect = viewModel::setIntercourse,
            )
        }
        if (settings.showMittelschmerz) {
            SwitchRow(stringResource(R.string.mittelschmerz), entry.mittelschmerz, viewModel::setMittelschmerz)
        }
        if (settings.showBreastSymptom) {
            SwitchRow(stringResource(R.string.breast_symptom), entry.breastSymptom, viewModel::setBreastSymptom)
        }
    }
}
