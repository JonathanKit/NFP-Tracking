package de.easynfp.data.db

import de.easynfp.data.model.Codes
import de.easynfp.data.model.CycleRecord
import de.easynfp.data.model.DayEntry
import de.easynfp.data.model.Disturbance
import de.easynfp.rules.model.CycleOverrides
import de.easynfp.rules.model.Temperature

internal fun DayEntryEntity.toModel(disturbanceIds: Set<Long>): DayEntry = DayEntry(
    date = date,
    temperature = temperatureHundredths?.let(::Temperature),
    measurementTime = measurementTime,
    temperatureExcluded = temperatureExcluded,
    mucusSensation = mucusSensation,
    mucusAppearance = mucusAppearance,
    bleeding = bleeding,
    intercourse = intercourse,
    mittelschmerz = mittelschmerz,
    breastSymptom = breastSymptom,
    note = note,
    disturbanceIds = disturbanceIds,
)

internal fun DayEntry.toEntity(updatedAt: Long): DayEntryEntity = DayEntryEntity(
    date = date,
    temperatureHundredths = temperature?.hundredths,
    measurementTime = measurementTime,
    temperatureExcluded = temperatureExcluded,
    mucusSensation = mucusSensation,
    mucusAppearance = mucusAppearance,
    bleeding = bleeding,
    intercourse = intercourse,
    mittelschmerz = mittelschmerz,
    breastSymptom = breastSymptom,
    note = note?.takeIf { it.isNotBlank() },
    updatedAt = updatedAt,
)

internal fun CycleEntity.toModel(): CycleRecord = CycleRecord(
    startDate = startDate,
    excludedFromStatistics = excludedFromStatistics,
    overrides = CycleOverrides(
        firstHigherMeasurement = overrideFirstHigherMeasurement,
        mucusPeak = overrideMucusPeak,
    ),
)

internal fun CycleRecord.toEntity(): CycleEntity = CycleEntity(
    startDate = startDate,
    excludedFromStatistics = excludedFromStatistics,
    overrideFirstHigherMeasurement = overrides.firstHigherMeasurement,
    overrideMucusPeak = overrides.mucusPeak,
)

internal fun DisturbanceEntity.toModel(): Disturbance = Disturbance(
    id = id,
    predefined = predefinedCode?.let(Codes.predefinedDisturbance::valueOf),
    customName = customName,
    archived = archived,
    sortOrder = sortOrder,
)

internal fun Disturbance.toEntity(): DisturbanceEntity = DisturbanceEntity(
    id = id,
    predefinedCode = predefined?.code,
    customName = customName,
    archived = archived,
    sortOrder = sortOrder,
)
