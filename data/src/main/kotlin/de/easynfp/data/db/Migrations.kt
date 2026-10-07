package de.easynfp.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * All schema migrations, oldest first (N-07). Every released version must stay reachable, so
 * migrations are never removed and never destructive. Each one has a test in MigrationTest.
 */
object Migrations {
    /** Version 2: symptoms and bleeding outside the period (F-16). */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE day_entries ADD COLUMN otherBleeding TEXT")
            db.execSQL("ALTER TABLE day_entries ADD COLUMN cramps TEXT")
            db.execSQL("ALTER TABLE day_entries ADD COLUMN nausea TEXT")
            db.execSQL("ALTER TABLE day_entries ADD COLUMN diarrhea INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE day_entries ADD COLUMN vomiting INTEGER NOT NULL DEFAULT 0")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
