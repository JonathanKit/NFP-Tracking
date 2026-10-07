package de.easynfp.app.export

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import de.easynfp.app.R
import de.easynfp.app.text.Formats
import de.easynfp.backup.CsvWriter
import de.easynfp.data.DataContainer
import de.easynfp.data.EvaluationSnapshot
import de.easynfp.data.model.DayEntry
import de.easynfp.data.model.Disturbance
import de.easynfp.data.model.Intercourse
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** F-53: all day entries as CSV. */
object CsvExporter {
    suspend fun export(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val res = context.resources
        val container = DataContainer.get(context)
        val snapshot = container.evaluations.current()
        val disturbances = container.repository.disturbances.first().associateBy { it.id }
        val header = res.getStringArray(R.array.csv_header).toList()
        val rows = snapshot.entries.values.sortedBy { it.date }.map { row(res, it, snapshot, disturbances) }
        try {
            val stream = context.contentResolver.openOutputStream(uri, "wt") ?: return@withContext false
            stream.use { it.write(CsvWriter.write(header, rows).toByteArray(Charsets.UTF_8)) }
            true
        } catch (e: IOException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }

    /** One line per day, in the order of `csv_header`. */
    private fun row(
        res: Resources,
        entry: DayEntry,
        snapshot: EvaluationSnapshot,
        disturbances: Map<Long, Disturbance>,
    ): List<String> {
        fun yes(value: Boolean) = if (value) res.getString(R.string.yes) else ""
        return listOf(
            entry.date.toString(),
            snapshot.evaluationFor(entry.date)?.cycle?.cycleDayOf(entry.date)?.toString().orEmpty(),
            entry.temperature?.let { Formats.temperatureValue(it) }.orEmpty(),
            entry.measurementTime?.toString().orEmpty(),
            yes(entry.temperatureExcluded),
            entry.mucusSensation?.let { Formats.sensation(res, it) }.orEmpty(),
            entry.mucusAppearance?.let { Formats.appearance(res, it) }.orEmpty(),
            entry.mucusCategory?.symbol.orEmpty(),
            entry.bleeding?.let { Formats.bleeding(res, it) }.orEmpty(),
            entry.disturbanceIds.mapNotNull { disturbances[it] }.joinToString(
                ", ",
            ) { Formats.disturbanceName(res, it) },
            when (entry.intercourse) {
                Intercourse.PROTECTED -> res.getString(R.string.intercourse_protected)
                Intercourse.UNPROTECTED -> res.getString(R.string.intercourse_unprotected)
                null -> ""
            },
            yes(entry.mittelschmerz),
            yes(entry.breastSymptom),
            entry.otherBleeding?.let { Formats.intensity(res, it) }.orEmpty(),
            entry.cramps?.let { Formats.intensity(res, it) }.orEmpty(),
            entry.nausea?.let { Formats.intensity(res, it) }.orEmpty(),
            yes(entry.diarrhea),
            yes(entry.vomiting),
            entry.note.orEmpty(),
        )
    }
}
