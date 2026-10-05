package de.easynfp.app.ui.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
private const val MONTHS_AFTER_TODAY = 1
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
                    MonthGrid(months[page], current, onDayClick = navigator::openDay)
                    Legend()
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
private fun MonthGrid(month: YearMonth, snapshot: EvaluationSnapshot, onDayClick: (LocalDate) -> Unit) {
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
                        .aspectRatio(1f)
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
    val phase = if (date <= snapshot.today) evaluation?.phaseOn(date) else null
    val entry = snapshot.entries[date]
    val isPeriodStart = snapshot.cycles.any { it.startDate == date }
    val fertile = phase?.phase == Phase.FERTILE
    val background = when (phase?.phase) {
        null -> MaterialTheme.colorScheme.surface
        Phase.FERTILE -> colors.fertile
        else -> colors.infertile
    }
    val phaseText = phase?.let {
        stringResource(
            when (it.phase) {
                Phase.INFERTILE_START -> R.string.phase_infertile_start
                Phase.FERTILE -> R.string.phase_fertile
                Phase.INFERTILE_END -> R.string.phase_infertile_end
            },
        )
    }
    val description = listOfNotNull(Formats.dateMedium(date), phaseText).joinToString(", ")
    val clickable = date <= snapshot.today
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .then(if (fertile) Modifier.hatched(colors.fertileHatch.copy(alpha = 0.35f), spacing = 10f) else Modifier)
            .then(
                if (date == snapshot.today) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                } else {
                    Modifier
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
            fontWeight = if (isPeriodStart) FontWeight.Bold else FontWeight.Normal,
            color = if (fertile) colors.onFertile else MaterialTheme.colorScheme.onSurface,
        )
        // Phase letter (N-05: not only color).
        if (phase != null) {
            Text(
                stringResource(if (fertile) R.string.chart_fertile_short else R.string.chart_infertile_short),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = if (fertile) colors.onFertile else colors.onInfertile,
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
