package de.easynfp.data

import androidx.room.withTransaction
import de.easynfp.data.db.CycleEntity
import de.easynfp.data.db.DayDisturbanceEntity
import de.easynfp.data.db.DisturbanceEntity
import de.easynfp.data.db.NfpDatabase
import de.easynfp.data.db.toEntity
import de.easynfp.data.db.toModel
import de.easynfp.data.model.CycleRecord
import de.easynfp.data.model.DayEntry
import de.easynfp.data.model.Disturbance
import de.easynfp.data.model.PredefinedDisturbance
import de.easynfp.rules.model.Temperature
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Single access point to the stored data. All writes are transactional. */
class NfpRepository(
    private val db: NfpDatabase,
    private val clock: Clock,
) {
    private val dayDao = db.dayEntryDao()
    private val cycleDao = db.cycleDao()
    private val disturbanceDao = db.disturbanceDao()

    val dayEntries: Flow<List<DayEntry>> =
        combine(dayDao.observeAll(), dayDao.observeAllDisturbanceLinks()) { entries, links ->
            val idsByDate = links.groupBy({ it.date }, { it.disturbanceId })
            entries.map { it.toModel(idsByDate[it.date].orEmpty().toSet()) }
        }

    val cycles: Flow<List<CycleRecord>> = cycleDao.observeAll().map { list -> list.map { it.toModel() } }

    val disturbances: Flow<List<Disturbance>> = disturbanceDao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun getDay(date: LocalDate): DayEntry? = dayDao.get(date)?.toModel(dayDao.disturbanceIds(date).toSet())

    /** F-13: stores (or deletes, if empty) the entry of a day. */
    suspend fun saveDay(entry: DayEntry) {
        db.withTransaction {
            dayDao.clearDisturbances(entry.date)
            if (entry.isEmpty) {
                dayDao.delete(entry.date)
            } else {
                dayDao.upsert(entry.toEntity(clock.millis()))
                dayDao.insertDisturbanceLinks(entry.disturbanceIds.map { DayDisturbanceEntity(entry.date, it) })
            }
        }
    }

    /** F-21: stores temperature and time, keeping everything else recorded for that day. */
    suspend fun saveTemperature(date: LocalDate, temperature: Temperature, time: LocalTime) {
        db.withTransaction {
            val existing = getDay(date) ?: DayEntry(date)
            saveDay(existing.copy(temperature = temperature, measurementTime = time))
        }
    }

    /** F-03: the value used to preset the temperature input. */
    suspend fun latestTemperatureBefore(date: LocalDate): Temperature? =
        dayDao.latestWithTemperatureBefore(date)?.temperatureHundredths?.let(::Temperature)

    /** F-08: marks or unmarks [date] as first day of a period. */
    suspend fun setPeriodStart(date: LocalDate, isStart: Boolean) {
        db.withTransaction {
            if (isStart) {
                if (cycleDao.get(date) == null) cycleDao.upsert(CycleEntity(date, false, null, null))
            } else {
                cycleDao.delete(date)
            }
        }
    }

    /** F-42, F-46 */
    suspend fun updateCycle(record: CycleRecord) {
        cycleDao.upsert(record.toEntity())
    }

    /** F-09 */
    suspend fun addDisturbance(name: String): Long = db.withTransaction {
        disturbanceDao.insert(
            DisturbanceEntity(
                id = 0,
                predefinedCode = null,
                customName = name.trim(),
                archived = false,
                sortOrder = disturbanceDao.maxSortOrder() + 1,
            ),
        )
    }

    /** F-09: rename or archive. */
    suspend fun updateDisturbance(disturbance: Disturbance) {
        disturbanceDao.update(disturbance.toEntity())
    }

    suspend fun snapshotForBackup(): BackupContent = db.withTransaction {
        val links = dayDao.getAllDisturbanceLinks().groupBy({ it.date }, { it.disturbanceId })
        BackupContent(
            days = dayDao.getAll().map { it.toModel(links[it.date].orEmpty().toSet()) },
            cycles = cycleDao.getAll().map { it.toModel() },
            disturbances = disturbanceDao.getAll().map { it.toModel() },
        )
    }

    /** Replaces all data with [content] (backup import, F-50). */
    suspend fun replaceAll(content: BackupContent) {
        db.withTransaction {
            clearTables()
            disturbanceDao.insertAll(content.disturbances.map { it.toEntity() })
            cycleDao.insertAll(content.cycles.map { it.toEntity() })
            val now = clock.millis()
            dayDao.insertAll(content.days.filterNot { it.isEmpty }.map { it.toEntity(now) })
            val knownIds = content.disturbances.map { it.id }.toSet()
            dayDao.insertDisturbanceLinks(
                content.days.flatMap { day ->
                    day.disturbanceIds.filter { it in knownIds }.map { DayDisturbanceEntity(day.date, it) }
                },
            )
        }
    }

    /** F-55: deletes everything and restores the predefined disturbances. */
    suspend fun deleteAll() {
        db.withTransaction {
            clearTables()
            disturbanceDao.insertAll(
                PredefinedDisturbance.entries.mapIndexed { index, predefined ->
                    DisturbanceEntity(0, predefined.code, null, archived = false, sortOrder = index)
                },
            )
        }
    }

    private suspend fun clearTables() {
        dayDao.deleteAllDisturbanceLinks()
        dayDao.deleteAll()
        cycleDao.deleteAll()
        disturbanceDao.deleteAll()
    }
}

/** All user data, as exported to and imported from a backup. */
data class BackupContent(
    val days: List<DayEntry>,
    val cycles: List<CycleRecord>,
    val disturbances: List<Disturbance>,
)
