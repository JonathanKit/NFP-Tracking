package de.easynfp.app.text

import android.content.res.Resources
import de.easynfp.app.R
import de.easynfp.data.model.Disturbance
import de.easynfp.data.model.PredefinedDisturbance
import de.easynfp.rules.model.Bleeding
import de.easynfp.rules.model.MucusAppearance
import de.easynfp.rules.model.MucusCategory
import de.easynfp.rules.model.MucusSensation
import de.easynfp.rules.model.Temperature
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Formatting of values for display. Texts come from resources so the app can be translated. */
object Formats {
    private const val HUNDRED = 100

    /** "36,55" (decimal separator of the current locale). */
    fun temperatureValue(temperature: Temperature, locale: Locale = Locale.getDefault()): String {
        val separator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
        val abs = kotlin.math.abs(temperature.hundredths)
        val sign = if (temperature.hundredths < 0) "-" else ""
        return "$sign${abs / HUNDRED}$separator${(abs % HUNDRED).toString().padStart(2, '0')}"
    }

    /** "36,55 °C" */
    fun temperature(temperature: Temperature, locale: Locale = Locale.getDefault()): String =
        "${temperatureValue(temperature, locale)} °C"

    fun time(time: LocalTime): String = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

    fun dateMedium(date: LocalDate): String = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

    /** Pattern from resources, e.g. "EEEE, d. MMMM". */
    fun dateLong(resources: Resources, date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern(resources.getString(R.string.pattern_date_long)))

    fun dateShort(resources: Resources, date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern(resources.getString(R.string.pattern_date_short)))

    fun monthYear(resources: Resources, date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern(resources.getString(R.string.pattern_month_year)))

    fun mucusCategory(category: MucusCategory): String = category.symbol

    fun sensation(resources: Resources, value: MucusSensation): String = resources.getString(
        when (value) {
            MucusSensation.DRY -> R.string.sensation_dry
            MucusSensation.NOTHING_FELT -> R.string.sensation_nothing
            MucusSensation.MOIST -> R.string.sensation_moist
            MucusSensation.WET_SLIPPERY -> R.string.sensation_wet
        },
    )

    fun appearance(resources: Resources, value: MucusAppearance): String = resources.getString(
        when (value) {
            MucusAppearance.NOTHING_SEEN -> R.string.appearance_nothing
            MucusAppearance.THICK -> R.string.appearance_thick
            MucusAppearance.CLEAR_STRETCHY -> R.string.appearance_clear
        },
    )

    fun bleeding(resources: Resources, value: Bleeding): String = resources.getString(
        when (value) {
            Bleeding.SPOTTING -> R.string.bleeding_spotting
            Bleeding.LIGHT -> R.string.bleeding_light
            Bleeding.MEDIUM -> R.string.bleeding_medium
            Bleeding.HEAVY -> R.string.bleeding_heavy
        },
    )

    fun disturbanceName(resources: Resources, disturbance: Disturbance): String =
        disturbance.customName?.takeIf { it.isNotBlank() }
            ?: disturbance.predefined?.let { predefinedName(resources, it) }
            ?: ""

    fun predefinedName(resources: Resources, value: PredefinedDisturbance): String = resources.getString(
        when (value) {
            PredefinedDisturbance.SHORT_SLEEP -> R.string.disturbance_short_sleep
            PredefinedDisturbance.LATE_TO_BED -> R.string.disturbance_late_to_bed
            PredefinedDisturbance.ALCOHOL -> R.string.disturbance_alcohol
            PredefinedDisturbance.ILLNESS -> R.string.disturbance_illness
            PredefinedDisturbance.STRESS -> R.string.disturbance_stress
            PredefinedDisturbance.TRAVEL -> R.string.disturbance_travel
            PredefinedDisturbance.DIFFERENT_TIME -> R.string.disturbance_different_time
            PredefinedDisturbance.MEDICATION -> R.string.disturbance_medication
            PredefinedDisturbance.OTHER_THERMOMETER -> R.string.disturbance_other_thermometer
        },
    )
}
