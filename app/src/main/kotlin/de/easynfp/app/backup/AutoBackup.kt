package de.easynfp.app.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import de.easynfp.backup.BackupFormat
import de.easynfp.data.DataContainer
import de.easynfp.data.settings.AppSettings
import java.io.IOException
import java.util.concurrent.TimeUnit

/** F-51: periodic backup into a folder chosen by the user, keeping the last N files. */
object AutoBackupScheduler {
    private const val WORK_NAME = "auto-backup"
    const val FILE_PREFIX = "easynfp-auto"

    fun schedule(context: Context, settings: AppSettings) {
        // WorkManager initializes itself at app start; the check only protects unusual environments (tests).
        if (!WorkManager.isInitialized()) return
        val workManager = WorkManager.getInstance(context)
        if (!settings.autoBackupEnabled || settings.autoBackupFolderUri == null) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(
            settings.autoBackupIntervalDays.coerceAtLeast(1).toLong(),
            TimeUnit.DAYS,
        ).build()
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}

class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = DataContainer.get(applicationContext)
        // F-48: never overwrite real backups with demo data.
        if (container.isDemo) return Result.success()
        val settings = container.settings.current()
        val folderUri = settings.autoBackupFolderUri
        if (!settings.autoBackupEnabled || folderUri == null) return Result.success()
        val folder = DocumentFile.fromTreeUri(applicationContext, Uri.parse(folderUri))
        if (folder == null || !folder.canWrite()) return Result.failure()

        return try {
            if (write(folder)) {
                prune(folder, settings.autoBackupKeepCount)
                container.settings.update { it.copy(lastAutoBackupEpochMillis = System.currentTimeMillis()) }
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: IOException) {
            Result.retry()
        } catch (e: SecurityException) {
            Result.failure()
        }
    }

    private suspend fun write(folder: DocumentFile): Boolean {
        val bytes = BackupManager.createBackup(applicationContext, password = null)
        val name = BackupManager.suggestedFileName(AutoBackupScheduler.FILE_PREFIX)
        val file = folder.createFile(BackupFormat.MIME_TYPE, name) ?: return false
        val stream = applicationContext.contentResolver.openOutputStream(file.uri, "wt") ?: return false
        stream.use { it.write(bytes) }
        return true
    }

    /** Keeps the newest [keep] automatic backups; names sort chronologically. */
    private fun prune(folder: DocumentFile, keep: Int) {
        folder.listFiles()
            .filter { it.name?.startsWith(AutoBackupScheduler.FILE_PREFIX) == true }
            .sortedByDescending { it.name }
            .drop(keep.coerceAtLeast(1))
            .forEach { it.delete() }
    }
}
