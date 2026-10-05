package de.easynfp.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import de.easynfp.data.model.Intercourse
import de.easynfp.rules.model.Bleeding
import de.easynfp.rules.model.MucusAppearance
import de.easynfp.rules.model.MucusSensation
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "day_entries")
data class DayEntryEntity(
    @PrimaryKey val date: LocalDate,
    val temperatureHundredths: Int?,
    val measurementTime: LocalTime?,
    val temperatureExcluded: Boolean,
    val mucusSensation: MucusSensation?,
    val mucusAppearance: MucusAppearance?,
    val bleeding: Bleeding?,
    val intercourse: Intercourse?,
    val mittelschmerz: Boolean,
    val breastSymptom: Boolean,
    val note: String?,
    /** Epoch milliseconds of the last change. */
    val updatedAt: Long,
)

@Entity(tableName = "cycles")
data class CycleEntity(
    @PrimaryKey val startDate: LocalDate,
    val excludedFromStatistics: Boolean,
    val overrideFirstHigherMeasurement: LocalDate?,
    val overrideMucusPeak: LocalDate?,
)

@Entity(tableName = "disturbances")
data class DisturbanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    /** Code of a predefined disturbance, null for own ones. */
    val predefinedCode: String?,
    val customName: String?,
    val archived: Boolean,
    val sortOrder: Int,
)

@Entity(
    tableName = "day_disturbances",
    primaryKeys = ["date", "disturbanceId"],
    foreignKeys = [
        ForeignKey(
            entity = DayEntryEntity::class,
            parentColumns = ["date"],
            childColumns = ["date"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = DisturbanceEntity::class,
            parentColumns = ["id"],
            childColumns = ["disturbanceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("disturbanceId")],
)
data class DayDisturbanceEntity(
    val date: LocalDate,
    val disturbanceId: Long,
)
