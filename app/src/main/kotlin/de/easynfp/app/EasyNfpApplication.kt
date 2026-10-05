package de.easynfp.app

import android.app.Application
import de.easynfp.app.backup.AutoBackupScheduler
import de.easynfp.app.lock.AppLock
import de.easynfp.app.reminders.Notifications
import de.easynfp.app.reminders.ReminderScheduler
import de.easynfp.data.DataContainer
import de.easynfp.widget.EasyNfpWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class EasyNfpApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val container: DataContainer by lazy { DataContainer.get(this) }

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        AppLock.install()

        // Keep the widget in sync with the data and the app lock (F-20, F-54).
        appScope.launch {
            combine(
                container.repository.dayEntries,
                container.repository.cycles,
                container.settings.settings.map { it.appLockEnabled },
            ) { _, _, _ -> Unit }
                .drop(1)
                .debounce(WIDGET_UPDATE_DEBOUNCE_MILLIS)
                .collect { EasyNfpWidget.updateAll(this@EasyNfpApplication) }
        }
        // Reminders and automatic backups follow the settings (F-23, F-24, F-51).
        appScope.launch {
            container.settings.settings
                .distinctUntilChangedBy {
                    listOf(
                        it.morningReminderEnabled,
                        it.morningReminderTime,
                        it.eveningReminderEnabled,
                        it.eveningReminderTime,
                    )
                }
                .collect { ReminderScheduler.schedule(this@EasyNfpApplication, it) }
        }
        appScope.launch {
            container.settings.settings
                .distinctUntilChangedBy {
                    Triple(
                        it.autoBackupEnabled,
                        it.autoBackupFolderUri,
                        it.autoBackupIntervalDays,
                    )
                }
                .collect { AutoBackupScheduler.schedule(this@EasyNfpApplication, it) }
        }
    }

    private companion object {
        const val WIDGET_UPDATE_DEBOUNCE_MILLIS = 500L
    }
}
