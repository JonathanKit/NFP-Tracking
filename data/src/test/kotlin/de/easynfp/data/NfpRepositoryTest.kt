package de.easynfp.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.easynfp.backup.BackupFormat
import de.easynfp.data.backup.BackupMapper
import de.easynfp.data.db.NfpDatabase
import de.easynfp.data.demo.DemoData
import de.easynfp.data.model.CycleRecord
import de.easynfp.data.model.DayEntry
import de.easynfp.data.model.Intensity
import de.easynfp.data.model.Intercourse
import de.easynfp.data.model.PredefinedDisturbance
import de.easynfp.rules.model.Bleeding
import de.easynfp.rules.model.CycleOverrides
import de.easynfp.rules.model.MucusAppearance
import de.easynfp.rules.model.MucusSensation
import de.easynfp.rules.model.Temperature
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NfpRepositoryTest {
    private lateinit var db: NfpDatabase
    private lateinit var repository: NfpRepository
    private val clock = Clock.fixed(Instant.parse("2026-10-05T06:30:00Z"), ZoneOffset.UTC)
    private val day = LocalDate.of(2026, 10, 5)

    @Before
    fun setUp() {
        db = NfpDatabase.build(ApplicationProvider.getApplicationContext(), name = null)
        repository = NfpRepository(db, clock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `F-10 predefined disturbances are seeded`() = runBlocking {
        val disturbances = repository.disturbances.first()
        assertEquals(PredefinedDisturbance.entries.toList(), disturbances.map { it.predefined })
    }

    @Test
    fun `day entry round trip including disturbances`() = runBlocking {
        val alcohol = repository.disturbances.first().first { it.predefined == PredefinedDisturbance.ALCOHOL }
        val entry = DayEntry(
            date = day,
            temperature = Temperature(3655),
            measurementTime = LocalTime.of(6, 30),
            temperatureExcluded = true,
            mucusSensation = MucusSensation.MOIST,
            mucusAppearance = MucusAppearance.THICK,
            bleeding = Bleeding.SPOTTING,
            intercourse = Intercourse.PROTECTED,
            mittelschmerz = true,
            otherBleeding = Intensity.LIGHT,
            cramps = Intensity.HEAVY,
            nausea = Intensity.MEDIUM,
            diarrhea = true,
            vomiting = true,
            note = "Notiz",
            disturbanceIds = setOf(alcohol.id),
        )
        repository.saveDay(entry)
        assertEquals(entry, repository.getDay(day))
        assertEquals(listOf(entry), repository.dayEntries.first())

        repository.saveDay(entry.copy(disturbanceIds = emptySet()))
        assertEquals(emptySet(), repository.getDay(day)?.disturbanceIds)
    }

    @Test
    fun `empty entry is deleted`() = runBlocking {
        repository.saveDay(DayEntry(day, note = "x"))
        repository.saveDay(DayEntry(day))
        assertNull(repository.getDay(day))
    }

    @Test
    fun `F-21 saving the temperature keeps the other fields`() = runBlocking {
        repository.saveDay(DayEntry(day, mucusSensation = MucusSensation.DRY))
        repository.saveTemperature(day, Temperature(3640), LocalTime.of(6, 45))
        val stored = repository.getDay(day)
        assertEquals(Temperature(3640), stored?.temperature)
        assertEquals(MucusSensation.DRY, stored?.mucusSensation)
    }

    @Test
    fun `F-03 latest temperature before a date`() = runBlocking {
        repository.saveTemperature(day.minusDays(3), Temperature(3630), LocalTime.of(6, 0))
        repository.saveDay(DayEntry(day.minusDays(1), note = "ohne Temperatur"))
        assertEquals(Temperature(3630), repository.latestTemperatureBefore(day))
        assertNull(repository.latestTemperatureBefore(day.minusDays(3)))
    }

    @Test
    fun `F-08 period starts`() = runBlocking {
        repository.setPeriodStart(day, true)
        repository.setPeriodStart(day, true)
        assertEquals(listOf(CycleRecord(day)), repository.cycles.first())
        repository.setPeriodStart(day, false)
        assertTrue(repository.cycles.first().isEmpty())
    }

    @Test
    fun `F-09 own disturbances`() = runBlocking {
        val id = repository.addDisturbance(" Yoga ")
        val yoga = repository.disturbances.first().first { it.id == id }
        assertEquals("Yoga", yoga.customName)
        repository.updateDisturbance(yoga.copy(archived = true))
        assertTrue(repository.disturbances.first().first { it.id == id }.archived)
    }

    @Test
    fun `F-50 backup round trip restores everything`() = runBlocking {
        val stress = repository.disturbances.first().first { it.predefined == PredefinedDisturbance.STRESS }
        repository.setPeriodStart(day.minusDays(20), true)
        repository.updateCycle(
            CycleRecord(day.minusDays(20), excludedFromStatistics = true, CycleOverrides(mucusPeak = day.minusDays(5))),
        )
        repository.saveDay(DayEntry(day, Temperature(3655), disturbanceIds = setOf(stress.id)))
        repository.saveDay(
            DayEntry(
                day.minusDays(1),
                mucusSensation = MucusSensation.WET,
                mucusAppearance = MucusAppearance.LIQUID,
                cramps = Intensity.LIGHT,
                vomiting = true,
            ),
        )
        repository.saveDay(DayEntry(day.minusDays(2), diarrhea = true))
        val original = repository.snapshotForBackup()

        val bytes = BackupFormat.encode(
            BackupMapper.toBackup(original, emptyMap(), "test", clock.instant()),
            "pw".toCharArray(),
        )
        repository.deleteAll()
        assertTrue(repository.dayEntries.first().isEmpty())

        val restored = BackupMapper.fromBackup(BackupFormat.decode(bytes, "pw".toCharArray()))
        repository.replaceAll(restored)
        assertEquals(original, repository.snapshotForBackup())
    }

    @Test
    fun `F-55 delete all keeps the predefined disturbances`() = runBlocking {
        repository.addDisturbance("Eigene")
        repository.saveDay(DayEntry(day, Temperature(3655)))
        repository.deleteAll()
        assertTrue(repository.dayEntries.first().isEmpty())
        assertEquals(PredefinedDisturbance.entries.size, repository.disturbances.first().size)
    }

    @Test
    fun `F-48 demo data gives evaluated cycles and a forecast`() = runBlocking {
        val content = DemoData.generate(day)
        repository.replaceAll(content)
        assertEquals(content.days.size, repository.dayEntries.first().size)
        val snapshot = EvaluationSnapshot.compute(day, repository.cycles.first(), repository.dayEntries.first())
        assertEquals(DemoData.COMPLETED_CYCLES + 1, snapshot.evaluations.size)
        assertEquals(DemoData.CURRENT_CYCLE_DAY, snapshot.current?.cycle?.cycleDayOf(day))
        val evaluated = snapshot.evaluations.count { it.temperatureCompleted != null }
        assertTrue(evaluated >= DemoData.COMPLETED_CYCLES - 2, "only $evaluated cycles evaluated")
        assertTrue(snapshot.evaluations.count { it.infertileFromEveningOfDay != null } >= DemoData.COMPLETED_CYCLES - 3)
        assertTrue(snapshot.forecast != null)
        // Same date, same data.
        assertEquals(content, DemoData.generate(day))
    }

    @Test
    fun `F-40 evaluation snapshot follows the data`() = runBlocking {
        repository.setPeriodStart(day.minusDays(2), true)
        val evaluations = EvaluationRepository(repository, clock)
        val snapshot = evaluations.current()
        assertEquals(day, snapshot.today)
        assertEquals(3, snapshot.current?.dayCount)
    }
}
