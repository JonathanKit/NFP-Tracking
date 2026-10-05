package de.easynfp.rules.statistics

import java.time.LocalDate

/**
 * F-45: estimated start of the next period as a date range.
 * This is purely a planning aid from past cycle lengths and never a statement about fertility.
 */
public data class PeriodForecast(
    val earliest: LocalDate,
    val latest: LocalDate,
    /** Number of past cycles the estimate is based on. */
    val basedOnCycles: Int,
    val shortestCycle: Int,
    val longestCycle: Int,
)

public object PeriodForecaster {
    public const val MIN_CYCLES: Int = 3
    public const val MAX_CYCLES: Int = 6

    /**
     * @param completedLengths lengths of completed cycles in chronological order.
     * @param currentCycleStart first day of the current cycle.
     */
    public fun forecast(completedLengths: List<Int>, currentCycleStart: LocalDate): PeriodForecast? {
        val recent = completedLengths.takeLast(MAX_CYCLES)
        if (recent.size < MIN_CYCLES) return null
        val shortest = recent.min()
        val longest = recent.max()
        return PeriodForecast(
            earliest = currentCycleStart.plusDays(shortest.toLong()),
            latest = currentCycleStart.plusDays(longest.toLong()),
            basedOnCycles = recent.size,
            shortestCycle = shortest,
            longestCycle = longest,
        )
    }
}
