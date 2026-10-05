package de.easynfp.app.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.easynfp.app.R
import de.easynfp.app.text.ExplanationFormatter
import de.easynfp.app.text.Formats
import de.easynfp.app.ui.components.DatePickerDialogFor
import de.easynfp.app.ui.components.PhaseBadge
import de.easynfp.app.ui.components.SectionCard
import de.easynfp.app.ui.components.TopLevelScaffold
import de.easynfp.app.ui.components.appViewModelFactory
import de.easynfp.app.ui.navigation.AppNavigator
import de.easynfp.data.EvaluationSnapshot
import de.easynfp.rules.CycleEvaluation
import de.easynfp.rules.Phase
import de.easynfp.rules.statistics.PeriodForecast

@Composable
fun TodayScreen(
    navigator: AppNavigator,
    viewModel: TodayViewModel = viewModel(factory = appViewModelFactory { c, _, _ -> TodayViewModel(c) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TopLevelScaffold(title = stringResource(R.string.nav_today), onSettings = navigator::openSettings) { padding ->
        val snapshot = state.snapshot
        when {
            state.loading || snapshot == null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val current = snapshot.current
                if (current == null) {
                    WelcomeCard(snapshot, onPeriodStart = viewModel::setPeriodStart)
                } else {
                    StatusCard(snapshot, current, onDetails = { navigator.openChart() })
                    EntryActions(snapshot, navigator)
                    if (state.todos.isNotEmpty()) TodoCard(state.todos, navigator)
                    state.forecast?.let { ForecastCard(it) }
                }
                if (!state.settings.measuringHintsDismissed) {
                    HintsCard(onMore = { navigator.openInfo("hints") }, onDismiss = viewModel::dismissHints)
                }
            }
        }
    }
}

@Composable
private fun WelcomeCard(snapshot: EvaluationSnapshot, onPeriodStart: (java.time.LocalDate) -> Unit) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    SectionCard(title = stringResource(R.string.welcome_title)) {
        Text(stringResource(R.string.welcome_text))
        Text(
            stringResource(R.string.disclaimer_short),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.welcome_pick_period_start))
        }
    }
    if (showPicker) {
        DatePickerDialogFor(
            initial = snapshot.today,
            maxDate = snapshot.today,
            onConfirm = {
                showPicker = false
                onPeriodStart(it)
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun StatusCard(snapshot: EvaluationSnapshot, current: CycleEvaluation, onDetails: () -> Unit) {
    val context = LocalContext.current
    val formatter = remember(context) { ExplanationFormatter(context.resources) }
    val cycleDay = current.cycle.cycleDayOf(snapshot.today)
    val phase = current.phaseOf(cycleDay)
    SectionCard(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Text(
            stringResource(R.string.cycle_day_large, cycleDay),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
        )
        PhaseBadge(phase.phase, phase.infertileFromEvening)
        Text(statusSentence(current, cycleDay, phase.phase, phase.infertileFromEvening))
        Text(
            formatter.summary(current),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onDetails, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            Text(stringResource(R.string.today_show_chart))
        }
    }
}

@Composable
private fun statusSentence(current: CycleEvaluation, cycleDay: Int, phase: Phase, fromEvening: Boolean): String {
    val lastStartDay = current.startPhase.lastInfertileDay ?: cycleDay
    val infertileFrom = current.infertileFromEveningOfDay ?: cycleDay
    return when {
        fromEvening -> stringResource(R.string.status_fertile_until_evening)
        phase == Phase.INFERTILE_START -> stringResource(R.string.status_infertile_start, lastStartDay)
        phase == Phase.INFERTILE_END -> stringResource(R.string.status_infertile_end, infertileFrom)
        else -> stringResource(R.string.status_fertile)
    }
}

@Composable
private fun EntryActions(snapshot: EvaluationSnapshot, navigator: AppNavigator) {
    val todayEntry = snapshot.entries[snapshot.today]
    val temperature = todayEntry?.temperature
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        if (temperature == null) {
            Button(onClick = navigator::openQuickEntry, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Thermostat, contentDescription = null, modifier = Modifier.size(18.dp))
                Box(Modifier.width(8.dp))
                Text(stringResource(R.string.action_enter_temperature))
            }
        } else {
            FilledTonalButton(onClick = navigator::openQuickEntry, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Box(Modifier.width(8.dp))
                Text(Formats.temperature(temperature))
            }
        }
        OutlinedButton(onClick = { navigator.openDay(snapshot.today) }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.EditCalendar, contentDescription = null, modifier = Modifier.size(18.dp))
            Box(Modifier.width(8.dp))
            Text(stringResource(R.string.action_edit_today))
        }
    }
}

@Composable
private fun TodoCard(todos: List<Todo>, navigator: AppNavigator) {
    val context = LocalContext.current
    SectionCard(title = stringResource(R.string.todo_title)) {
        todos.forEach { todo ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp),
                )
                Box(Modifier.width(12.dp))
                val todoLine: Pair<String, () -> Unit> = when (todo) {
                    Todo.TemperatureMissing ->
                        stringResource(R.string.todo_temperature_missing) to navigator::openQuickEntry
                    is Todo.MucusMissing ->
                        stringResource(R.string.todo_mucus_missing, Formats.dateShort(context.resources, todo.date)) to
                            { navigator.openDay(todo.date) }
                    is Todo.MucusGap -> stringResource(
                        R.string.todo_mucus_gap,
                        todo.days.joinToString(", ") { Formats.dateShort(context.resources, it) },
                    ) to { navigator.openDay(todo.days.first()) }
                    is Todo.NewCycleSuggested ->
                        stringResource(R.string.todo_new_cycle) to { navigator.openDay(todo.date) }
                }
                val (text, action) = todoLine
                Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = action) { Text(stringResource(R.string.todo_open)) }
            }
        }
    }
}

@Composable
private fun ForecastCard(forecast: PeriodForecast) {
    val context = LocalContext.current
    SectionCard(title = stringResource(R.string.forecast_title)) {
        Text(
            stringResource(
                R.string.forecast_range,
                Formats.dateShort(context.resources, forecast.earliest),
                Formats.dateShort(context.resources, forecast.latest),
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            pluralStringResource(R.plurals.forecast_basis, forecast.basedOnCycles, forecast.basedOnCycles) + " " +
                stringResource(R.string.forecast_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HintsCard(onMore: () -> Unit, onDismiss: () -> Unit) {
    SectionCard(title = stringResource(R.string.hints_title)) {
        Text(stringResource(R.string.hints_short), style = MaterialTheme.typography.bodyMedium)
        Row {
            TextButton(onClick = onMore) { Text(stringResource(R.string.hints_more)) }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.hints_dismiss)) }
        }
    }
}
