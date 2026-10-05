package de.easynfp.data.db

import androidx.room.migration.Migration

/**
 * All schema migrations, oldest first. Example for a future version 2:
 *
 * ```
 * val MIGRATION_1_2 = object : Migration(1, 2) {
 *     override fun migrate(db: SupportSQLiteDatabase) {
 *         db.execSQL("ALTER TABLE day_entries ADD COLUMN cervixPosition TEXT")
 *     }
 * }
 * ```
 */
object Migrations {
    val ALL: Array<Migration> = arrayOf()
}
