package de.easynfp.backup

import kotlinx.serialization.Serializable

/**
 * Complete content of a backup file (F-50), format version [BackupFormat.CURRENT_VERSION].
 *
 * Rules for changing this format (N-07):
 * - Adding an optional field with a default value is compatible and needs no new version.
 * - Renaming, removing or re-interpreting a field requires a new format version and a migration in
 *   [BackupMigrations], so that every backup ever written stays importable.
 *
 * Dates are ISO-8601 strings (`2026-10-05`), times `HH:mm`. Enumerations are stored as stable codes
 * defined by the data layer, never as ordinals.
 */
@Serializable
public data class BackupData(
    val formatVersion: Int = BackupFormat.CURRENT_VERSION,
    val app: String = BackupFormat.APP_ID,
    /** ISO-8601 instant of the export. */
    val createdAt: String,
    val appVersion: String,
    val cycles: List<BackupCycle> = emptyList(),
    val days: List<BackupDay> = emptyList(),
    val disturbances: List<BackupDisturbance> = emptyList(),
    /** User settings as key/value pairs; unknown keys are ignored on import. */
    val settings: Map<String, String> = emptyMap(),
)

@Serializable
public data class BackupCycle(
    val startDate: String,
    val excludedFromStatistics: Boolean = false,
    val overrideFirstHigherMeasurement: String? = null,
    val overrideMucusPeak: String? = null,
)

@Serializable
public data class BackupDay(
    val date: String,
    val temperatureHundredths: Int? = null,
    val measurementTime: String? = null,
    val temperatureExcluded: Boolean = false,
    val mucusSensation: String? = null,
    val mucusAppearance: String? = null,
    val bleeding: String? = null,
    val intercourse: String? = null,
    val mittelschmerz: Boolean = false,
    val breastSymptom: Boolean = false,
    val note: String? = null,
    val disturbanceIds: List<Long> = emptyList(),
)

@Serializable
public data class BackupDisturbance(
    val id: Long,
    /** Code of a predefined disturbance (F-10), null for disturbances created by the user. */
    val key: String? = null,
    /** User defined name; for predefined disturbances only set if renamed. */
    val name: String? = null,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
)
