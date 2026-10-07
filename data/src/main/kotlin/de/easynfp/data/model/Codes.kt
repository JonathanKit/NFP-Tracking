package de.easynfp.data.model

import de.easynfp.rules.model.Bleeding
import de.easynfp.rules.model.MucusAppearance
import de.easynfp.rules.model.MucusSensation

/**
 * Bidirectional mapping between an enum and the stable code stored in the database and in backups.
 * Codes must never change once released; enum constants may be renamed freely.
 */
class CodeMap<E : Enum<E>>(private val codes: Map<E, String>) {
    private val reverse = codes.entries.associate { (key, value) -> value to key }

    init {
        require(reverse.size == codes.size) { "Duplicate codes" }
    }

    fun code(value: E): String = codes.getValue(value)

    fun valueOf(code: String): E? = reverse[code]
}

/** All stored enum codes in one place (see docs/data-model.md). */
object Codes {
    val sensation = CodeMap(
        mapOf(
            MucusSensation.DRY to "DRY",
            MucusSensation.NOTHING_FELT to "NOTHING_FELT",
            MucusSensation.MOIST to "MOIST",
            MucusSensation.WET_SLIPPERY to "WET_SLIPPERY",
            MucusSensation.WET to "WET",
        ),
    )
    val appearance = CodeMap(
        mapOf(
            MucusAppearance.NOTHING_SEEN to "NOTHING_SEEN",
            MucusAppearance.THICK to "THICK",
            MucusAppearance.CLEAR_STRETCHY to "CLEAR_STRETCHY",
            MucusAppearance.WHITISH_CLOUDY to "WHITISH_CLOUDY",
            MucusAppearance.LUMPY_YELLOWISH to "LUMPY_YELLOWISH",
            MucusAppearance.GLASSY to "GLASSY",
            MucusAppearance.LIQUID to "LIQUID",
        ),
    )
    val intensity = CodeMap(
        mapOf(
            Intensity.LIGHT to "LIGHT",
            Intensity.MEDIUM to "MEDIUM",
            Intensity.HEAVY to "HEAVY",
        ),
    )
    val bleeding = CodeMap(
        mapOf(
            Bleeding.SPOTTING to "SPOTTING",
            Bleeding.LIGHT to "LIGHT",
            Bleeding.MEDIUM to "MEDIUM",
            Bleeding.HEAVY to "HEAVY",
        ),
    )
    val intercourse = CodeMap(
        mapOf(
            Intercourse.PROTECTED to "PROTECTED",
            Intercourse.UNPROTECTED to "UNPROTECTED",
        ),
    )
    val predefinedDisturbance = CodeMap(PredefinedDisturbance.entries.associateWith { it.code })
}
