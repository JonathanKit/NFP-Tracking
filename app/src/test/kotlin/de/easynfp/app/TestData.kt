package de.easynfp.app

import de.easynfp.data.EvaluationSnapshot
import de.easynfp.data.model.CycleRecord
import de.easynfp.data.model.DayEntry
import de.easynfp.rules.model.Bleeding
import de.easynfp.rules.model.MucusAppearance
import de.easynfp.rules.model.MucusSensation
import de.easynfp.rules.model.Temperature
import java.time.LocalDate

/** The typical 28-day cycle of the rules tests (example of requirement F-41). */
object TestData {
    val START: LocalDate = LocalDate.of(2026, 1, 1)

    private val TEMPS = (
        "36.50 36.45 36.40 36.45 36.35 36.40 36.45 36.40 36.35 36.45 36.40 36.50 36.45 36.40 " +
            "36.55 36.65 36.75 36.80 36.75 36.85 36.80 36.75 36.80 36.85 36.75 36.70 36.60 36.55"
        ).split(" ")
    private val MUCUS = "- - - - - t t ø f S S S+ S+ S+ S+ f t t t t t t t t t t t t".split(" ")

    fun cycleEntries(start: LocalDate): List<DayEntry> = TEMPS.indices.map { index ->
        val (sensation, appearance) = when (MUCUS[index]) {
            "t" -> MucusSensation.DRY to MucusAppearance.NOTHING_SEEN
            "ø" -> MucusSensation.NOTHING_FELT to MucusAppearance.NOTHING_SEEN
            "f" -> MucusSensation.MOIST to MucusAppearance.NOTHING_SEEN
            "S" -> MucusSensation.MOIST to MucusAppearance.THICK
            "S+" -> MucusSensation.WET_SLIPPERY to MucusAppearance.CLEAR_STRETCHY
            else -> null to null
        }
        DayEntry(
            date = start.plusDays(index.toLong()),
            temperature = Temperature.parse(TEMPS[index]),
            mucusSensation = sensation,
            mucusAppearance = appearance,
            bleeding = if (index < 4) Bleeding.MEDIUM else null,
        )
    }

    /** Two complete typical cycles and the current one up to [currentDay]. */
    fun snapshot(currentDay: Int = 10): EvaluationSnapshot {
        val starts = (0 until 3).map { START.plusDays(28L * it) }
        val entries = starts.flatMap(::cycleEntries).filter { it.date < starts[2].plusDays(currentDay.toLong()) }
        return EvaluationSnapshot.compute(
            today = starts[2].plusDays(currentDay - 1L),
            cycles = starts.map { CycleRecord(it) },
            entries = entries,
        )
    }
}
