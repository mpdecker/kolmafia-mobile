package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.platform.UserDataFileIO
import net.sourceforge.kolmafia.utilities.SimpleXPath
import net.sourceforge.kolmafia.utilities.CharacterEntities
import net.sourceforge.kolmafia.utilities.PHPLCG
import net.sourceforge.kolmafia.utilities.PHPMTRandom

/**
 * AshP919–927 Track E — Matcher / file / URL / PHP random.
 *
 * Phase 919: create_matcher + find/group/start/end/replace_first/replace_all/reset/group_count
 * Phase 920: xpath (minimal SimpleXPath for common KoL HTML patterns)
 * Phase 921: file_to_map / map_to_file (simple key/value via UserDataFileIO)
 * Phase 922: url_encode / url_decode
 * Phase 923: entity_encode / entity_decode
 * Phase 924: to_url(location) / to_wiki_url overloads
 * Phase 925: php_seed / php_rand / php_mt_rand
 */
internal fun GameRuntimeLibrary.registerAshP919TrackEBatch(scope: AshScope) {
    // ── Phase 919: Matcher functions ────────────────────────────────
    regFn(scope, "create_matcher", AshType.MATCHER,
        listOf("pattern" to AshType.STRING, "string" to AshType.STRING)) { _, args ->
        val pattern = args[0].toString()
        val input = args[1].toString()
        val regex = Regex(pattern, RegexOption.DOT_MATCHES_ALL)
        AshValue.matcher(AshMatcherState(regex, input))
    }

    regFn(scope, "find", AshType.BOOLEAN, listOf("m" to AshType.MATCHER)) { _, args ->
        val ms = args[0].content as? AshMatcherState ?: return@regFn AshValue.FALSE
        AshValue.of(ms.find())
    }

    regFn(scope, "group", AshType.STRING,
        listOf("m" to AshType.MATCHER, "group" to AshType.INT)) { _, args ->
        val ms = args[0].content as? AshMatcherState ?: return@regFn AshValue.EMPTY_STRING
        AshValue.of(ms.group(args[1].toLong().toInt()))
    }

    regFn(scope, "group", AshType.STRING, listOf("m" to AshType.MATCHER)) { _, args ->
        val ms = args[0].content as? AshMatcherState ?: return@regFn AshValue.EMPTY_STRING
        AshValue.of(ms.group(0))
    }

    regFn(scope, "start", AshType.INT,
        listOf("m" to AshType.MATCHER, "group" to AshType.INT)) { _, args ->
        val ms = args[0].content as? AshMatcherState ?: return@regFn AshValue.of(-1)
        AshValue.of(ms.start(args[1].toLong().toInt()).toLong())
    }

    regFn(scope, "start", AshType.INT, listOf("m" to AshType.MATCHER)) { _, args ->
        val ms = args[0].content as? AshMatcherState ?: return@regFn AshValue.of(-1)
        AshValue.of(ms.start(0).toLong())
    }

    regFn(scope, "end", AshType.INT,
        listOf("m" to AshType.MATCHER, "group" to AshType.INT)) { _, args ->
        val ms = args[0].content as? AshMatcherState ?: return@regFn AshValue.of(-1)
        AshValue.of(ms.end(args[1].toLong().toInt()).toLong())
    }

    regFn(scope, "end", AshType.INT, listOf("m" to AshType.MATCHER)) { _, args ->
        val ms = args[0].content as? AshMatcherState ?: return@regFn AshValue.of(-1)
        AshValue.of(ms.end(0).toLong())
    }

    regFn(scope, "replace_first", AshType.STRING,
        listOf("m" to AshType.MATCHER, "replacement" to AshType.STRING)) { _, args ->
        val ms = args[0].content as? AshMatcherState ?: return@regFn AshValue.EMPTY_STRING
        AshValue.of(ms.replaceFirst(args[1].toString()))
    }

    regFn(scope, "replace_all", AshType.STRING,
        listOf("m" to AshType.MATCHER, "replacement" to AshType.STRING)) { _, args ->
        val ms = args[0].content as? AshMatcherState ?: return@regFn AshValue.EMPTY_STRING
        AshValue.of(ms.replaceAll(args[1].toString()))
    }

    regFn(scope, "reset", AshType.MATCHER,
        listOf("m" to AshType.MATCHER, "input" to AshType.STRING)) { _, args ->
        val ms = args[0].content as? AshMatcherState ?: return@regFn args[0]
        ms.reset(args[1].toString())
        args[0]
    }

    regFn(scope, "reset", AshType.MATCHER, listOf("m" to AshType.MATCHER)) { _, args ->
        val ms = args[0].content as? AshMatcherState ?: return@regFn args[0]
        ms.reset()
        args[0]
    }

    regFn(scope, "group_count", AshType.INT, listOf("m" to AshType.MATCHER)) { _, args ->
        val ms = args[0].content as? AshMatcherState ?: return@regFn AshValue.ZERO
        AshValue.of(ms.groupCount().toLong())
    }

    // ── Phase 920: xpath ───────────────────────────────────────────
    val stringArrayType = AggregateType(AshType.INT, AshType.STRING)
    regFn(scope, "xpath", stringArrayType,
        listOf("html" to AshType.STRING, "xpath" to AshType.STRING)) { _, args ->
        val html = args[0].toString()
        val expression = args[1].toString()
        val results = SimpleXPath.evaluate(html, expression)
        val aggregate = AggregateValue(stringArrayType)
        results.forEachIndexed { index, value ->
            aggregate[AshValue.of(index.toLong())] = AshValue.of(value)
        }
        aggregate
    }

    // ── Phase 921 / 6481–6490: file_to_map / map_to_file ─────────────
    // Desktop 2-arg defaults compact=true; 3-arg toggles nested compact layout.
    fun loadFileToMap(filename: String, agg: AggregateValue, compact: Boolean): AshValue {
        val text = UserDataFileIO.readText(filename) ?: return AshValue.FALSE
        agg.map.clear()
        val aggType = agg.type
        text.lineSequence().forEachIndexed { lineIndex, rawLine ->
            val line = rawLine.trimEnd('\r')
            if (line.isBlank() || line.startsWith('#')) return@forEachIndexed
            val parts = line.split('\t')
            if (parts.size <= 1 && !compact) return@forEachIndexed
            try {
                writeCompactPath(agg, aggType, parts, compact)
            } catch (e: Exception) {
                throw ScriptException(
                    "Invalid line in data file \"$filename\" line ${lineIndex + 1}: \"$line\"" +
                        (e.message?.let { " ($it)" } ?: ""),
                )
            }
        }
        return AshValue.TRUE
    }

    fun dumpMapToFile(agg: AggregateValue, filename: String, compact: Boolean): AshValue {
        val sb = StringBuilder()
        dumpAggregate(agg, sb, compact, depth = 0)
        return try {
            UserDataFileIO.writeText(filename, sb.toString())
            AshValue.TRUE
        } catch (_: Exception) {
            AshValue.FALSE
        }
    }

    regFn(scope, "file_to_map", AshType.BOOLEAN,
        listOf("filename" to AshType.STRING, "result" to AshType.AGGREGATE)) { _, args ->
        val agg = args[1] as? AggregateValue ?: return@regFn AshValue.FALSE
        loadFileToMap(args[0].toString(), agg, compact = true)
    }
    regFn(scope, "file_to_map", AshType.BOOLEAN,
        listOf("filename" to AshType.STRING, "result" to AshType.AGGREGATE, "compact" to AshType.BOOLEAN)) { _, args ->
        val agg = args[1] as? AggregateValue ?: return@regFn AshValue.FALSE
        loadFileToMap(args[0].toString(), agg, args[2].toBoolean())
    }

    regFn(scope, "map_to_file", AshType.BOOLEAN,
        listOf("map" to AshType.AGGREGATE, "filename" to AshType.STRING)) { _, args ->
        val agg = args[0] as? AggregateValue ?: return@regFn AshValue.FALSE
        dumpMapToFile(agg, args[1].toString(), compact = true)
    }
    regFn(scope, "map_to_file", AshType.BOOLEAN,
        listOf("map" to AshType.AGGREGATE, "filename" to AshType.STRING, "compact" to AshType.BOOLEAN)) { _, args ->
        val agg = args[0] as? AggregateValue ?: return@regFn AshValue.FALSE
        dumpMapToFile(agg, args[1].toString(), args[2].toBoolean())
    }

    // ── Phase 922: url_encode / url_decode ─────────────────────────
    regFn(scope, "url_encode", AshType.STRING, listOf("string" to AshType.STRING)) { _, args ->
        AshValue.of(urlEncode(args[0].toString()))
    }

    regFn(scope, "url_decode", AshType.STRING, listOf("string" to AshType.STRING)) { _, args ->
        AshValue.of(urlDecode(args[0].toString()))
    }

    // ── Phase 923: entity_encode / entity_decode ───────────────────
    regFn(scope, "entity_encode", AshType.STRING, listOf("string" to AshType.STRING)) { _, args ->
        AshValue.of(CharacterEntities.escape(args[0].toString()))
    }

    regFn(scope, "entity_decode", AshType.STRING, listOf("string" to AshType.STRING)) { _, args ->
        AshValue.of(CharacterEntities.unescape(args[0].toString()))
    }

    // ── Phase 924: to_url / to_wiki_url ────────────────────────────
    regFn(scope, "to_url", AshType.STRING, listOf("loc" to AshType.LOCATION)) { _, args ->
        val name = args[0].toString()
        val zone = gameDatabase?.zone(name)
        val snarfblat = zone?.snarfblat ?: ""
        AshValue.of(if (snarfblat.isNotEmpty()) "adventure.php?snarfblat=$snarfblat" else "")
    }

    regFn(scope, "to_wiki_url", AshType.STRING, listOf("value" to AshType.STRING)) { _, args ->
        AshValue.of(wikiUrlFor(args[0].toString()))
    }
    regFn(scope, "to_wiki_url", AshType.STRING, listOf("value" to AshType.ITEM)) { _, args ->
        AshValue.of(wikiUrlFor(args[0].toString()))
    }
    regFn(scope, "to_wiki_url", AshType.STRING, listOf("value" to AshType.EFFECT)) { _, args ->
        AshValue.of(wikiUrlFor(args[0].toString()))
    }
    regFn(scope, "to_wiki_url", AshType.STRING, listOf("value" to AshType.SKILL)) { _, args ->
        AshValue.of(wikiUrlFor(args[0].toString()))
    }
    regFn(scope, "to_wiki_url", AshType.STRING, listOf("value" to AshType.FAMILIAR)) { _, args ->
        AshValue.of(wikiUrlFor(args[0].toString()))
    }
    regFn(scope, "to_wiki_url", AshType.STRING, listOf("value" to AshType.LOCATION)) { _, args ->
        AshValue.of(wikiUrlFor(args[0].toString()))
    }
    regFn(scope, "to_wiki_url", AshType.STRING, listOf("value" to AshType.MONSTER)) { _, args ->
        AshValue.of(wikiUrlFor(args[0].toString()))
    }

    // ── Phase 925 / 970–971: PHP random ─────────────────────────────
    // Desktop: php_seed/php_rand use LCG; php_mt_* use Mersenne Twister.
    // xpath remains a minimal SimpleXPath implementation (not full HtmlCleaner).
    regFn(scope, "php_seed", AshType.VOID, listOf("seed" to AshType.INT)) { _, args ->
        phpLcgRandom = PHPLCG(args[0].toLong())
        AshValue.VOID
    }
    regFn(scope, "php_rand", AshType.INT, emptyList()) { _, _ ->
        val rng = phpLcgRandom ?: PHPLCG(1L).also { phpLcgRandom = it }
        AshValue.of(rng.rand().toLong())
    }
    regFn(scope, "php_rand", AshType.INT,
        listOf("min" to AshType.INT, "max" to AshType.INT)) { _, args ->
        val min = args[0].toLong().toInt()
        val max = args[1].toLong().toInt()
        val rng = phpLcgRandom ?: PHPLCG(1L).also { phpLcgRandom = it }
        AshValue.of(rng.rand(min, max).toLong())
    }
    regFn(scope, "php_mt_rand", AshType.INT, emptyList()) { _, _ ->
        val rng = phpMtRandom ?: PHPMTRandom(1L).also { phpMtRandom = it }
        AshValue.of(rng.nextInt(0, Int.MAX_VALUE - 1).toLong())
    }
    regFn(scope, "php_mt_rand", AshType.INT,
        listOf("min" to AshType.INT, "max" to AshType.INT)) { _, args ->
        val min = args[0].toLong().toInt()
        val max = args[1].toLong().toInt()
        val rng = phpMtRandom ?: PHPMTRandom(1L).also { phpMtRandom = it }
        AshValue.of(rng.nextInt(min, max).toLong())
    }

    regFn(scope, "php_mt_seed", AshType.VOID, listOf("seed" to AshType.INT)) { _, args ->
        phpMtRandom = PHPMTRandom(args[0].toLong())
        AshValue.VOID
    }
}

