package de.easynfp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DayEntryDao {
    @Query("SELECT * FROM day_entries ORDER BY date")
    fun observeAll(): Flow<List<DayEntryEntity>>

    @Query("SELECT * FROM day_entries ORDER BY date")
    suspend fun getAll(): List<DayEntryEntity>

    @Query("SELECT * FROM day_entries WHERE date = :date")
    suspend fun get(date: LocalDate): DayEntryEntity?

    @Query(
        "SELECT * FROM day_entries WHERE date < :date AND temperatureHundredths IS NOT NULL " +
            "ORDER BY date DESC LIMIT 1",
    )
    suspend fun latestWithTemperatureBefore(date: LocalDate): DayEntryEntity?

    @Upsert
    suspend fun upsert(entry: DayEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<DayEntryEntity>)

    @Query("DELETE FROM day_entries WHERE date = :date")
    suspend fun delete(date: LocalDate)

    @Query("DELETE FROM day_entries")
    suspend fun deleteAll()

    @Query("SELECT * FROM day_disturbances")
    fun observeAllDisturbanceLinks(): Flow<List<DayDisturbanceEntity>>

    @Query("SELECT * FROM day_disturbances")
    suspend fun getAllDisturbanceLinks(): List<DayDisturbanceEntity>

    @Query("SELECT disturbanceId FROM day_disturbances WHERE date = :date")
    suspend fun disturbanceIds(date: LocalDate): List<Long>

    @Query("DELETE FROM day_disturbances WHERE date = :date")
    suspend fun clearDisturbances(date: LocalDate)

    @Query("DELETE FROM day_disturbances")
    suspend fun deleteAllDisturbanceLinks()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDisturbanceLinks(links: List<DayDisturbanceEntity>)
}

@Dao
interface CycleDao {
    @Query("SELECT * FROM cycles ORDER BY startDate")
    fun observeAll(): Flow<List<CycleEntity>>

    @Query("SELECT * FROM cycles ORDER BY startDate")
    suspend fun getAll(): List<CycleEntity>

    @Query("SELECT * FROM cycles WHERE startDate = :startDate")
    suspend fun get(startDate: LocalDate): CycleEntity?

    @Upsert
    suspend fun upsert(cycle: CycleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cycles: List<CycleEntity>)

    @Query("DELETE FROM cycles WHERE startDate = :startDate")
    suspend fun delete(startDate: LocalDate)

    @Query("DELETE FROM cycles")
    suspend fun deleteAll()
}

@Dao
interface DisturbanceDao {
    @Query("SELECT * FROM disturbances ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<DisturbanceEntity>>

    @Query("SELECT * FROM disturbances ORDER BY sortOrder, id")
    suspend fun getAll(): List<DisturbanceEntity>

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM disturbances")
    suspend fun maxSortOrder(): Int

    @Insert
    suspend fun insert(disturbance: DisturbanceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(disturbances: List<DisturbanceEntity>)

    @Update
    suspend fun update(disturbance: DisturbanceEntity)

    @Query("DELETE FROM disturbances")
    suspend fun deleteAll()
}
