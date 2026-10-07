package de.easynfp.data.demo

import de.easynfp.data.BackupContent
import de.easynfp.data.model.CycleRecord
import de.easynfp.data.model.DayEntry
import de.easynfp.data.model.Disturbance
import de.easynfp.data.model.Intensity
import de.easynfp.data.model.Intercourse
import de.easynfp.data.model.PredefinedDisturbance
import de.easynfp.rules.model.Bleeding
import de.easynfp.rules.model.MucusAppearance
import de.easynfp.rules.model.MucusSensation
import de.easynfp.rules.model.Temperature
import java.time.LocalDate
import java.time.LocalTime
import kotlin.random.Random

/**
 * Generated, plausible cycles for the demo mode: a clear temperature shift two weeks before the next
 * period, a mucus build-up towards the peak, period bleeding, occasional disturbances (some of them
 * excluded), symptoms and gaps. The same [today] and [seed] always give the same data.
 *
 * The numbers below are made-up example values (days, percentages, temperatures), not rules.
 */
@Suppress("MagicNumber")
object DemoData {
    const val COMPLETED_CYCLES = 13
    const val CURRENT_CYCLE_DAY = 13

    private const val LOW_BASE = 3640
    private const val SHIFT = 38
    private const val NOISE = 7
    private const val LUTEAL_DAYS = 14
    private const val SPIKE = 25

    private val PERIOD = listOf(Bleeding.MEDIUM, Bleeding.HEAVY, Bleeding.MEDIUM, Bleeding.LIGHT, Bleeding.SPOTTING)

    fun generate(today: LocalDate, seed: Int = 7): BackupContent {
        val random = Random(seed)
        val lengths = List(COMPLETED_CYCLES) { 26 + random.nextInt(6) }
        val currentStart = today.minusDays(CURRENT_CYCLE_DAY - 1L)
        val starts = lengths.reversed().runningFold(currentStart) { start, length -> start.minusDays(length.toLong()) }
            .reversed()
        val cycles = starts.map { CycleRecord(it) }
        val days = starts.flatMapIndexed { index, start ->
            val length = lengths.getOrNull(index)
            val shift = random.nextInt(-1, 2)
            // The current cycle is shown a few days before ovulation, so the forecast has something to show.
            val ovulation = if (length != null) length - LUTEAL_DAYS + shift else CURRENT_CYCLE_DAY + 3
            val lastDay = length ?: CURRENT_CYCLE_DAY
            (1..lastDay).mapNotNull { day ->
                val date = start.plusDays(day - 1L)
                cycleDay(date, day, ovulation, isToday = date == today, random = random)
            }
        }
        return BackupContent(days = days, cycles = cycles, disturbances = disturbances())
    }

    private fun cycleDay(date: LocalDate, day: Int, ovulation: Int, isToday: Boolean, random: Random): DayEntry? {
        if (random.chance(2)) return null
        val high = day > ovulation
        var hundredths = LOW_BASE + (if (high) SHIFT else 0) + random.nextInt(-NOISE, NOISE + 1)
        // A disturbed value in the low phase, usually excluded by the user (F-04, F-11).
        val disturbed = !high && day in 3 until ovulation - 6 && random.chance(8)
        if (disturbed) hundredths += SPIKE
        val (sensation, appearance) = if (isToday) null to null else mucus(day, ovulation, random)
        val entry = DayEntry(
            date = date,
            temperature = Temperature(hundredths).takeUnless { random.chance(3) },
            measurementTime = LocalTime.of(6, 10 + random.nextInt(35)),
            temperatureExcluded = disturbed && random.chance(85),
            mucusSensation = sensation,
            mucusAppearance = appearance,
            bleeding = PERIOD.getOrNull(day - 1),
            intercourse = if (random.chance(12)) Intercourse.entries.random(random) else null,
            disturbanceIds = if (disturbed) setOf(disturbanceIdOf(random)) else emptySet(),
        )
        return withSymptoms(entry, day, ovulation, random)
    }

    private fun withSymptoms(entry: DayEntry, day: Int, ovulation: Int, random: Random): DayEntry = entry.copy(
        mittelschmerz = day == ovulation && random.nextBoolean(),
        breastSymptom = day > ovulation + 8 && random.chance(30),
        otherBleeding = if (day == ovulation + 1 && random.chance(15)) Intensity.LIGHT else null,
        cramps = when (day) {
            1 -> Intensity.MEDIUM
            2 -> Intensity.LIGHT
            else -> null
        }.takeIf { random.chance(70) },
        nausea = if (day == 1 && random.chance(25)) Intensity.LIGHT else null,
        diarrhea = day == 1 && random.chance(20),
    )

    private fun Random.chance(percent: Int): Boolean = nextInt(100) < percent

    /** Mucus from dry after the period to the peak on the day of ovulation and back to dry. */
    private fun mucus(day: Int, ovulation: Int, random: Random): Pair<MucusSensation?, MucusAppearance?> {
        val offset = day - ovulation
        return when {
            day <= PERIOD.size -> null to null
            offset < -6 -> MucusSensation.DRY to MucusAppearance.NOTHING_SEEN
            offset < -4 -> MucusSensation.NOTHING_FELT to MucusAppearance.NOTHING_SEEN
            offset == -4 -> MucusSensation.MOIST to MucusAppearance.NOTHING_SEEN
            offset == -3 -> MucusSensation.MOIST to MucusAppearance.WHITISH_CLOUDY
            offset == -2 -> MucusSensation.MOIST to MucusAppearance.THICK
            offset == -1 -> MucusSensation.WET to MucusAppearance.GLASSY
            offset == 0 -> MucusSensation.WET_SLIPPERY to MucusAppearance.CLEAR_STRETCHY
            offset == 1 -> MucusSensation.MOIST to MucusAppearance.LUMPY_YELLOWISH
            offset <= 3 -> MucusSensation.NOTHING_FELT to MucusAppearance.NOTHING_SEEN
            else ->
                (if (random.nextInt(100) < 80) MucusSensation.DRY else MucusSensation.NOTHING_FELT) to
                    MucusAppearance.NOTHING_SEEN
        }
    }

    private val SPIKE_DISTURBANCES = listOf(
        PredefinedDisturbance.ALCOHOL,
        PredefinedDisturbance.SHORT_SLEEP,
        PredefinedDisturbance.LATE_TO_BED,
        PredefinedDisturbance.ILLNESS,
    )

    private fun disturbanceIdOf(random: Random): Long =
        PredefinedDisturbance.entries.indexOf(SPIKE_DISTURBANCES.random(random)) + 1L

    private fun disturbances(): List<Disturbance> = PredefinedDisturbance.entries.mapIndexed { index, predefined ->
        Disturbance(id = index + 1L, predefined = predefined, customName = null, archived = false, sortOrder = index)
    }
}
