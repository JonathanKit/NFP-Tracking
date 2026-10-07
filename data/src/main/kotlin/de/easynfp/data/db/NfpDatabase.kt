package de.easynfp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import de.easynfp.data.model.PredefinedDisturbance

/**
 * The app database.
 *
 * Changing an entity requires increasing [VERSION], committing the generated schema file under
 * data/schemas and adding a migration to [Migrations.ALL] plus a test in MigrationTest (N-07).
 * Destructive migrations are never allowed.
 */
@Database(
    entities = [
        DayEntryEntity::class,
        CycleEntity::class,
        DisturbanceEntity::class,
        DayDisturbanceEntity::class,
    ],
    version = NfpDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class NfpDatabase : RoomDatabase() {
    abstract fun dayEntryDao(): DayEntryDao

    abstract fun cycleDao(): CycleDao

    abstract fun disturbanceDao(): DisturbanceDao

    companion object {
        const val VERSION = 2
        const val NAME = "easynfp.db"

        /** Separate database of the demo mode; the real data is never touched by it. */
        const val DEMO_NAME = "easynfp-demo.db"

        fun build(context: Context, name: String? = NAME): NfpDatabase {
            val builder = if (name == null) {
                Room.inMemoryDatabaseBuilder(context, NfpDatabase::class.java)
            } else {
                DatabaseSafety.copyBeforeMigration(context, name, VERSION)
                Room.databaseBuilder(context, NfpDatabase::class.java, name)
            }
            return builder
                .addMigrations(*Migrations.ALL)
                .addCallback(SeedCallback)
                .build()
        }
    }

    /** F-10: inserts the predefined disturbances when the database is created. */
    internal object SeedCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            seedPredefinedDisturbances(db)
        }
    }
}

internal fun seedPredefinedDisturbances(db: SupportSQLiteDatabase) {
    PredefinedDisturbance.entries.forEachIndexed { index, disturbance ->
        db.execSQL(
            "INSERT INTO disturbances (predefinedCode, customName, archived, sortOrder) VALUES (?, NULL, 0, ?)",
            arrayOf<Any>(disturbance.code, index),
        )
    }
}
