package de.easynfp.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import java.time.format.DateTimeParseException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Reads and writes [AppSettings]. Keys are stable and also used in backups. */
class SettingsRepository(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data.map(::fromPreferences)

    suspend fun current(): AppSettings = settings.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val updated = transform(fromPreferences(prefs))
            write(prefs, updated)
        }
    }

    /** Settings that are part of a backup (device specific values like the backup folder are not). */
    suspend fun exportForBackup(): Map<String, String> {
        val s = current()
        return mapOf(
            Keys.MORNING_ENABLED.name to s.morningReminderEnabled.toString(),
            Keys.MORNING_TIME.name to s.morningReminderTime.toString(),
            Keys.EVENING_ENABLED.name to s.eveningReminderEnabled.toString(),
            Keys.EVENING_TIME.name to s.eveningReminderTime.toString(),
            Keys.SHOW_INTERCOURSE.name to s.showIntercourse.toString(),
            Keys.SHOW_MITTELSCHMERZ.name to s.showMittelschmerz.toString(),
            Keys.SHOW_BREAST.name to s.showBreastSymptom.toString(),
            Keys.HINTS_DISMISSED.name to s.measuringHintsDismissed.toString(),
        )
    }

    suspend fun importFromBackup(values: Map<String, String>) {
        update { s ->
            s.copy(
                morningReminderEnabled = values[Keys.MORNING_ENABLED.name]?.toBooleanStrictOrNull()
                    ?: s.morningReminderEnabled,
                morningReminderTime = values[Keys.MORNING_TIME.name]?.let(::parseTime) ?: s.morningReminderTime,
                eveningReminderEnabled = values[Keys.EVENING_ENABLED.name]?.toBooleanStrictOrNull()
                    ?: s.eveningReminderEnabled,
                eveningReminderTime = values[Keys.EVENING_TIME.name]?.let(::parseTime) ?: s.eveningReminderTime,
                showIntercourse = values[Keys.SHOW_INTERCOURSE.name]?.toBooleanStrictOrNull() ?: s.showIntercourse,
                showMittelschmerz = values[Keys.SHOW_MITTELSCHMERZ.name]?.toBooleanStrictOrNull()
                    ?: s.showMittelschmerz,
                showBreastSymptom = values[Keys.SHOW_BREAST.name]?.toBooleanStrictOrNull() ?: s.showBreastSymptom,
                measuringHintsDismissed = values[Keys.HINTS_DISMISSED.name]?.toBooleanStrictOrNull()
                    ?: s.measuringHintsDismissed,
            )
        }
    }

    /** F-55 */
    suspend fun reset() {
        dataStore.edit { it.clear() }
    }

    private fun fromPreferences(p: Preferences): AppSettings {
        val d = AppSettings()
        return AppSettings(
            morningReminderEnabled = p[Keys.MORNING_ENABLED] ?: d.morningReminderEnabled,
            morningReminderTime = p[Keys.MORNING_TIME]?.let(::parseTime) ?: d.morningReminderTime,
            eveningReminderEnabled = p[Keys.EVENING_ENABLED] ?: d.eveningReminderEnabled,
            eveningReminderTime = p[Keys.EVENING_TIME]?.let(::parseTime) ?: d.eveningReminderTime,
            showIntercourse = p[Keys.SHOW_INTERCOURSE] ?: d.showIntercourse,
            showMittelschmerz = p[Keys.SHOW_MITTELSCHMERZ] ?: d.showMittelschmerz,
            showBreastSymptom = p[Keys.SHOW_BREAST] ?: d.showBreastSymptom,
            appLockEnabled = p[Keys.APP_LOCK] ?: d.appLockEnabled,
            autoBackupEnabled = p[Keys.AUTO_BACKUP] ?: d.autoBackupEnabled,
            autoBackupFolderUri = p[Keys.AUTO_BACKUP_FOLDER],
            autoBackupIntervalDays = p[Keys.AUTO_BACKUP_INTERVAL] ?: d.autoBackupIntervalDays,
            autoBackupKeepCount = p[Keys.AUTO_BACKUP_KEEP] ?: d.autoBackupKeepCount,
            lastAutoBackupEpochMillis = p[Keys.LAST_AUTO_BACKUP],
            measuringHintsDismissed = p[Keys.HINTS_DISMISSED] ?: d.measuringHintsDismissed,
        )
    }

    private fun write(p: MutablePreferences, s: AppSettings) {
        p[Keys.MORNING_ENABLED] = s.morningReminderEnabled
        p[Keys.MORNING_TIME] = s.morningReminderTime.toString()
        p[Keys.EVENING_ENABLED] = s.eveningReminderEnabled
        p[Keys.EVENING_TIME] = s.eveningReminderTime.toString()
        p[Keys.SHOW_INTERCOURSE] = s.showIntercourse
        p[Keys.SHOW_MITTELSCHMERZ] = s.showMittelschmerz
        p[Keys.SHOW_BREAST] = s.showBreastSymptom
        p[Keys.APP_LOCK] = s.appLockEnabled
        p[Keys.AUTO_BACKUP] = s.autoBackupEnabled
        val folder = s.autoBackupFolderUri
        if (folder == null) p.remove(Keys.AUTO_BACKUP_FOLDER) else p[Keys.AUTO_BACKUP_FOLDER] = folder
        p[Keys.AUTO_BACKUP_INTERVAL] = s.autoBackupIntervalDays
        p[Keys.AUTO_BACKUP_KEEP] = s.autoBackupKeepCount
        val last = s.lastAutoBackupEpochMillis
        if (last == null) p.remove(Keys.LAST_AUTO_BACKUP) else p[Keys.LAST_AUTO_BACKUP] = last
        p[Keys.HINTS_DISMISSED] = s.measuringHintsDismissed
    }

    private fun parseTime(text: String): LocalTime? = try {
        LocalTime.parse(text)
    } catch (e: DateTimeParseException) {
        null
    }

    private object Keys {
        val MORNING_ENABLED = booleanPreferencesKey("morning_reminder_enabled")
        val MORNING_TIME = stringPreferencesKey("morning_reminder_time")
        val EVENING_ENABLED = booleanPreferencesKey("evening_reminder_enabled")
        val EVENING_TIME = stringPreferencesKey("evening_reminder_time")
        val SHOW_INTERCOURSE = booleanPreferencesKey("show_intercourse")
        val SHOW_MITTELSCHMERZ = booleanPreferencesKey("show_mittelschmerz")
        val SHOW_BREAST = booleanPreferencesKey("show_breast_symptom")
        val APP_LOCK = booleanPreferencesKey("app_lock_enabled")
        val AUTO_BACKUP = booleanPreferencesKey("auto_backup_enabled")
        val AUTO_BACKUP_FOLDER = stringPreferencesKey("auto_backup_folder")
        val AUTO_BACKUP_INTERVAL = intPreferencesKey("auto_backup_interval_days")
        val AUTO_BACKUP_KEEP = intPreferencesKey("auto_backup_keep_count")
        val LAST_AUTO_BACKUP = longPreferencesKey("last_auto_backup")
        val HINTS_DISMISSED = booleanPreferencesKey("measuring_hints_dismissed")
    }
}
