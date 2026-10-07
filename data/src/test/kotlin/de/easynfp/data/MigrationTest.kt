package de.easynfp.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.easynfp.data.db.DatabaseSafety
import de.easynfp.data.db.Migrations
import de.easynfp.data.db.NfpDatabase
import de.easynfp.data.model.DayEntry
import de.easynfp.data.model.Intercourse
import de.easynfp.rules.model.Bleeding
import de.easynfp.rules.model.MucusAppearance
import de.easynfp.rules.model.MucusSensation
import de.easynfp.rules.model.Temperature
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * N-07: every schema version must be reachable by migrations without data loss.
 * When adding version N, add a test that creates version N-1 with data, migrates and checks the data.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NfpDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun `schema of the current version matches the committed schema file`() {
        helper.createDatabase(DB_NAME, 1).close()
        helper.runMigrationsAndValidate(DB_NAME, NfpDatabase.VERSION, true, *Migrations.ALL).close()
    }

    @Test
    fun `version 1 data is readable`() {
        helper.createDatabase(DB_NAME, 1).use { db ->
            db.execSQL(
                "INSERT INTO day_entries (date, temperatureHundredths, measurementTime, temperatureExcluded, " +
                    "mucusSensation, mucusAppearance, bleeding, intercourse, mittelschmerz, breastSymptom, note, " +
                    "updatedAt) VALUES ('2026-10-05', 3655, '06:30', 0, 'MOIST', 'THICK', NULL, NULL, 0, 0, NULL, 0)",
            )
        }
        helper.runMigrationsAndValidate(DB_NAME, NfpDatabase.VERSION, true, *Migrations.ALL).use { db ->
            db.query("SELECT temperatureHundredths FROM day_entries WHERE date = '2026-10-05'").use { cursor ->
                check(cursor.moveToFirst())
                check(cursor.getInt(0) == 3655)
            }
        }
    }

    @Test
    fun `N-07 migration 1 to 2 keeps all values and adds empty symptoms`() {
        helper.createDatabase(DB_NAME, 1).use { db ->
            db.execSQL(
                "INSERT INTO day_entries (date, temperatureHundredths, measurementTime, temperatureExcluded, " +
                    "mucusSensation, mucusAppearance, bleeding, intercourse, mittelschmerz, breastSymptom, note, " +
                    "updatedAt) VALUES ('2026-10-04', 3640, '06:15', 1, 'WET_SLIPPERY', 'CLEAR_STRETCHY', 'HEAVY', " +
                    "'UNPROTECTED', 1, 1, 'Notiz', 42)",
            )
            db.execSQL("INSERT INTO cycles VALUES ('2026-10-01', 1, '2026-10-03', '2026-10-02')")
            db.execSQL(
                "INSERT INTO disturbances (id, predefinedCode, customName, archived, sortOrder) " +
                    "VALUES (20, NULL, 'Yoga', 1, 9)",
            )
            db.execSQL("INSERT INTO day_disturbances VALUES ('2026-10-04', 20)")
        }
        helper.runMigrationsAndValidate(DB_NAME, 2, true, Migrations.MIGRATION_1_2).close()

        // Read through Room with the current entities: nothing may be lost.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.databaseBuilder(context, NfpDatabase::class.java, helperDatabasePath())
            .addMigrations(*Migrations.ALL)
            .build()
        try {
            val repository = NfpRepository(database, java.time.Clock.systemUTC())
            runBlocking {
                val entry = repository.getDay(LocalDate.parse("2026-10-04"))
                assertEquals(
                    DayEntry(
                        date = LocalDate.parse("2026-10-04"),
                        temperature = Temperature(3640),
                        measurementTime = LocalTime.of(6, 15),
                        temperatureExcluded = true,
                        mucusSensation = MucusSensation.WET_SLIPPERY,
                        mucusAppearance = MucusAppearance.CLEAR_STRETCHY,
                        bleeding = Bleeding.HEAVY,
                        intercourse = Intercourse.UNPROTECTED,
                        mittelschmerz = true,
                        breastSymptom = true,
                        note = "Notiz",
                        disturbanceIds = setOf(20L),
                    ),
                    entry,
                )
                val cycle = repository.cycles.first().single()
                assertEquals(true, cycle.excludedFromStatistics)
                assertEquals(LocalDate.parse("2026-10-02"), cycle.overrides.mucusPeak)
                assertEquals("Yoga", repository.disturbances.first().single { it.id == 20L }.customName)
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun `N-07 the old database file is copied before an update migrates it`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(NfpDatabase.NAME)
        // Simulate the database of an older app version.
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(NfpDatabase.NAME), null).use { db ->
            db.execSQL("CREATE TABLE marker (value TEXT)")
            db.version = 1
        }
        DatabaseSafety.copyBeforeMigration(context, NfpDatabase.NAME, targetVersion = 2)
        val copy = File(context.getDatabasePath(NfpDatabase.NAME).path + ".v1.bak")
        assertTrue(copy.exists())
        // No copy when the version is already current.
        copy.delete()
        DatabaseSafety.copyBeforeMigration(context, NfpDatabase.NAME, targetVersion = 1)
        assertFalse(copy.exists())
        context.deleteDatabase(NfpDatabase.NAME)
    }

    private fun helperDatabasePath(): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getDatabasePath(DB_NAME).path

    private companion object {
        const val DB_NAME = "migration-test.db"
    }
}
