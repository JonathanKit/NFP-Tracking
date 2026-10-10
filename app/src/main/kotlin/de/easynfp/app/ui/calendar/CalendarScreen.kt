package de.easynfp.app.ui.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import de.easynfp.app.R
import de.easynfp.app.text.Formats
import de.easynfp.app.ui.components.TopLevelScaffold
import de.easynfp.app.ui.components.appViewModelFactory
import de.easynfp.app.ui.components.hatched
import de.easynfp.app.ui.navigation.AppNavigator
import de.easynfp.app.ui.theme.LocalPhaseColors
import de.easynfp.data.DataContainer
import de.easynfp.data.EvaluationSnapshot
import de.easynfp.rules.Phase
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CalendarViewModel(container: DataContainer) : ViewModel() {
    val snapshot: StateFlow<EvaluationSnapshot?> = container.evaluations.snapshot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

private const val MONTHS_BEFORE_FIRST_ENTRY = 1

// F-47: the forecast reaches about four months ahead.
private const val MONTHS_AFTER_TODAY = 3
private const val DEFAULT_MONTHS_BACK = 12L

/** F-35: month calendar with bleeding, phase and entered days; tap opens the day. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    navigator: AppNavigator,
    viewModel: CalendarViewModel = viewModel(factory = appViewModelFactory { c, _, _ -> CalendarViewModel(c) }),
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    TopLevelScaffold(title = stringResource(R.string.nav_calendar), onSettings = navigator::openSettings) { padding ->
        val current = snapshot
        if (current == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@TopLevelScaffold
        }
        val todayMonth = YearMonth.from(current.today)
        val firstDate = listOfNotNull(
            current.cycles.firstOrNull()?.startDate,
            current.entries.keys.minOrNull(),
        ).minOrNull()
        val firstMonth = (firstDate?.let(YearMonth::from) ?: todayMonth.minusMonths(DEFAULT_MONTHS_BACK))
            .minusMonths(MONTHS_BEFORE_FIRST_ENTRY.toLong())
        val lastMonth = todayMonth.plusMonths(MONTHS_AFTER_TODAY.toLong())
        val months = remember(firstMonth, lastMonth) {
            generateSequence(firstMonth) { it.plusMonths(1) }.takeWhile { it <= lastMonth }.toList()
        }
        val pagerState = rememberPagerState(initialPage = months.indexOf(todayMonth).coerceAtLeast(0)) { months.size }
        val scope = rememberCoroutineScope()
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                BoxWithConstraints {
                    // Rows share the free height, so the month fills the screen (always room for six weeks).
                    val rowHeight = ((maxHeight - CALENDAR_CHROME_HEIGHT.dp) / MAX_WEEKS).coerceAtLeast(
                        MIN_ROW_HEIGHT.dp,
                    )
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp),
                    ) {
                        MonthHeader(
                            month = months[page],
                            onPrevious = { scope.launch { pagerState.animateScrollToPage(page - 1) } },
                            onNext = { scope.launch { pagerState.animateScrollToPage(page + 1) } },
                            hasPrevious = page > 0,
                            hasNext = page < months.lastIndex,
                        )
                        MonthGrid(months[page], current, rowHeight, onDayClick = navigator::openDay)
                        Legend()
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    hasPrevious: Boolean,
    hasNext: Boolean,
) {
    val resources = LocalContext.current.resources
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious, enabled = hasPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous_month))
        }
        Text(
            Formats.monthYear(resources, month.atDay(1)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(onClick = onNext, enabled = hasNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_month))
        }
    }
}

@Composable
private fun MonthGrid(month: YearMonth, snapshot: EvaluationSnapshot, rowHeight: Dp, onDayClick: (LocalDate) -> Unit) {
    val locale = Locale.getDefault()
    val firstDayOfWeek = DayOfWeek.MONDAY
    Row(Modifier.fillMaxWidth()) {
        (0 until DAYS_PER_WEEK).forEach { offset ->
            Text(
                firstDayOfWeek.plus(offset.toLong()).getDisplayName(TextStyle.SHORT, locale),
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    val leading = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + DAYS_PER_WEEK) % DAYS_PER_WEEK
    val cells = List(leading) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    cells.chunked(DAYS_PER_WEEK).forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            week.forEach { date ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(rowHeight)
                        .padding(2.dp),
                ) {
                    if (date != null) DayCell(date, snapshot, onDayClick)
                }
            }
            repeat(DAYS_PER_WEEK - week.size) { Box(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, snapshot: EvaluationSnapshot, onDayClick: (LocalDate) -> Unit) {
    val colors = LocalPhaseColors.current
    val evaluation = snapshot.evaluationFor(date)
    // F-47: future days show the forecast, clearly marked as such.
    val forecastDay = if (date > snapshot.today) snapshot.forecast?.dayOn(date) else null
    val forecast = forecastDay != null
    val phase = if (date <= snapshot.today) evaluation?.phaseOn(date)?.phase else forecastDay?.phase
    val entry = snapshot.entries[date]
    val isPeriodStart = snapshot.cycles.any { it.startDate == date }
    val fertile = phase == Phase.FERTILE
    val alpha = if (forecast) FORECAST_ALPHA else 1f
    val background = when (phase) {
        null -> MaterialTheme.colorScheme.surface
        Phase.FERTILE -> colors.fertile.copy(alpha = alpha)
        else -> colors.infertile.copy(alpha = alpha)
    }
    val phaseText = phase?.let {
        stringResource(
            when (it) {
                Phase.INFERTILE_START -> R.string.phase_infertile_start
                Phase.FERTILE -> R.string.phase_fertile
                Phase.INFERTILE_END -> R.string.phase_infertile_end
            },
        )
    }
    val description = listOfNotNull(
        Formats.dateMedium(date),
        phaseText,
        if (forecast) stringResource(R.string.phase_forecast_suffix) else null,
        if (forecastDay?.expectedPeriod == true) stringResource(R.string.forecast_expected_period) else null,
    ).joinToString(", ")
    val clickable = date <= snapshot.today
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(background)
            .then(
                if (fertile) {
                    Modifier.hatched(colors.fertileHatch.copy(alpha = 0.35f * alpha), spacing = 10f)
                } else {
                    Modifier
                },
            )
            .then(
                when {
                    date == snapshot.today -> Modifier.border(3.dp, colors.todayOutline, shape)
                    forecastDay?.expectedPeriod == true -> Modifier.border(
                        2.dp,
                        colors.bleeding.copy(alpha = 0.7f),
                        shape,
                    )
                    else -> Modifier
                },
            )
            .then(if (clickable) Modifier.clickable { onDayClick(date) } else Modifier)
            .semantics { contentDescription = description },
    ) {
        Text(
            date.dayOfMonth.toString(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isPeriodStart || date == snapshot.today) FontWeight.Bold else FontWeight.Normal,
            color = (if (fertile) colors.onFertile else MaterialTheme.colorScheme.onSurface)
                .copy(alpha = if (forecast) FORECAST_TEXT_ALPHA else 1f),
        )
        // Phase letter (N-05: not only color); "?" marks the forecast.
        if (phase != null) {
            Text(
                stringResource(if (fertile) R.string.chart_fertile_short else R.string.chart_infertile_short) +
                    if (forecast) "?" else "",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = (if (fertile) colors.onFertile else colors.onInfertile)
                    .copy(alpha = if (forecast) FORECAST_TEXT_ALPHA else 1f),
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            entry?.bleeding?.let {
                Box(
                    Modifier
                        .size(if (it.isPeriodBleeding) 8.dp else 5.dp)
                        .clip(CircleShape)
                        .background(colors.bleeding),
                )
            }
            if (entry != null) {
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
        }
    }
}

@Composable
private fun Legend() {
    Text(
        stringResource(R.string.calendar_legend),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 12.dp),
    )
}

private const val DAYS_PER_WEEK = 7

private const val MAX_WEEKS = 6

// Month header, weekday row and legend.
private const val CALENDAR_CHROME_HEIGHT = 190
private const val MIN_ROW_HEIGHT = 52
private const val FORECAST_ALPHA = 0.45f
private const val FORECAST_TEXT_ALPHA = 0.7f
