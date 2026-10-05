package de.easynfp.data.settings

import java.time.LocalTime

/** User settings stored in DataStore. Defaults apply on first start. */
data class AppSettings(
    /** F-23 */
    val morningReminderEnabled: Boolean = false,
    val morningReminderTime: LocalTime = LocalTime.of(6, 30),
    /** F-24 */
    val eveningReminderEnabled: Boolean = false,
    val eveningReminderTime: LocalTime = LocalTime.of(21, 0),
    /** F-14: optional fields in the day entry. */
    val showIntercourse: Boolean = true,
    val showMittelschmerz: Boolean = true,
    val showBreastSymptom: Boolean = true,
    /** F-54 */
    val appLockEnabled: Boolean = false,
    /** F-51 */
    val autoBackupEnabled: Boolean = false,
    val autoBackupFolderUri: String? = null,
    val autoBackupIntervalDays: Int = 7,
    val autoBackupKeepCount: Int = 5,
    val lastAutoBackupEpochMillis: Long? = null,
    /** R-08: the measuring hints were read and dismissed. */
    val measuringHintsDismissed: Boolean = false,
)
