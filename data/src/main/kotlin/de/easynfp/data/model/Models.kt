package de.easynfp.data.model

import de.easynfp.rules.model.Bleeding
import de.easynfp.rules.model.CycleOverrides
import de.easynfp.rules.model.CycleStart
import de.easynfp.rules.model.DayObservation
import de.easynfp.rules.model.MucusAppearance
import de.easynfp.rules.model.MucusCategory
import de.easynfp.rules.model.MucusSensation
import de.easynfp.rules.model.Temperature
import java.time.LocalDate
import java.time.LocalTime

/** F-14 */
enum class Intercourse { PROTECTED, UNPROTECTED }

/** F-16: strength of a symptom or of a bleeding outside the period. "Not present" is stored as null. */
enum class Intensity { LIGHT, MEDIUM, HEAVY }

/** F-10: disturbances that exist from the first start. [code] is stored, the label comes from resources. */
enum class PredefinedDisturbance(val code: String) {
    SHORT_SLEEP("SHORT_SLEEP"),
    LATE_TO_BED("LATE_TO_BED"),
    ALCOHOL("ALCOHOL"),
    ILLNESS("ILLNESS"),
    STRESS("STRESS"),
    TRAVEL("TRAVEL"),
    DIFFERENT_TIME("DIFFERENT_TIME"),
    MEDICATION("MEDICATION"),
    OTHER_THERMOMETER("OTHER_THERMOMETER"),
}

/** Everything recorded for one day (F-01 to F-16). */
data class DayEntry(
    val date: LocalDate,
    val temperature: Temperature? = null,
    val measurementTime: LocalTime? = null,
    val temperatureExcluded: Boolean = false,
    val mucusSensation: MucusSensation? = null,
    val mucusAppearance: MucusAppearance? = null,
    /** F-07: period bleeding (shown as "Periode"). */
    val bleeding: Bleeding? = null,
    val intercourse: Intercourse? = null,
    val mittelschmerz: Boolean = false,
    val breastSymptom: Boolean = false,
    /** F-16: bleeding outside the period. Not used by the rules. */
    val otherBleeding: Intensity? = null,
    /** F-16 */
    val cramps: Intensity? = null,
    val nausea: Intensity? = null,
    val diarrhea: Boolean = false,
    val vomiting: Boolean = false,
    val note: String? = null,
    val disturbanceIds: Set<Long> = emptySet(),
) {
    val mucusCategory: MucusCategory? get() = MucusCategory.derive(mucusSensation, mucusAppearance)

    val hasMucusEntry: Boolean get() = mucusSensation != null || mucusAppearance != null

    /** F-16 */
    val hasSymptoms: Boolean
        get() = otherBleeding != null || cramps != null || nausea != null || diarrhea || vomiting

    /** True if nothing is recorded, so the entry can be deleted instead of stored. */
    val isEmpty: Boolean
        get() = temperature == null && !hasMucusEntry && bleeding == null && intercourse == null &&
            !mittelschmerz && !breastSymptom && !hasSymptoms && note.isNullOrBlank() && disturbanceIds.isEmpty()

    fun toObservation(): DayObservation = DayObservation(
        date = date,
        temperature = temperature,
        temperatureExcluded = temperatureExcluded,
        mucusSensation = mucusSensation,
        mucusAppearance = mucusAppearance,
        bleeding = bleeding,
    )
}

/** A recorded period start with its per-cycle settings (F-08, F-42, F-46). */
data class CycleRecord(
    val startDate: LocalDate,
    val excludedFromStatistics: Boolean = false,
    val overrides: CycleOverrides = CycleOverrides(),
) {
    fun toCycleStart(): CycleStart = CycleStart(startDate, overrides)
}

/** F-09: a disturbance the user can assign to days. */
data class Disturbance(
    val id: Long,
    val predefined: PredefinedDisturbance?,
    /** Name given by the user; for predefined disturbances only when renamed. */
    val customName: String?,
    val archived: Boolean,
    val sortOrder: Int,
)
