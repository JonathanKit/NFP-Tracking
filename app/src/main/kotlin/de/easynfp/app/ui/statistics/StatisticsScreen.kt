package de.easynfp.app.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.easynfp.app.R
import de.easynfp.app.text.Formats
import de.easynfp.app.ui.components.SectionCard
import de.easynfp.app.ui.components.TopLevelScaffold
import de.easynfp.app.ui.components.appViewModelFactory
import de.easynfp.app.ui.navigation.AppNavigator
import de.easynfp.rules.statistics.NumberSummary
import java.util.Locale

@Composable
fun StatisticsScreen(
    navigator: AppNavigator,
    viewModel: StatisticsViewModel = viewModel(factory = appViewModelFactory { c, _, _ -> StatisticsViewModel(c) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TopLevelScaffold(title = stringResource(R.string.nav_statistics), onSettings = navigator::openSettings) { padding ->
        if (state.loading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@TopLevelScaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Filter in one row above the content.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatisticsRange.entries.forEach { range ->
                    FilterChip(
                        selected = state.range == range,
                        onClick = { viewModel.setRange(range) },
                        label = {
                            Text(
                                when (range) {
                                    StatisticsRange.LAST_6 -> stringResource(R.string.range_last_6)
                                    StatisticsRange.LAST_12 -> stringResource(R.string.range_last_12)
                                    StatisticsRange.ALL -> stringResource(R.string.range_all)
                                },
                            )
                        },
                    )
                }
            }
            val summary = state.summary
            if (summary == null || summary.cycleCount == 0) {
                SectionCard { Text(stringResource(R.string.statistics_empty)) }
            } else {
                Text(
                    pluralStringResource(R.plurals.statistics_basis, summary.cycleCount, summary.cycleCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SectionCard {
                    SummaryRow(stringResource(R.string.stat_cycle_length), summary.cycleLength)
                    HorizontalDivider()
                    SummaryRow(
                        stringResource(R.string.stat_first_higher_day),
                        summary.firstHigherDay,
                        earliestLatest = true,
                    )
                    HorizontalDivider()
                    SummaryRow(stringResource(R.string.stat_high_phase), summary.highPhaseLength)
                    HorizontalDivider()
                    SummaryRow(stringResource(R.string.stat_fertile_phase), summary.fertilePhaseLength)
                    HorizontalDivider()
                    SummaryRow(stringResource(R.string.stat_infertile_start), summary.infertileStartDays)
                    HorizontalDivider()
                    SummaryRow(stringResource(R.string.stat_infertile_end), summary.infertileEndDays)
                }
            }

            val included = state.rows.filterNot { it.excluded }
            if (included.size >= 2) {
                // F-44: two separate charts, never one chart with two axes.
                SectionCard(title = stringResource(R.string.chart_cycle_lengths)) {
                    BarChart(
                        labels = included.map { it.number.toString() },
                        values = included.map { it.figures?.length },
                        description = stringResource(R.string.chart_cycle_lengths),
                    )
                }
                SectionCard(title = stringResource(R.string.chart_first_higher_days)) {
                    BarChart(
                        labels = included.map { it.number.toString() },
                        values = included.map { it.figures?.firstHigherDay },
                        description = stringResource(R.string.chart_first_higher_days),
                    )
                }
            }

            state.forecast?.let { forecast ->
                val resources = LocalContext.current.resources
                SectionCard(title = stringResource(R.string.forecast_title)) {
                    Text(
                        stringResource(
                            R.string.forecast_range,
                            Formats.dateShort(resources, forecast.earliest),
                            Formats.dateShort(resources, forecast.latest),
                        ),
                    )
                    Text(
                        stringResource(R.string.forecast_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (state.rows.isNotEmpty()) {
                CycleTable(state.rows, onExcludedChange = viewModel::setExcluded, onOpen = navigator::openChart)
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, summary: NumberSummary?, earliestLatest: Boolean = false) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        if (summary == null) {
            Text(stringResource(R.string.stat_no_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val mean = String.format(Locale.getDefault(), "%.1f", summary.mean)
            val sd = String.format(Locale.getDefault(), "%.1f", summary.standardDeviation)
            Text(
                if (earliestLatest) {
                    stringResource(R.string.stat_values_earliest, mean, summary.min, summary.max, sd)
                } else {
                    stringResource(R.string.stat_values, mean, summary.min, summary.max, sd)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** Single-series bar chart: thin bars with rounded tops on a shared baseline, recessive grid. */
@Composable
private fun BarChart(labels: List<String>, values: List<Int?>, description: String) {
    val barColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val present = values.filterNotNull()
    if (present.isEmpty()) {
        Text(stringResource(R.string.stat_no_data))
        return
    }
    val max = present.max()
    val min = present.min()
    val summary = labels.zip(values).joinToString(", ") { (label, value) -> "$label: ${value ?: "–"}" }
    Column(Modifier.semantics { contentDescription = "$description. $summary" }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.chart_min_max, min, max),
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
            )
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(top = 8.dp),
        ) {
            val slot = size.width / values.size
            val barWidth = (slot * BAR_FRACTION).coerceAtMost(BAR_MAX_WIDTH.dp.toPx())
            val top = max.toFloat()
            // Baseline at zero so bar lengths stay honest.
            drawLine(gridColor, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            drawLine(gridColor, Offset(0f, 0f), Offset(size.width, 0f), 0.5.dp.toPx())
            values.forEachIndexed { index, value ->
                if (value == null) return@forEachIndexed
                val height = size.height * value / top
                val left = index * slot + (slot - barWidth) / 2
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, size.height - height),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                )
                // Square off the bottom corners so bars sit on the baseline.
                drawRect(
                    color = barColor,
                    topLeft = Offset(left, size.height - minOf(height, 4.dp.toPx())),
                    size = Size(barWidth, minOf(height, 4.dp.toPx())),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            labels.forEach { label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        Text(
            stringResource(R.string.chart_x_axis_cycles),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

/** Table view of all cycles, with exclusion from statistics (F-46). */
@Composable
private fun CycleTable(
    rows: List<CycleRow>,
    onExcludedChange: (CycleRow, Boolean) -> Unit,
    onOpen: (java.time.LocalDate) -> Unit,
) {
    val resources = LocalContext.current.resources
    SectionCard(title = stringResource(R.string.cycle_table_title)) {
        Text(
            stringResource(R.string.cycle_table_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        rows.asReversed().forEach { row ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    TextButton(
                        onClick = { onOpen(row.evaluation.cycle.startDate) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) {
                        Text(
                            stringResource(
                                R.string.cycle_table_row,
                                row.number,
                                Formats.dateShort(resources, row.evaluation.cycle.startDate),
                            ),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    val figures = row.figures
                    Text(
                        if (figures != null) {
                            stringResource(
                                R.string.cycle_table_details,
                                figures.length,
                                figures.firstHigherDay?.toString() ?: "–",
                            )
                        } else {
                            ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (row.excluded) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    stringResource(R.string.cycle_table_include),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Switch(checked = !row.excluded, onCheckedChange = { onExcludedChange(row, !it) })
            }
        }
    }
}

private const val BAR_FRACTION = 0.6f
private const val BAR_MAX_WIDTH = 24
