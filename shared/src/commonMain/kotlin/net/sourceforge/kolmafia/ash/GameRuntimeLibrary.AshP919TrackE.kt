package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.platform.UserDataFileIO
import net.sourceforge.kolmafia.utilities.CharacterEntities
import net.sourceforge.kolmafia.utilities.PHPLCG
import net.sourceforge.kolmafia.utilities.PHPMTRandom

/**
 * AshP919–927 Track E — Matcher / file / URL / PHP random.
 *
 * Phase 919: create_matcher + find/group/start/end/replace_first/replace_all/reset/group_count
 * Phase 920: xpath (stub — no KMP XPath lib)
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

    // ── Phase 920: xpath stub ──────────────────────────────────────
    val stringArrayType = AggregateType(AshType.INT, AshType.STRING)
    regFn(scope, "xpath", stringArrayType,
        listOf("html" to AshType.STRING, "xpath" to AshType.STRING)) { _, _ ->
        // TODO: No KMP XPath library available; returns empty string[]
        AggregateValue(stringArrayType)
    }

    // ── Phase 921: file_to_map / map_to_file ───────────────────────
    regFn(scope, "file_to_map", AshType.BOOLEAN,
        listOf("filename" to AshType.STRING, "result" to AshType.AGGREGATE)) { _, args ->
        val filename = args[0].toString()
        val agg = args[1] as? AggregateValue ?: return@regFn AshValue.FALSE
        val text = UserDataFileIO.readText(filename) ?: return@regFn AshValue.FALSE
        val aggType = agg.type
        text.lineSequence().filter { it.isNotBlank() }.forEach { line ->
            val parts = line.split('\t', limit = 2)
            val key = AshValue(aggType.indexType, coerceFileValue(parts[0], aggType.indexType))
            val value = if (parts.size > 1)
                AshValue(aggType.dataType, coerceFileValue(parts[1], aggType.dataType))
            else aggType.dataType.defaultValue()
            agg[key] = value
        }
        AshValue.TRUE
    }

    regFn(scope, "map_to_file", AshType.BOOLEAN,
        listOf("map" to AshType.AGGREGATE, "filename" to AshType.STRING)) { _, args ->
        val agg = args[0] as? AggregateValue ?: return@regFn AshValue.FALSE
        val filename = args[1].toString()
        val sb = StringBuilder()
        for ((k, v) in agg.map) {
            sb.append(k.toString())
            sb.append('\t')
            sb.appendLine(v.toString())
        }
        try {
            UserDataFileIO.writeText(filename, sb.toString())
            AshValue.TRUE
        } catch (_: Exception) {
            AshValue.FALSE
        }
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
    // xpath remains an empty-array stub (no KMP XPath lib — explicit non-goal).
    regFn(scope, "php_seed", AshType.VOID, listOf("seed" to AshType.INT)) { _, args ->
        phpLcgRandom = PHPLCG(args[0].toLong())
        AshValue.VOID
    }
    regFn(scope, "php_rand", AshType.INT, emptyList()) { _, _ ->
        val rng = phpLcgRandom ?: PHPLCG(1L).also { phpLcgRandom = it }
        AshValue.of(rng.rand().toLong())
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