private var GameRuntimeLibrary.phpMtRandom: PHPMTRandom?
    get() = phpMtRandomHolder
    set(value) { phpMtRandomHolder = value }

private var GameRuntimeLibrary.phpLcgRandom: PHPLCG?
    get() = phpLcgRandomHolder
    set(value) { phpLcgRandomHolder = value }

@Suppress("ObjectPropertyName")
private var phpMtRandomHolder: PHPMTRandom? = null

@Suppress("ObjectPropertyName")
private var phpLcgRandomHolder: PHPLCG? = null

private fun coerceFileValue(raw: String, type: AshType): Any? = when (type) {
    AshType.INT -> raw.toLongOrNull() ?: 0L
    AshType.FLOAT -> raw.toDoubleOrNull() ?: 0.0
    AshType.BOOLEAN -> raw.equals("true", ignoreCase = true)
    else -> raw
}

/**
 * Compact file layout writes tab-separated key paths ending in a leaf value.
 * Non-compact writes one key per line with nested aggregates indented by tabs.
 */
private fun writeCompactPath(
    root: AggregateValue,
    rootType: AggregateType,
    parts: List<String>,
    compact: Boolean,
) {
    if (parts.isEmpty()) return
    if (!compact) {
        // Non-compact: key\tvalue only for flat maps
        val key = AshValue(rootType.indexType, coerceFileValue(parts[0], rootType.indexType))
        val value = if (parts.size > 1)
            AshValue(rootType.dataType, coerceFileValue(parts[1], rootType.dataType))
        else rootType.dataType.defaultValue()
        root[key] = value
        return
    }
    var current: AggregateValue = root
    var currentType: AggregateType = rootType
    var index = 0
    while (index < parts.size) {
        val key = AshValue(currentType.indexType, coerceFileValue(parts[index], currentType.indexType))
        val remaining = parts.size - index - 1
        val dataType = currentType.dataType
        if (dataType is AggregateType && remaining > 1) {
            val nested = current.map[key] as? AggregateValue
                ?: AggregateValue(dataType).also { current[key] = it }
            current = nested
            currentType = dataType
            index++
            continue
        }
        val valueRaw = if (index + 1 < parts.size) parts[index + 1] else ""
        current[key] = when (dataType) {
            is AggregateType -> {
                // Nested map expecting further keys — store empty aggregate if leaf
                AggregateValue(dataType)
            }
            else -> AshValue(dataType, coerceFileValue(valueRaw, dataType))
        }
        return
    }
}

