package com.example.bnki.data

/**
 * Sehr einfache CSV-Serialisierung für Karten: Spalten front;back;hint;tags.
 * Felder werden bei Bedarf in Anführungszeichen gesetzt (RFC-4180-ähnlich).
 */
object CsvIo {
    private const val SEP = ';'
    private val HEADER = "front${SEP}back${SEP}hint${SEP}tags"

    data class Row(val front: String, val back: String, val hint: String, val tags: String)

    fun export(cards: List<Card>): String {
        val sb = StringBuilder()
        sb.append(HEADER).append('\n')
        for (c in cards) {
            sb.append(escape(c.front)).append(SEP)
                .append(escape(c.back)).append(SEP)
                .append(escape(c.hint)).append(SEP)
                .append(escape(c.tags)).append('\n')
        }
        return sb.toString()
    }

    fun parse(text: String): List<Row> {
        val rows = mutableListOf<Row>()
        val fields = parseFields(text)
        // Kopfzeile überspringen, wenn sie wie unser Header aussieht.
        var start = 0
        if (fields.isNotEmpty() && fields[0].size >= 1 && fields[0][0].equals("front", ignoreCase = true)) {
            start = 1
        }
        for (i in start until fields.size) {
            val row = fields[i]
            if (row.all { it.isBlank() }) continue
            rows.add(
                Row(
                    front = row.getOrElse(0) { "" },
                    back = row.getOrElse(1) { "" },
                    hint = row.getOrElse(2) { "" },
                    tags = row.getOrElse(3) { "" },
                )
            )
        }
        return rows
    }

    private fun escape(field: String): String {
        val needsQuote = field.contains(SEP) || field.contains('"') || field.contains('\n') || field.contains('\r')
        if (!needsQuote) return field
        return "\"" + field.replace("\"", "\"\"") + "\""
    }

    /** Zerlegt den gesamten Text in Zeilen von Feldern, respektiert Quotes. */
    private fun parseFields(text: String): List<List<String>> {
        val result = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                inQuotes -> {
                    if (ch == '"') {
                        if (i + 1 < text.length && text[i + 1] == '"') {
                            field.append('"'); i++
                        } else inQuotes = false
                    } else field.append(ch)
                }
                ch == '"' -> inQuotes = true
                ch == SEP -> { current.add(field.toString()); field.clear() }
                ch == '\n' -> {
                    current.add(field.toString()); field.clear()
                    result.add(current); current = mutableListOf()
                }
                ch == '\r' -> { /* ignorieren */ }
                else -> field.append(ch)
            }
            i++
        }
        if (field.isNotEmpty() || current.isNotEmpty()) {
            current.add(field.toString())
            result.add(current)
        }
        return result
    }
}
