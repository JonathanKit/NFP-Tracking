package de.easynfp.rules

import de.easynfp.rules.model.Bleeding
import de.easynfp.rules.model.CycleData
import de.easynfp.rules.model.CycleOverrides
import de.easynfp.rules.model.DayObservation
import de.easynfp.rules.model.MucusAppearance
import de.easynfp.rules.model.MucusCategory
import de.easynfp.rules.model.MucusSensation
import de.easynfp.rules.model.Temperature
import de.easynfp.rules.temperature.ValidTemperature
import java.time.LocalDate

/**
 * Compact notation for test cycles, one whitespace separated token per cycle day:
 *
 * - temperatures: `36.45`, `(36.90)` = excluded (F-04), `-` = no measurement
 * - mucus: `t`, `ø` (or `o`), `f`, `S`, `S+`, `-` = no entry
 * - bleeding: `x` = spotting, `l` = light, `m` = medium, `h` = heavy, `-` = none
 */
internal object TestCycles {
    val START: LocalDate = LocalDate.of(2026, 1, 1)

    fun tokens(text: String): List<String> = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

    fun cycle(
        temps: String = "",
        mucus: String = "",
        bleeding: String = "",
        start: LocalDate = START,
        length: Int? = null,
        overrides: CycleOverrides = CycleOverrides(),
    ): CycleData = CycleData(
        startDate = start,
        endDate = length?.let { start.plusDays(it - 1L) },
        observations = observations(temps, mucus, bleeding, start),
        overrides = overrides,
    )

    fun observations(temps: String, mucus: String, bleeding: String, start: LocalDate): List<DayObservation> {
        val t = tokens(temps)
        val m = tokens(mucus)
        val b = tokens(bleeding)
        val days = maxOf(t.size, m.size, b.size)
        return (0 until days).map { index ->
            val tempToken = t.getOrNull(index) ?: "-"
            val excluded = tempToken.startsWith("(")
            val (sensation, appearance) = mucusObservation(m.getOrNull(index) ?: "-")
            DayObservation(
                date = start.plusDays(index.toLong()),
                temperature = Temperature.parse(tempToken.removePrefix("(").removeSuffix(")")),
                temperatureExcluded = excluded,
                mucusSensation = sensation,
                mucusAppearance = appearance,
                bleeding = bleedingOf(b.getOrNull(index) ?: "-"),
            )
        }
    }

    /** Valid (rounded) temperatures by cycle day, as the engine passes them to the temperature evaluator. */
    fun values(temps: String): List<ValidTemperature> = observations(temps, "", "", START)
        .mapIndexedNotNull { index, obs -> obs.evaluationTemperature?.let { ValidTemperature(index + 1, it) } }

    fun categories(mucus: String): Map<Int, MucusCategory> = tokens(mucus)
        .mapIndexedNotNull { index, token -> categoryOf(token)?.let { index + 1 to it } }
        .toMap()

    fun categoryOf(token: String): MucusCategory? = when (token) {
        "t" -> MucusCategory.DRY
        "ø", "o" -> MucusCategory.NOTHING
        "f" -> MucusCategory.MOIST
        "S" -> MucusCategory.S
        "S+" -> MucusCategory.S_PLUS
        "-" -> null
        else -> error("Unknown mucus token $token")
    }

    private fun mucusObservation(token: String): Pair<MucusSensation?, MucusAppearance?> = when (token) {
        "t" -> MucusSensation.DRY to MucusAppearance.NOTHING_SEEN
        "ø", "o" -> MucusSensation.NOTHING_FELT to MucusAppearance.NOTHING_SEEN
        "f" -> MucusSensation.MOIST to MucusAppearance.NOTHING_SEEN
        "S" -> MucusSensation.MOIST to MucusAppearance.THICK
        "S+" -> MucusSensation.WET_SLIPPERY to MucusAppearance.CLEAR_STRETCHY
        "-" -> null to null
        else -> error("Unknown mucus token $token")
    }

    private fun bleedingOf(token: String): Bleeding? = when (token) {
        "x" -> Bleeding.SPOTTING
        "l" -> Bleeding.LIGHT
        "m" -> Bleeding.MEDIUM
        "h" -> Bleeding.HEAVY
        "-" -> null
        else -> error("Unknown bleeding token $token")
    }

    fun t(text: String): Temperature = requireNotNull(Temperature.parse(text))

    /**
     * A typical cycle of 28 days: first higher measurement on day 15, temperature evaluated on day 17
     * (regular rule), mucus peak on day 15, infertile from the evening of day 18. This is the example
     * sentence of requirement F-41.
     */
    const val TYPICAL_TEMPS =
        "36.50 36.45 36.40 36.45 36.35 36.40 36.45 36.40 36.35 36.45 36.40 36.50 36.45 36.40 " +
            "36.55 36.65 36.75 36.80 36.75 36.85 36.80 36.75 36.80 36.85 36.75 36.70 36.60 36.55"
    const val TYPICAL_MUCUS =
        "- - - - - t t ø f S S S+ S+ S+ S+ f t t t t t t t t t t t t"
    const val TYPICAL_BLEEDING = "h m m l x"
}