private fun dumpAggregate(agg: AggregateValue, sb: StringBuilder, compact: Boolean, depth: Int) {
    for ((k, v) in agg.map) {
        if (compact) {
            dumpCompact(k, v, sb, prefix = "")
        } else {
            repeat(depth) { sb.append('\t') }
            sb.append(k.toString())
            when (v) {
                is AggregateValue -> {
                    sb.appendLine()
                    dumpAggregate(v, sb, compact = false, depth = depth + 1)
                }
                else -> {
                    sb.append('\t')
                    sb.appendLine(v.toString())
                }
            }
        }
    }
}

private fun dumpCompact(key: AshValue, value: AshValue, sb: StringBuilder, prefix: String) {
    val path = if (prefix.isEmpty()) key.toString() else "$prefix\t${key}"
    when (value) {
        is AggregateValue -> {
            if (value.map.isEmpty()) {
                sb.append(path).appendLine()
            } else {
                for ((k, v) in value.map) dumpCompact(k, v, sb, path)
            }
        }
        else -> sb.append(path).append('\t').appendLine(value.toString())
    }
}

private fun urlEncode(s: String): String = buildString {
    for (c in s) {
        when {
            c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '-' || c == '_' || c == '.' || c == '~' -> append(c)
            c == ' ' -> append('+')
            else -> {
                val bytes = c.toString().encodeToByteArray()
                for (b in bytes) {
                    append('%')
                    append(((b.toInt() shr 4) and 0xF).digitToChar(16).uppercaseChar())
                    append((b.toInt() and 0xF).digitToChar(16).uppercaseChar())
                }
            }
        }
    }
}

private fun urlDecode(s: String): String {
    val bytes = mutableListOf<Byte>()
    var i = 0
    while (i < s.length) {
        when {
            s[i] == '%' && i + 2 < s.length -> {
                val hi = s[i + 1].digitToIntOrNull(16)
                val lo = s[i + 2].digitToIntOrNull(16)
                if (hi != null && lo != null) {
                    bytes.add(((hi shl 4) or lo).toByte())
                    i += 3
                } else {
                    bytes.add(s[i].code.toByte())
                    i++
                }
            }
            s[i] == '+' -> {
                bytes.add(' '.code.toByte())
                i++
            }
            else -> {
                bytes.addAll(s[i].toString().encodeToByteArray().toList())
                i++
            }
        }
    }
    return bytes.toByteArray().decodeToString()
}
