package de.easynfp.rules.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** F-42: manual decisions of the user for a single cycle. */
public data class CycleOverrides(
    val firstHigherMeasurement: LocalDate? = null,
    val mucusPeak: LocalDate? = null,
) {
    val isEmpty: Boolean get() = firstHigherMeasurement == null && mucusPeak == null
}

/**
 * One cycle: from its first day up to and including [endDate], which is the day before the next
 * period started. [endDate] is null for the current cycle.
 */
public data class CycleData(
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val observations: List<DayObservation>,
    val overrides: CycleOverrides = CycleOverrides(),
) {
    init {
        require(endDate == null || !endDate.isBefore(startDate)) { "Cycle ends before it starts" }
    }

    val isCompleted: Boolean get() = endDate != null

    /** Length in days of a completed cycle, null for the current cycle. */
    val length: Int? get() = endDate?.let { cycleDayOf(it) }

    /** Cycle day (1-based) of [date]. May be out of range for dates outside the cycle. */
    public fun cycleDayOf(date: LocalDate): Int = ChronoUnit.DAYS.between(startDate, date).toInt() + 1

    public fun dateOf(cycleDay: Int): LocalDate = startDate.plusDays((cycleDay - 1).toLong())

    public operator fun contains(date: LocalDate): Boolean =
        !date.isBefore(startDate) && (endDate == null || !date.isAfter(endDate))
}

/** Start of a cycle as stored by the user (F-08), together with per-cycle settings. */
public data class CycleStart(
    val startDate: LocalDate,
    val overrides: CycleOverrides = CycleOverrides(),
)

/** Groups day observations into cycles according to the recorded period starts. */
public object CycleBuilder {
    public fun build(starts: List<CycleStart>, observations: List<DayObservation>): List<CycleData> {
        val sortedStarts = starts.distinctBy { it.startDate }.sortedBy { it.startDate }
        val sortedObservations = observations.sortedBy { it.date }
        return sortedStarts.mapIndexed { index, start ->
            val endDate = sortedStarts.getOrNull(index + 1)?.startDate?.minusDays(1)
            val inCycle = sortedObservations.filter { obs ->
                !obs.date.isBefore(start.startDate) && (endDate == null || !obs.date.isAfter(endDate))
            }
            CycleData(start.startDate, endDate, inCycle, start.overrides)
        }
    }
}
