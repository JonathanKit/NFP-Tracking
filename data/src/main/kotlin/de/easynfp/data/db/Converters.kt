package de.easynfp.data.db

import androidx.room.TypeConverter
import de.easynfp.data.model.Codes
import de.easynfp.data.model.Intercourse
import de.easynfp.rules.model.Bleeding
import de.easynfp.rules.model.MucusAppearance
import de.easynfp.rules.model.MucusSensation
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Dates are stored as ISO text (sortable), enums as stable codes from [Codes]. */
class Converters {
    @TypeConverter
    fun fromDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromTime(value: LocalTime?): String? = value?.format(TIME_FORMAT)

    @TypeConverter
    fun toTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it, TIME_FORMAT) }

    @TypeConverter
    fun fromSensation(value: MucusSensation?): String? = value?.let(Codes.sensation::code)

    @TypeConverter
    fun toSensation(value: String?): MucusSensation? = value?.let(Codes.sensation::valueOf)

    @TypeConverter
    fun fromAppearance(value: MucusAppearance?): String? = value?.let(Codes.appearance::code)

    @TypeConverter
    fun toAppearance(value: String?): MucusAppearance? = value?.let(Codes.appearance::valueOf)

    @TypeConverter
    fun fromBleeding(value: Bleeding?): String? = value?.let(Codes.bleeding::code)

    @TypeConverter
    fun toBleeding(value: String?): Bleeding? = value?.let(Codes.bleeding::valueOf)

    @TypeConverter
    fun fromIntercourse(value: Intercourse?): String? = value?.let(Codes.intercourse::code)

    @TypeConverter
    fun toIntercourse(value: String?): Intercourse? = value?.let(Codes.intercourse::valueOf)

    companion object {
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
