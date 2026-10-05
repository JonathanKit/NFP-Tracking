package de.easynfp.rules

import de.easynfp.rules.model.Bleeding
import java.time.LocalDate

/**
 * F-08: the app suggests "Neue Periode beginnt heute" on the first bleeding day after an evaluated
 * temperature high phase. It never starts a cycle on its own.
 */
public object NewCycleSuggestion {
    /**
     * @param current evaluation of the cycle that contains [date], or null if no cycle is recorded yet.
     * @param bleeding the bleeding entered for [date].
     */
    public fun shouldSuggest(current: CycleEvaluation?, date: LocalDate, bleeding: Bleeding?): Boolean {
        if (bleeding == null || !bleeding.isPeriodBleeding) return false
        if (current == null) return true
        val cycle = current.cycle
        if (date !in cycle || date == cycle.startDate) return false
        val completed = current.temperatureCompleted ?: return false
        val day = cycle.cycleDayOf(date)
        if (day <= completed.completionDay) return false
        val earlierBleeding = cycle.observations.any { obs ->
            val obsDay = cycle.cycleDayOf(obs.date)
            obsDay in (completed.completionDay + 1) until day && obs.bleeding?.isPeriodBleeding == true
        }
        return !earlierBleeding
    }
}
