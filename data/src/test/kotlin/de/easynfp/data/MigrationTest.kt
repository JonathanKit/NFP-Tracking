package de.easynfp.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.easynfp.data.db.Migrations
import de.easynfp.data.db.NfpDatabase
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

    private companion object {
        const val DB_NAME = "migration-test.db"
    }
}
