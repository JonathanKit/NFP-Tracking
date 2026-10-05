package de.easynfp.app.ui.chart

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import de.easynfp.app.export.PdfExporter
import de.easynfp.app.text.ExplanationFormatter
import de.easynfp.app.text.Formats
import de.easynfp.app.ui.components.SectionCard
import de.easynfp.app.ui.components.SwitchRow
import de.easynfp.app.ui.components.TopLevelScaffold
import de.easynfp.app.ui.components.appViewModelFactory
import de.easynfp.app.ui.navigation.AppNavigator
import de.easynfp.data.EvaluationSnapshot
import de.easynfp.data.settings.AppSettings
import de.easynfp.rules.CycleEvaluation
import kotlinx.coroutines.launch

@Composable
fun CycleSheetScreen(
    navigator: AppNavigator,
    settings: AppSettings,
    viewModel: CycleSheetViewModel = viewModel(
        factory = appViewModelFactory { c, _, handle -> CycleSheetViewModel(c, handle) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    TopLevelScaffold(
        title = stringResource(R.string.nav_chart),
        onSettings = navigator::openSettings,
        snackbarHostState = snackbar,
    ) { padding ->
        val snapshot = state.snapshot
        when {
            state.loading || snapshot == null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            snapshot.evaluations.isEmpty() -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.chart_empty)) }

            else -> {
                val pagerState = rememberPagerState(initialPage = state.initialPage) { snapshot.evaluations.size }
                val scope = rememberCoroutineScope()
                // F-34: swipe between cycles.
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    beyondViewportPageCount = 0,
                ) { page ->
                    CyclePage(
                        snapshot = snapshot,
                        index = page,
                        settings = settings,
                        viewModel = viewModel,
                        navigator = navigator,
                        snackbar = snackbar,
                        onPrevious = { scope.launch { pagerState.animateScrollToPage(page - 1) } },
                        onNext = { scope.launch { pagerState.animateScrollToPage(page + 1) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun CyclePage(
    snapshot: EvaluationSnapshot,
    index: Int,
    settings: AppSettings,
    viewModel: CycleSheetViewModel,
    navigator: AppNavigator,
    snackbar: SnackbarHostState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val evaluation = snapshot.evaluations[index]
    val context = LocalContext.current
    val formatter = remember(context) { ExplanationFormatter(context.resources) }
    val model = remember(evaluation, snapshot.entries, settings.showIntercourse) {
        ChartModel.build(evaluation, snapshot.entries, snapshot.today, settings.showIntercourse)
    }
    val record = snapshot.recordFor(evaluation)
    val scope = rememberCoroutineScope()
    val pdfDone = stringResource(R.string.pdf_exported)
    val pdfFailed = stringResource(R.string.pdf_failed)
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) {
            uri ->
        if (uri != null) {
            scope.launch {
                val ok = PdfExporter.export(context, uri, evaluation, snapshot, index + 1, settings.showIntercourse)
                snackbar.showSnackbar(if (ok) pdfDone else pdfFailed)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CycleHeader(snapshot, index, evaluation, onPrevious, onNext)
        CycleChart(
            model = model,
            onDayClick = navigator::openDay,
            contentDescription = formatter.summary(evaluation),
            modifier = Modifier.padding(end = 8.dp),
        )
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.chart_legend),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ExplanationCard(formatter, evaluation)
            OverrideCard(evaluation, viewModel)
            SectionCard(title = stringResource(R.string.chart_options)) {
                SwitchRow(
                    title = stringResource(R.string.exclude_from_statistics),
                    subtitle = stringResource(R.string.exclude_from_statistics_hint),
                    checked = record?.excludedFromStatistics == true,
                    onCheckedChange = { viewModel.setExcludedFromStatistics(evaluation, it) },
                )
                OutlinedButton(
                    onClick = {
                        pdfLauncher.launch(
                            context.getString(R.string.pdf_file_name, evaluation.cycle.startDate),
                        )
                    },
                ) {
                    Icon(Icons.Outlined.PictureAsPdf, contentDescription = null)
                    Text(stringResource(R.string.export_pdf), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun CycleHeader(
    snapshot: EvaluationSnapshot,
    index: Int,
    evaluation: CycleEvaluation,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val resources = LocalContext.current.resources
    val cycle = evaluation.cycle
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious, enabled = index > 0) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous_cycle))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.cycle_number, index + 1),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            val end = cycle.endDate
            val length = cycle.length
            Text(
                if (end != null && length != null) {
                    stringResource(
                        R.string.cycle_range_completed,
                        Formats.dateShort(resources, cycle.startDate),
                        Formats.dateShort(resources, end),
                    ) + " · " + pluralStringResource(R.plurals.days, length, length)
                } else {
                    stringResource(
                        R.string.cycle_range_current,
                        Formats.dateShort(resources, cycle.startDate),
                        cycle.cycleDayOf(snapshot.today),
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onNext, enabled = index < snapshot.evaluations.lastIndex) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_cycle))
        }
    }
}

/** F-41: which rule applied with which days. */
@Composable
private fun ExplanationCard(formatter: ExplanationFormatter, evaluation: CycleEvaluation) {
    SectionCard(title = stringResource(R.string.explanation_title)) {
        Text(formatter.summary(evaluation), fontWeight = FontWeight.Medium)
        formatter.steps(evaluation).forEach { line ->
            Row {
                Text(
                    line.ruleCode,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 2.dp, end = 8.dp)
                        .heightIn(min = 16.dp),
                )
                Text(line.text, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private enum class OverrideKind { FIRST_HIGHER, PEAK }

/** F-42: manual first higher measurement and mucus peak, marked and reversible. */
@Composable
private fun OverrideCard(evaluation: CycleEvaluation, viewModel: CycleSheetViewModel) {
    var editing by rememberSaveable { mutableStateOf<OverrideKind?>(null) }
    val overrides = evaluation.cycle.overrides
    val cycle = evaluation.cycle
    SectionCard(title = stringResource(R.string.override_title)) {
        Text(stringResource(R.string.override_hint), style = MaterialTheme.typography.bodySmall)
        OverrideRow(
            label = stringResource(R.string.override_first_higher),
            value = overrides.firstHigherMeasurement?.let { cycle.cycleDayOf(it) },
            onEdit = { editing = OverrideKind.FIRST_HIGHER },
            onReset = { viewModel.setFirstHigherOverride(evaluation, null) },
        )
        OverrideRow(
            label = stringResource(R.string.override_peak),
            value = overrides.mucusPeak?.let { cycle.cycleDayOf(it) },
            onEdit = { editing = OverrideKind.PEAK },
            onReset = { viewModel.setPeakOverride(evaluation, null) },
        )
    }
    editing?.let { kind ->
        DayChooserDialog(
            title = stringResource(
                if (kind == OverrideKind.FIRST_HIGHER) R.string.override_first_higher else R.string.override_peak,
            ),
            dayCount = evaluation.dayCount,
            onSelect = { day ->
                val date = cycle.dateOf(day)
                if (kind == OverrideKind.FIRST_HIGHER) {
                    viewModel.setFirstHigherOverride(evaluation, date)
                } else {
                    viewModel.setPeakOverride(evaluation, date)
                }
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun OverrideRow(label: String, value: Int?, onEdit: () -> Unit, onReset: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label)
            Text(
                if (value != null) {
                    stringResource(
                        R.string.override_manual_day,
                        value,
                    )
                } else {
                    stringResource(R.string.override_auto)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (value != null) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        if (value != null) TextButton(onClick = onReset) { Text(stringResource(R.string.override_reset)) }
        TextButton(onClick = onEdit) { Text(stringResource(R.string.change)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayChooserDialog(title: String, dayCount: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            FlowRow(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                (1..dayCount).forEach { day ->
                    FilterChip(selected = false, onClick = { onSelect(day) }, label = { Text(day.toString()) })
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
