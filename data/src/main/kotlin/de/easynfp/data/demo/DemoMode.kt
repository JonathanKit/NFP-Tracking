package de.easynfp.data.demo

import android.content.Context
import de.easynfp.data.LiveClock
import de.easynfp.data.NfpRepository
import de.easynfp.data.db.NfpDatabase
import java.time.LocalDate

/**
 * Demo mode (settings): the app works on a separate database filled with generated cycles, so the
 * screens can be tried with a lot of data. The real database is not opened, changed or deleted.
 *
 * The flag is read synchronously when the [de.easynfp.data.DataContainer] is created, so switching
 * requires restarting the app process.
 */
object DemoMode {
    private const val PREFS = "easynfp_mode"
    private const val KEY_DEMO = "demo"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DEMO, false)

    /** Fills a fresh demo database with cycles up to [today] and switches to it on the next start. */
    suspend fun enable(context: Context, today: LocalDate = LocalDate.now(LiveClock)) {
        val app = context.applicationContext
        app.deleteDatabase(NfpDatabase.DEMO_NAME)
        val database = NfpDatabase.build(app, NfpDatabase.DEMO_NAME)
        try {
            NfpRepository(database, LiveClock).replaceAll(DemoData.generate(today))
        } finally {
            database.close()
        }
        setFlag(app, true)
    }

    /** Switches back to the real data on the next start and removes the demo database. */
    fun disable(context: Context) {
        val app = context.applicationContext
        setFlag(app, false)
        app.deleteDatabase(NfpDatabase.DEMO_NAME)
    }

    private fun setFlag(context: Context, enabled: Boolean) {
        // commit() instead of apply(): the process is restarted right afterwards.
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_DEMO, enabled).commit()
    }
}
