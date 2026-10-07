package de.easynfp.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * N-07: before an app update migrates the database for the first time, a copy of the old file is kept
 * in the private app storage (`databases/<name>.v<old version>.bak`). If a migration ever failed, the
 * data could still be recovered from it. Only the copy of the latest migration is kept.
 */
internal object DatabaseSafety {
    private const val TAG = "DatabaseSafety"
    private val SUFFIXES = listOf("", "-wal", "-shm")

    fun copyBeforeMigration(context: Context, name: String, targetVersion: Int) {
        val file = context.getDatabasePath(name)
        if (!file.exists()) return
        val version = storedVersion(file) ?: return
        if (version <= 0 || version >= targetVersion) return
        try {
            file.parentFile?.listFiles { _, fileName -> fileName.startsWith("$name.v") && ".bak" in fileName }
                ?.forEach { it.delete() }
            SUFFIXES.forEach { suffix ->
                val source = File(file.path + suffix)
                if (source.exists()) source.copyTo(File("${file.path}.v$version.bak$suffix"), overwrite = true)
            }
        } catch (e: IOException) {
            // The migration itself does not depend on the copy.
            Log.w(TAG, "Could not copy the database before migrating", e)
        }
    }

    private fun storedVersion(file: File): Int? = try {
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { it.version }
    } catch (e: SQLiteException) {
        Log.w(TAG, "Could not read the database version", e)
        null
    }
}
