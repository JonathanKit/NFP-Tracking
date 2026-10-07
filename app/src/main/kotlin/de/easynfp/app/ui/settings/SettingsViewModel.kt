package de.easynfp.app.ui.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.easynfp.app.AppRestart
import de.easynfp.app.backup.BackupManager
import de.easynfp.app.export.CsvExporter
import de.easynfp.backup.BackupError
import de.easynfp.data.DataContainer
import de.easynfp.data.demo.DemoMode
import de.easynfp.data.settings.AppSettings
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One-time messages for a snackbar. */
sealed interface SettingsMessage {
    data object BackupExported : SettingsMessage

    data object BackupImported : SettingsMessage

    data object CsvExported : SettingsMessage

    data object DataDeleted : SettingsMessage

    data class Failed(val error: BackupError?) : SettingsMessage
}

/** A backup file that was read and waits for password and/or confirmation. */
data class PendingImport(val bytes: ByteArray, val encrypted: Boolean) {
    override fun equals(other: Any?): Boolean =
        other is PendingImport && other.encrypted == encrypted && other.bytes.contentEquals(bytes)

    override fun hashCode(): Int = bytes.contentHashCode() * 31 + encrypted.hashCode()
}

class SettingsViewModel(
    private val container: DataContainer,
    private val application: Application,
) : ViewModel() {
    val settings: StateFlow<AppSettings?> = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private val messageChannel = Channel<SettingsMessage>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    private val pendingImportState = MutableStateFlow<PendingImport?>(null)
    val pendingImport: StateFlow<PendingImport?> = pendingImportState.asStateFlow()

    private val busyState = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = busyState.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { container.settings.update(transform) }
    }

    /** F-50 */
    fun exportBackup(uri: Uri, password: String) = work {
        val result = BackupManager.export(application, uri, password.takeIf { it.isNotEmpty() }?.toCharArray())
        messageChannel.send(
            if (result is BackupManager.Result.Success) {
                SettingsMessage.BackupExported
            } else {
                SettingsMessage.Failed(
                    null,
                )
            },
        )
    }

    /** F-50: first step of the import, reads the file. */
    fun readImport(uri: Uri) = work {
        val bytes = BackupManager.read(application, uri)
        if (bytes == null) {
            messageChannel.send(SettingsMessage.Failed(null))
        } else {
            pendingImportState.value = PendingImport(bytes, BackupManager.isEncrypted(bytes))
        }
    }

    /** F-50: second step, after the user confirmed replacing all data. */
    fun confirmImport(password: String) {
        val pending = pendingImportState.value ?: return
        work {
            val result = BackupManager.import(
                application,
                pending.bytes,
                password.takeIf { it.isNotEmpty() }?.toCharArray(),
            )
            when (result) {
                BackupManager.Result.Success -> {
                    pendingImportState.value = null
                    messageChannel.send(SettingsMessage.BackupImported)
                }
                is BackupManager.Result.Failure -> {
                    // Keep the file for another password attempt.
                    if (result.error != BackupError.WRONG_PASSWORD) pendingImportState.value = null
                    messageChannel.send(SettingsMessage.Failed(result.error))
                }
            }
        }
    }

    fun cancelImport() {
        pendingImportState.value = null
    }

    /** F-51 */
    fun setAutoBackupFolder(uri: Uri) {
        application.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        update { it.copy(autoBackupFolderUri = uri.toString(), autoBackupEnabled = true) }
    }

    /** F-53 */
    fun exportCsv(uri: Uri) = work {
        val ok = CsvExporter.export(application, uri)
        messageChannel.send(if (ok) SettingsMessage.CsvExported else SettingsMessage.Failed(null))
    }

    /** F-48: demo data lives in its own database; switching restarts the app. */
    val isDemo: Boolean = container.isDemo

    fun setDemoMode(enabled: Boolean) = work {
        if (enabled) DemoMode.enable(application) else DemoMode.disable(application)
        AppRestart.restart(application)
    }

    /** F-55 */
    fun deleteAll() = work {
        container.repository.deleteAll()
        container.settings.reset()
        messageChannel.send(SettingsMessage.DataDeleted)
    }

    private fun work(block: suspend () -> Unit) {
        viewModelScope.launch {
            busyState.value = true
            try {
                block()
            } finally {
                busyState.value = false
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
