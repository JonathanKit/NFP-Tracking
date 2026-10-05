package de.easynfp.data.backup

import de.easynfp.backup.BackupCycle
import de.easynfp.backup.BackupData
import de.easynfp.backup.BackupDay
import de.easynfp.backup.BackupDisturbance
import de.easynfp.backup.BackupError
import de.easynfp.backup.BackupException
import de.easynfp.data.BackupContent
import de.easynfp.data.db.Converters
import de.easynfp.data.model.CodeMap
import de.easynfp.data.model.Codes
import de.easynfp.data.model.CycleRecord
import de.easynfp.data.model.DayEntry
import de.easynfp.data.model.Disturbance
import de.easynfp.rules.model.CycleOverrides
import de.easynfp.rules.model.Temperature
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

/** Converts between the stored data and the backup file format. */
object BackupMapper {

    fun toBackup(content: BackupContent, settings: Map<String, String>, appVersion: String, now: Instant): BackupData =
        BackupData(
            createdAt = now.toString(),
            appVersion = appVersion,
            cycles = content.cycles.map { cycle ->
                BackupCycle(
                    startDate = cycle.startDate.toString(),
                    excludedFromStatistics = cycle.excludedFromStatistics,
                    overrideFirstHigherMeasurement = cycle.overrides.firstHigherMeasurement?.toString(),
                    overrideMucusPeak = cycle.overrides.mucusPeak?.toString(),
                )
            },
            days = content.days.map { day ->
                BackupDay(
                    date = day.date.toString(),
                    temperatureHundredths = day.temperature?.hundredths,
                    measurementTime = day.measurementTime?.format(Converters.TIME_FORMAT),
                    temperatureExcluded = day.temperatureExcluded,
                    mucusSensation = day.mucusSensation?.let(Codes.sensation::code),
                    mucusAppearance = day.mucusAppearance?.let(Codes.appearance::code),
                    bleeding = day.bleeding?.let(Codes.bleeding::code),
                    intercourse = day.intercourse?.let(Codes.intercourse::code),
                    mittelschmerz = day.mittelschmerz,
                    breastSymptom = day.breastSymptom,
                    note = day.note,
                    disturbanceIds = day.disturbanceIds.sorted(),
                )
            },
            disturbances = content.disturbances.map { disturbance ->
                BackupDisturbance(
                    id = disturbance.id,
                    key = disturbance.predefined?.code,
                    name = disturbance.customName,
                    archived = disturbance.archived,
                    sortOrder = disturbance.sortOrder,
                )
            },
            settings = settings,
        )

    /** @throws BackupException with [BackupError.CORRUPT] for invalid values. */
    fun fromBackup(data: BackupData): BackupContent = try {
        BackupContent(
            days = data.days.map { day ->
                DayEntry(
                    date = LocalDate.parse(day.date),
                    temperature = day.temperatureHundredths?.let(::Temperature),
                    measurementTime = day.measurementTime?.let { LocalTime.parse(it, Converters.TIME_FORMAT) },
                    temperatureExcluded = day.temperatureExcluded,
                    mucusSensation = day.mucusSensation?.let { Codes.sensation.require(it) },
                    mucusAppearance = day.mucusAppearance?.let { Codes.appearance.require(it) },
                    bleeding = day.bleeding?.let { Codes.bleeding.require(it) },
                    intercourse = day.intercourse?.let { Codes.intercourse.require(it) },
                    mittelschmerz = day.mittelschmerz,
                    breastSymptom = day.breastSymptom,
                    note = day.note,
                    disturbanceIds = day.disturbanceIds.toSet(),
                )
            },
            cycles = data.cycles.map { cycle ->
                CycleRecord(
                    startDate = LocalDate.parse(cycle.startDate),
                    excludedFromStatistics = cycle.excludedFromStatistics,
                    overrides = CycleOverrides(
                        firstHigherMeasurement = cycle.overrideFirstHigherMeasurement?.let(LocalDate::parse),
                        mucusPeak = cycle.overrideMucusPeak?.let(LocalDate::parse),
                    ),
                )
            },
            disturbances = data.disturbances.map { disturbance ->
                Disturbance(
                    id = disturbance.id,
                    predefined = disturbance.key?.let { Codes.predefinedDisturbance.require(it) },
                    customName = disturbance.name,
                    archived = disturbance.archived,
                    sortOrder = disturbance.sortOrder,
                )
            },
        )
    } catch (e: DateTimeParseException) {
        throw BackupException(BackupError.CORRUPT, e)
    }

    private fun <E : Enum<E>> CodeMap<E>.require(code: String): E =
        valueOf(code) ?: throw BackupException(BackupError.CORRUPT)
}
