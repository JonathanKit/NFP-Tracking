package de.easynfp.app.export

import android.content.Context
import android.net.Uri
import de.easynfp.app.R
import de.easynfp.app.text.Formats
import de.easynfp.backup.CsvWriter
import de.easynfp.data.DataContainer
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
        val rows = snapshot.entries.values.sortedBy { it.date }.map { entry ->
            val evaluation = snapshot.evaluationFor(entry.date)
            listOf(
                entry.date.toString(),
                evaluation?.cycle?.cycleDayOf(entry.date)?.toString().orEmpty(),
                entry.temperature?.let { Formats.temperatureValue(it) }.orEmpty(),
                entry.measurementTime?.toString().orEmpty(),
                if (entry.temperatureExcluded) res.getString(R.string.yes) else "",
                entry.mucusSensation?.let { Formats.sensation(res, it) }.orEmpty(),
                entry.mucusAppearance?.let { Formats.appearance(res, it) }.orEmpty(),
                entry.mucusCategory?.symbol.orEmpty(),
                entry.bleeding?.let { Formats.bleeding(res, it) }.orEmpty(),
                entry.disturbanceIds.mapNotNull { disturbances[it] }
                    .joinToString(", ") { Formats.disturbanceName(res, it) },
                when (entry.intercourse) {
                    Intercourse.PROTECTED -> res.getString(R.string.intercourse_protected)
                    Intercourse.UNPROTECTED -> res.getString(R.string.intercourse_unprotected)
                    null -> ""
                },
                if (entry.mittelschmerz) res.getString(R.string.yes) else "",
                if (entry.breastSymptom) res.getString(R.string.yes) else "",
                entry.note.orEmpty(),
            )
        }
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
}
