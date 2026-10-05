package de.easynfp.app.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.easynfp.app.BuildConfig
import de.easynfp.app.R
import de.easynfp.app.backup.BackupManager
import de.easynfp.app.lock.AppLock
import de.easynfp.app.reminders.Notifications
import de.easynfp.app.text.Formats
import de.easynfp.app.ui.components.ConfirmDialog
import de.easynfp.app.ui.components.SectionCard
import de.easynfp.app.ui.components.SubScreenScaffold
import de.easynfp.app.ui.components.SwitchRow
import de.easynfp.app.ui.components.TimePickerDialogFor
import de.easynfp.app.ui.components.appViewModelFactory
import de.easynfp.app.ui.navigation.AppNavigator
import de.easynfp.backup.BackupError
import de.easynfp.data.settings.AppSettings
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private enum class TimeTarget { MORNING, EVENING }

@Composable
fun SettingsScreen(
    navigator: AppNavigator,
    viewModel: SettingsViewModel = viewModel(
        factory = appViewModelFactory { c, app, _ -> SettingsViewModel(c, app) },
    ),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val pendingImport by viewModel.pendingImport.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    val messages = mapOf(
        "exported" to stringResource(R.string.backup_exported),
        "imported" to stringResource(R.string.backup_imported),
        "csv" to stringResource(R.string.csv_exported),
        "deleted" to stringResource(R.string.data_deleted),
        "failed" to stringResource(R.string.operation_failed),
        "password" to stringResource(R.string.backup_error_password),
        "version" to stringResource(R.string.backup_error_version),
        "invalid" to stringResource(R.string.backup_error_invalid),
    )
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            val key = when (message) {
                SettingsMessage.BackupExported -> "exported"
                SettingsMessage.BackupImported -> "imported"
                SettingsMessage.CsvExported -> "csv"
                SettingsMessage.DataDeleted -> "deleted"
                is SettingsMessage.Failed -> when (message.error) {
                    BackupError.WRONG_PASSWORD, BackupError.PASSWORD_REQUIRED -> "password"
                    BackupError.UNSUPPORTED_VERSION -> "version"
                    BackupError.NOT_A_BACKUP, BackupError.CORRUPT -> "invalid"
                    null -> "failed"
                }
            }
            snackbar.showSnackbar(messages.getValue(key))
        }
    }

    SubScreenScaffold(
        title = stringResource(R.string.nav_settings),
        onBack = navigator::back,
        snackbarHostState = snackbar,
    ) { padding ->
        val current = settings ?: return@SubScreenScaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            EntrySettings(current, viewModel, navigator)
            ReminderSettings(current, viewModel)
            LockSettings(current, viewModel, context as? FragmentActivity)
            BackupSettings(current, viewModel)
            SectionCard(title = stringResource(R.string.settings_info)) {
                LinkRow(stringResource(R.string.hints_title)) { navigator.openInfo("hints") }
                LinkRow(stringResource(R.string.about_title)) { navigator.openInfo("about") }
                Text(
                    stringResource(R.string.version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DangerZone(viewModel)
        }
    }

    pendingImport?.let { pending ->
        ImportDialog(
            encrypted = pending.encrypted,
            onConfirm = viewModel::confirmImport,
            onDismiss = viewModel::cancelImport,
        )
    }
}

@Composable
private fun EntrySettings(settings: AppSettings, viewModel: SettingsViewModel, navigator: AppNavigator) {
    SectionCard(title = stringResource(R.string.settings_entry)) {
        SwitchRow(
            title = stringResource(R.string.intercourse),
            checked = settings.showIntercourse,
            onCheckedChange = { value -> viewModel.update { it.copy(showIntercourse = value) } },
        )
        SwitchRow(
            title = stringResource(R.string.mittelschmerz),
            checked = settings.showMittelschmerz,
            onCheckedChange = { value -> viewModel.update { it.copy(showMittelschmerz = value) } },
        )
        SwitchRow(
            title = stringResource(R.string.breast_symptom),
            checked = settings.showBreastSymptom,
            onCheckedChange = { value -> viewModel.update { it.copy(showBreastSymptom = value) } },
        )
        LinkRow(stringResource(R.string.manage_disturbances), onClick = navigator::openDisturbances)
    }
}

@Composable
private fun ReminderSettings(settings: AppSettings, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    var editing by rememberSaveable { mutableStateOf<TimeTarget?>(null) }
    var pendingEnable by rememberSaveable { mutableStateOf<TimeTarget?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendingEnable?.let { target -> enableReminder(viewModel, target, true) }
        pendingEnable = null
    }
    fun toggle(target: TimeTarget, enabled: Boolean) {
        if (enabled && !Notifications.hasPermission(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pendingEnable = target
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            enableReminder(viewModel, target, enabled)
        }
    }

    SectionCard(title = stringResource(R.string.settings_reminders)) {
        SwitchRow(
            title = stringResource(R.string.reminder_morning),
            subtitle = stringResource(R.string.reminder_morning_hint),
            checked = settings.morningReminderEnabled,
            onCheckedChange = { toggle(TimeTarget.MORNING, it) },
        )
        TimeRow(settings.morningReminderTime, enabled = settings.morningReminderEnabled) {
            editing = TimeTarget.MORNING
        }
        SwitchRow(
            title = stringResource(R.string.reminder_evening),
            subtitle = stringResource(R.string.reminder_evening_hint),
            checked = settings.eveningReminderEnabled,
            onCheckedChange = { toggle(TimeTarget.EVENING, it) },
        )
        TimeRow(settings.eveningReminderTime, enabled = settings.eveningReminderEnabled) {
            editing = TimeTarget.EVENING
        }
        val anyReminder = settings.morningReminderEnabled || settings.eveningReminderEnabled
        if (anyReminder && !Notifications.hasPermission(context)) {
            Text(
                stringResource(R.string.reminder_permission_missing),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    editing?.let { target ->
        TimePickerDialogFor(
            initial = if (target == TimeTarget.MORNING) settings.morningReminderTime else settings.eveningReminderTime,
            onConfirm = { time ->
                viewModel.update {
                    if (target == TimeTarget.MORNING) {
                        it.copy(
                            morningReminderTime = time,
                        )
                    } else {
                        it.copy(eveningReminderTime = time)
                    }
                }
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

private fun enableReminder(viewModel: SettingsViewModel, target: TimeTarget, enabled: Boolean) {
    viewModel.update {
        if (target == TimeTarget.MORNING) {
            it.copy(
                morningReminderEnabled = enabled,
            )
        } else {
            it.copy(eveningReminderEnabled = enabled)
        }
    }
}

@Composable
private fun TimeRow(time: LocalTime, enabled: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.reminder_time, Formats.time(time)),
            modifier = Modifier.weight(1f),
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onClick, enabled = enabled) { Text(stringResource(R.string.change)) }
    }
}

@Composable
private fun LockSettings(settings: AppSettings, viewModel: SettingsViewModel, activity: FragmentActivity?) {
    var unavailable by rememberSaveable { mutableStateOf(false) }
    val title = stringResource(R.string.lock_prompt_title)
    SectionCard(title = stringResource(R.string.settings_security)) {
        SwitchRow(
            title = stringResource(R.string.app_lock),
            subtitle = stringResource(R.string.app_lock_hint),
            checked = settings.appLockEnabled,
            onCheckedChange = { enabled ->
                if (activity == null) return@SwitchRow
                if (enabled && !AppLock.isAvailable(activity)) {
                    unavailable = true
                } else {
                    // Confirm with the device credential before changing the lock.
                    AppLock.authenticate(activity, title) { ok ->
                        if (ok) viewModel.update { it.copy(appLockEnabled = enabled) }
                    }
                }
            },
        )
        if (unavailable) {
            Text(
                stringResource(R.string.app_lock_unavailable),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun BackupSettings(settings: AppSettings, viewModel: SettingsViewModel) {
    var exportPassword by remember { mutableStateOf<String?>(null) }
    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(de.easynfp.backup.BackupFormat.MIME_TYPE),
    ) { uri ->
        val password = exportPassword
        if (uri != null && password != null) viewModel.exportBackup(uri, password)
        exportPassword = null
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.readImport(uri)
    }
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) viewModel.setAutoBackupFolder(uri)
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) viewModel.exportCsv(uri)
    }
    val csvName = stringResource(R.string.csv_file_name, LocalDate.now().toString())

    SectionCard(title = stringResource(R.string.settings_backup)) {
        Text(stringResource(R.string.backup_hint), style = MaterialTheme.typography.bodySmall)
        LinkRow(stringResource(R.string.backup_export)) { showExportDialog = true }
        LinkRow(stringResource(R.string.backup_import)) { importLauncher.launch(arrayOf("*/*")) }
        LinkRow(stringResource(R.string.csv_export)) { csvLauncher.launch(csvName) }
    }

    SectionCard(title = stringResource(R.string.auto_backup)) {
        SwitchRow(
            title = stringResource(R.string.auto_backup_enabled),
            subtitle = stringResource(R.string.auto_backup_hint),
            checked = settings.autoBackupEnabled,
            onCheckedChange = { enabled ->
                if (enabled && settings.autoBackupFolderUri == null) {
                    folderLauncher.launch(null)
                } else {
                    viewModel.update { it.copy(autoBackupEnabled = enabled) }
                }
            },
        )
        val folder = settings.autoBackupFolderUri
        Text(
            if (folder != null) {
                stringResource(R.string.auto_backup_folder, android.net.Uri.parse(folder).lastPathSegment.orEmpty())
            } else {
                stringResource(R.string.auto_backup_no_folder)
            },
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(
            onClick = { folderLauncher.launch(null) },
        ) { Text(stringResource(R.string.auto_backup_choose_folder)) }
        Text(stringResource(R.string.auto_backup_interval), style = MaterialTheme.typography.labelLarge)
        ChipRow(
            options = listOf(1, 7, 14, 30),
            selected = settings.autoBackupIntervalDays,
            label = { pluralStringResource(R.plurals.every_n_days, it, it) },
            onSelect = { value -> viewModel.update { it.copy(autoBackupIntervalDays = value) } },
        )
        Text(stringResource(R.string.auto_backup_keep), style = MaterialTheme.typography.labelLarge)
        ChipRow(
            options = listOf(3, 5, 10, 20),
            selected = settings.autoBackupKeepCount,
            label = { it.toString() },
            onSelect = { value -> viewModel.update { it.copy(autoBackupKeepCount = value) } },
        )
        settings.lastAutoBackupEpochMillis?.let { millis ->
            val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
            Text(
                stringResource(R.string.auto_backup_last, Formats.dateMedium(date)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showExportDialog) {
        ExportDialog(
            onConfirm = { password ->
                showExportDialog = false
                exportPassword = password
                exportLauncher.launch(BackupManager.suggestedFileName())
            },
            onDismiss = { showExportDialog = false },
        )
    }
}

@Composable
private fun <T> ChipRow(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) })
        }
    }
}

@Composable
private fun ExportDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    val mismatch = password != repeat
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_export)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.backup_password_hint))
                PasswordField(password, { password = it }, stringResource(R.string.password))
                PasswordField(repeat, { repeat = it }, stringResource(R.string.password_repeat))
                if (mismatch && repeat.isNotEmpty()) {
                    Text(stringResource(R.string.password_mismatch), color = MaterialTheme.colorScheme.error)
                }
                if (password.isEmpty()) {
                    Text(
                        stringResource(R.string.backup_unencrypted_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password) },
                enabled = !mismatch,
            ) { Text(stringResource(R.string.backup_choose_file)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ImportDialog(encrypted: Boolean, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_import)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.backup_import_warning))
                if (encrypted) PasswordField(password, { password = it }, stringResource(R.string.password))
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password) }, enabled = !encrypted || password.isNotEmpty()) {
                Text(stringResource(R.string.backup_import_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** F-55: delete everything, with two confirmations. */
@Composable
private fun DangerZone(viewModel: SettingsViewModel) {
    var step by rememberSaveable { mutableStateOf(0) }
    SectionCard(title = stringResource(R.string.settings_danger)) {
        TextButton(onClick = { step = 1 }) {
            Text(stringResource(R.string.delete_all), color = MaterialTheme.colorScheme.error)
        }
    }
    when (step) {
        1 -> ConfirmDialog(
            title = stringResource(R.string.delete_all),
            text = stringResource(R.string.delete_all_first),
            confirmLabel = stringResource(R.string.delete_all_continue),
            onConfirm = { step = 2 },
            onDismiss = { step = 0 },
        )
        2 -> ConfirmDialog(
            title = stringResource(R.string.delete_all_second_title),
            text = stringResource(R.string.delete_all_second),
            confirmLabel = stringResource(R.string.delete_all_confirm),
            onConfirm = {
                step = 0
                viewModel.deleteAll()
            },
            onDismiss = { step = 0 },
        )
    }
}

@Composable
private fun LinkRow(title: String, onClick: () -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    )
}
