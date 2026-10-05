package de.easynfp.data

import android.content.Context
import de.easynfp.data.db.NfpDatabase
import de.easynfp.data.settings.SettingsRepository
import java.time.Clock

/**
 * Manual dependency container for the data layer, shared by app and widget.
 * One instance per process, created lazily from the application context.
 */
class DataContainer private constructor(context: Context) {
    val clock: Clock = LiveClock
    val database: NfpDatabase = NfpDatabase.build(context)
    val repository: NfpRepository = NfpRepository(database, clock)
    val evaluations: EvaluationRepository = EvaluationRepository(repository, clock)
    val settings: SettingsRepository = SettingsRepository(context)

    companion object {
        @Volatile
        private var instance: DataContainer? = null

        fun get(context: Context): DataContainer = instance ?: synchronized(this) {
            instance ?: DataContainer(context.applicationContext).also { instance = it }
        }
    }
}
