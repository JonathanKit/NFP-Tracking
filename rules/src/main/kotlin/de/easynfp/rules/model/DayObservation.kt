package de.easynfp.rules.model

import java.time.LocalDate

/**
 * Everything the rule engine needs to know about a single day.
 * The data layer maps its stored day entries to this type.
 */
public data class DayObservation(
    val date: LocalDate,
    val temperature: Temperature? = null,
    /** F-04: excluded values stay visible but are ignored by the evaluation (R-02). */
    val temperatureExcluded: Boolean = false,
    val mucusSensation: MucusSensation? = null,
    val mucusAppearance: MucusAppearance? = null,
    val bleeding: Bleeding? = null,
) {
    /** R-11 */
    val mucusCategory: MucusCategory? get() = MucusCategory.derive(mucusSensation, mucusAppearance)

    /** The temperature as it enters the evaluation, or null if missing or excluded (R-01, R-02). */
    val evaluationTemperature: Temperature?
        get() = if (temperatureExcluded) null else temperature?.roundedForEvaluation()
}
