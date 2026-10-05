package de.easynfp.app.backup

import android.content.Context
import android.net.Uri
import de.easynfp.app.BuildConfig
import de.easynfp.backup.BackupError
import de.easynfp.backup.BackupException
import de.easynfp.backup.BackupFormat
import de.easynfp.data.DataContainer
import de.easynfp.data.backup.BackupMapper
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** F-50: export and import of the complete backup file via the Storage Access Framework. */
object BackupManager {

    sealed interface Result {
        data object Success : Result

        data class Failure(val error: BackupError?) : Result
    }

    fun suggestedFileName(prefix: String = "easynfp-sicherung"): String =
        "$prefix-${LocalDateTime.now().format(FILE_TIMESTAMP)}.${BackupFormat.FILE_EXTENSION}"

    suspend fun createBackup(context: Context, password: CharArray?): ByteArray = withContext(Dispatchers.IO) {
        val container = DataContainer.get(context)
        val content = container.repository.snapshotForBackup()
        val data = BackupMapper.toBackup(
            content = content,
            settings = container.settings.exportForBackup(),
            appVersion = BuildConfig.VERSION_NAME,
            now = container.clock.instant(),
        )
        BackupFormat.encode(data, password)
    }

    suspend fun export(context: Context, uri: Uri, password: CharArray?): Result = withContext(Dispatchers.IO) {
        try {
            val bytes = createBackup(context, password)
            val stream = context.contentResolver.openOutputStream(uri, "wt") ?: return@withContext Result.Failure(null)
            stream.use { it.write(bytes) }
            Result.Success
        } catch (e: IOException) {
            Result.Failure(null)
        } catch (e: SecurityException) {
            Result.Failure(null)
        }
    }

    suspend fun read(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        }
    }

    fun isEncrypted(bytes: ByteArray): Boolean = BackupFormat.isEncrypted(bytes)

    /** Replaces all data with the backup content. Nothing is changed if the file cannot be read. */
    suspend fun import(context: Context, bytes: ByteArray, password: CharArray?): Result = withContext(Dispatchers.IO) {
        try {
            val data = BackupFormat.decode(bytes, password)
            val content = BackupMapper.fromBackup(data)
            val container = DataContainer.get(context)
            container.repository.replaceAll(content)
            container.settings.importFromBackup(data.settings)
            Result.Success
        } catch (e: BackupException) {
            Result.Failure(e.error)
        }
    }

    private val FILE_TIMESTAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm")
}
