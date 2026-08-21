package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.platform.UserDataFileIO

/**
 * AshP975–980 Track O — String / file / buffer residuals.
 *
 * Phase 975: contains_text, starts_with, ends_with
 * Phase 976: join_strings, group_string, split_string(string,string,int)
 * Phase 977: buffer_to_file, file_to_buffer
 * Phase 978: file_to_array, append_to_file
 * Phase 979: to_json, form_field, form_fields(string)
 * Phase 980: is_integer, is_float
 */
internal fun GameRuntimeLibrary.registerAshP975TrackOBatch(scope: AshScope) {
    // ── Phase 975: contains_text / starts_with / ends_with ───────────
    regFn(scope, "contains_text", AshType.BOOLEAN,
        listOf("source" to AshType.STRING, "search" to AshType.STRING)) { _, args ->
        AshValue.of(args[0].toString().contains(args[1].toString(), ignoreCase = true))
    }

    regFn(scope, "starts_with", AshType.BOOLEAN,
        listOf("source" to AshType.STRING, "prefix" to AshType.STRING)) { _, args ->
        AshValue.of(args[0].toString().startsWith(args[1].toString()))
    }

    regFn(scope, "ends_with", AshType.BOOLEAN,
        listOf("source" to AshType.STRING, "suffix" to AshType.STRING)) { _, args ->
        AshValue.of(args[0].toString().endsWith(args[1].toString()))
    }

    // ── Phase 976: join_strings / group_string / split_string 3-arg ──
    regFn(scope, "join_strings", AshType.STRING,
        listOf("pieces" to AshType.AGGREGATE, "glue" to AshType.STRING)) { _, args ->
        val agg = args[0] as? AggregateValue ?: return@regFn AshValue.EMPTY_STRING
        val glue = args[1].toString()
        AshValue.of(agg.map.values.joinToString(glue) { it.toString() })
    }

    val stringArrayType = AggregateType(AshType.INT, AshType.STRING)

    regFn(scope, "group_string", AggregateType(AshType.INT, stringArrayType),
        listOf("source" to AshType.STRING, "pattern" to AshType.STRING)) { _, args ->
        val outerType = AggregateType(AshType.INT, stringArrayType)
        val result = AggregateValue(outerType)
        val source = args[0].toString()
        val regex = Regex(args[1].toString())
        val matches = regex.findAll(source)
        var idx = 0
        for (match in matches) {
            val inner = AggregateValue(stringArrayType)
            for ((g, gv) in match.groupValues.withIndex()) {
                inner[AshValue.of(g)] = AshValue.of(gv)
            }
            result[AshValue.of(idx)] = inner
            idx++
        }
        result
    }

    regFn(scope, "split_string", stringArrayType,
        listOf("source" to AshType.STRING, "delimiter" to AshType.STRING,
            "limit" to AshType.INT)) { _, args ->
        val result = AggregateValue(stringArrayType)
        val parts = args[0].toString().split(Regex(args[1].toString()), args[2].toLong().toInt())
        for ((i, part) in parts.withIndex()) {
            result[AshValue.of(i)] = AshValue.of(part)
        }
        result
    }

    // ── Phase 977: buffer_to_file / file_to_buffer ──────────────────
    regFn(scope, "buffer_to_file", AshType.BOOLEAN,
        listOf("data" to AshType.BUFFER, "filename" to AshType.STRING)) { _, args ->
        val data = args[0].toString()
        val filename = args[1].toString()
        try {
            UserDataFileIO.writeText(filename, data)
            AshValue.TRUE
        } catch (_: Exception) {
            AshValue.FALSE
        }
    }

    regFn(scope, "file_to_buffer", AshType.BUFFER,
        listOf("filename" to AshType.STRING)) { _, args ->
        val filename = args[0].toString()
        val text = UserDataFileIO.readText(filename) ?: ""
        AshValue(AshType.BUFFER, StringBuilder(text))
    }

    // ── Phase 978: file_to_array / append_to_file ───────────────────
    regFn(scope, "file_to_array", stringArrayType,
        listOf("filename" to AshType.STRING)) { _, args ->
        val result = AggregateValue(stringArrayType)
        val filename = args[0].toString()
        val text = UserDataFileIO.readText(filename) ?: ""
        text.lineSequence().forEachIndexed { i, line ->
            result[AshValue.of(i)] = AshValue.of(line)
        }
        result
    }

    regFn(scope, "append_to_file", AshType.VOID,
        listOf("data" to AshType.BUFFER, "filename" to AshType.STRING)) { _, args ->
        val data = args[0].toString()
        val filename = args[1].toString()
        try {
            val existing = UserDataFileIO.readText(filename) ?: ""
            UserDataFileIO.writeText(filename, existing + data)
        } catch (_: Exception) { /* best-effort */ }
        AshValue.VOID
    }

    // ── Phase 979: to_json / form_field / form_fields(string) ────────
    regFn(scope, "to_json", AshType.STRING, listOf("val" to AshType.AGGREGATE)) { _, args ->
        val agg = args[0] as? AggregateValue ?: return@regFn AshValue.of("{}")
        val sb = StringBuilder()
        sb.append('{')
        var first = true
        for ((k, v) in agg.map) {
            if (!first) sb.append(',')
            sb.append('"').append(escapeJson(k.toString())).append("\":")
            sb.append('"').append(escapeJson(v.toString())).append('"')
            first = false
        }
        sb.append('}')
        AshValue.of(sb.toString())
    }

    regFn(scope, "form_field", AshType.STRING, listOf("key" to AshType.STRING)) { _, args ->
        val key = args[0].toString()
        val fields = net.sourceforge.kolmafia.session.ChoiceCombatAshState.lastFormFields
        AshValue.of(fields[key].orEmpty())
    }

    val stringToString = AggregateType(AshType.STRING, AshType.STRING)
    regFn(scope, "form_fields", stringToString, listOf("url" to AshType.STRING)) { _, args ->
        val result = AggregateValue(stringToString)
        val url = args[0].toString()
        val q = url.indexOf('?')
        if (q >= 0) {
            val query = url.substring(q + 1)
            for (pair in query.split('&')) {
                val eq = pair.indexOf('=')
                if (eq > 0) {
                    result[AshValue.of(pair.substring(0, eq))] = AshValue.of(pair.substring(eq + 1))
                }
            }
        }
        result
    }

    // ── Phase 980: is_integer / is_float ────────────────────────────
    regFn(scope, "is_integer", AshType.BOOLEAN, listOf("s" to AshType.STRING)) { _, args ->
        AshValue.of(args[0].toString().toLongOrNull() != null)
    }

    regFn(scope, "is_float", AshType.BOOLEAN, listOf("s" to AshType.STRING)) { _, args ->
        AshValue.of(args[0].toString().toDoubleOrNull() != null)
    }
}

private fun escapeJson(s: String): String = buildString {
    for (c in s) {
        when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(c)
        }
    }
}
