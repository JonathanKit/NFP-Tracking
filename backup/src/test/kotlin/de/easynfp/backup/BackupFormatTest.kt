package de.easynfp.backup

import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class BackupFormatTest {

    private val sample = BackupData(
        createdAt = "2026-10-05T06:30:00Z",
        appVersion = "1.0.0",
        cycles = listOf(
            BackupCycle("2026-09-01"),
            BackupCycle("2026-09-29", excludedFromStatistics = true, overrideMucusPeak = "2026-10-12"),
        ),
        days = listOf(
            BackupDay(
                date = "2026-10-05",
                temperatureHundredths = 3655,
                measurementTime = "06:30",
                mucusSensation = "MOIST",
                mucusAppearance = "THICK",
                bleeding = null,
                note = "Äpfel; \"Zitat\"\nzweite Zeile",
                disturbanceIds = listOf(1, 3),
            ),
        ),
        disturbances = listOf(BackupDisturbance(1, key = "ALCOHOL"), BackupDisturbance(7, name = "Yoga")),
        settings = mapOf("reminderTime" to "06:15"),
    )

    @Test
    fun `plain round trip`() {
        val bytes = BackupFormat.encode(sample)
        assertFalse(BackupFormat.isEncrypted(bytes))
        assertEquals(sample, BackupFormat.decode(bytes))
    }

    @Test
    fun `encrypted round trip`() {
        val bytes = BackupFormat.encode(sample, "geheim".toCharArray())
        assertTrue(BackupFormat.isEncrypted(bytes))
        assertFalse(bytes.toString(Charsets.UTF_8).contains("3655"))
        assertEquals(sample, BackupFormat.decode(bytes, "geheim".toCharArray()))
    }

    @Test
    fun `encrypted backup needs the right password`() {
        val bytes = BackupFormat.encode(sample, "geheim".toCharArray())
        assertEquals(
            BackupError.PASSWORD_REQUIRED,
            assertFailsWith<BackupException> { BackupFormat.decode(bytes) }.error,
        )
        assertEquals(
            BackupError.WRONG_PASSWORD,
            assertFailsWith<BackupException> { BackupFormat.decode(bytes, "falsch".toCharArray()) }.error,
        )
    }

    @Test
    fun `empty password means no encryption`() {
        val bytes = BackupFormat.encode(sample, CharArray(0))
        assertFalse(BackupFormat.isEncrypted(bytes))
    }

    @Test
    fun `rejects foreign and broken files`() {
        assertEquals(
            BackupError.NOT_A_BACKUP,
            assertFailsWith<BackupException> { BackupFormat.decode("hello".toByteArray()) }.error,
        )
        assertEquals(
            BackupError.NOT_A_BACKUP,
            assertFailsWith<BackupException> { BackupFormat.decode("""{"app":"other"}""".toByteArray()) }.error,
        )
        assertEquals(
            BackupError.CORRUPT,
            assertFailsWith<BackupException> {
                BackupFormat.decode("""{"app":"easyNFP","formatVersion":1,"days":"x"}""".toByteArray())
            }.error,
        )
    }

    @Test
    fun `N-07 newer format versions are rejected instead of misread`() {
        val json = """{"app":"easyNFP","formatVersion":99,"createdAt":"x","appVersion":"9"}"""
        assertEquals(
            BackupError.UNSUPPORTED_VERSION,
            assertFailsWith<BackupException> { BackupFormat.decode(json.toByteArray()) }.error,
        )
    }

    @Test
    fun `N-07 a version 1 file written by hand stays importable`() {
        // Frozen example of format version 1. Never change this string: it guards compatibility.
        val v1 = """
            {
              "formatVersion": 1,
              "app": "easyNFP",
              "createdAt": "2026-10-05T06:30:00Z",
              "appVersion": "1.0.0",
              "cycles": [ { "startDate": "2026-09-29" } ],
              "days": [ { "date": "2026-09-29", "temperatureHundredths": 3650, "bleeding": "HEAVY" } ],
              "disturbances": [ { "id": 1, "key": "ALCOHOL" } ],
              "unknownFutureField": true
            }
        """.trimIndent()
        val data = BackupFormat.decode(v1.toByteArray())
        assertEquals("2026-09-29", data.cycles.single().startDate)
        assertEquals(3650, data.days.single().temperatureHundredths)
        assertEquals("HEAVY", data.days.single().bleeding)
        assertFalse(data.days.single().temperatureExcluded)
    }

    @Test
    fun `csv escaping`() {
        assertEquals("abc", CsvWriter.escape("abc"))
        assertEquals("\"a;b\"", CsvWriter.escape("a;b"))
        assertEquals("\"sag \"\"hallo\"\"\"", CsvWriter.escape("sag \"hallo\""))
        assertEquals("\"zwei\nZeilen\"", CsvWriter.escape("zwei\nZeilen"))
        val csv = CsvWriter.write(listOf("Datum", "Temperatur"), listOf(listOf("2026-10-05", "36,55")))
        assertTrue(csv.startsWith("\uFEFF"))
        assertContentEquals(listOf("\uFEFFDatum;Temperatur", "2026-10-05;36,55", ""), csv.split("\r\n"))
    }
}
