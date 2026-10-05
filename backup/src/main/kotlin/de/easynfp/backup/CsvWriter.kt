package de.easynfp.backup

/**
 * Minimal CSV writer for the export of all day entries (F-53).
 *
 * Uses `;` as separator and a UTF-8 byte order mark, which spreadsheet applications in German
 * locales open correctly. Fields are quoted when necessary (RFC 4180 style).
 */
public object CsvWriter {
    public const val SEPARATOR: Char = ';'
    private const val BOM = "\uFEFF"

    public fun write(header: List<String>, rows: List<List<String>>): String = buildString {
        append(BOM)
        appendRow(header)
        rows.forEach { appendRow(it) }
    }

    private fun StringBuilder.appendRow(fields: List<String>) {
        fields.forEachIndexed { index, field ->
            if (index > 0) append(SEPARATOR)
            append(escape(field))
        }
        append("\r\n")
    }

    public fun escape(field: String): String {
        val needsQuotes = field.any { it == SEPARATOR || it == '"' || it == '\n' || it == '\r' } ||
            field.startsWith(' ') || field.endsWith(' ')
        return if (needsQuotes) "\"" + field.replace("\"", "\"\"") + "\"" else field
    }
}
