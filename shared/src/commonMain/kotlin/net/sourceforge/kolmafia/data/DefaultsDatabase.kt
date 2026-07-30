package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Desktop preference defaults from [defaults.txt].
 * Mirrors [net.sourceforge.kolmafia.preferences.Preferences.initializeMaps].
 */
@OptIn(ExperimentalResourceApi::class)
object DefaultsDatabase {

    enum class Scope {
        GLOBAL,
        USER,
    }

    data class Entry(
        val scope: Scope,
        val name: String,
        val value: String,
        val attributes: Set<String> = emptySet(),
        val deprecationNotice: String = "",
    )

    private val entries = linkedMapOf<String, Entry>()
    private val resetOnAscensionInternal = mutableSetOf<String>()
    private val resetOnFightInternal = mutableSetOf<String>()
    private val legacyDailiesInternal = mutableSetOf<String>()
    private var loaded = false

    val isLoaded: Boolean get() = loaded
    val loadedEntryCount: Int get() = entries.size

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/defaults.txt").decodeToString()
        applyParse(parse(text))
        loaded = true
    }

    fun getString(name: String): String = entries[name]?.value ?: ""

    fun getBoolean(name: String): Boolean = getString(name).equals("true", ignoreCase = true)

    fun getInt(name: String): Int = getString(name).toIntOrNull() ?: 0

    fun has(name: String): Boolean = name in entries

    fun resetOnAscension(): Set<String> = resetOnAscensionInternal.toSet()

    fun resetOnFight(): Set<String> = resetOnFightInternal.toSet()

    fun legacyDailies(): Set<String> = legacyDailiesInternal.toSet()

    fun deprecationNotice(name: String): String? {
        val notice = entries[name]?.deprecationNotice
        return notice?.takeIf { it.isNotEmpty() }
    }

    internal fun parseForTest(text: String): ParseSnapshot = parse(text)

    internal fun injectForTest(snapshot: ParseSnapshot) {
        applyParse(snapshot)
        loaded = true
    }

    internal fun resetForTest() {
        entries.clear()
        resetOnAscensionInternal.clear()
        resetOnFightInternal.clear()
        legacyDailiesInternal.clear()
        loaded = false
    }

    data class ParseSnapshot(
        val entries: Map<String, Entry>,
        val resetOnAscension: Set<String>,
        val resetOnFight: Set<String>,
        val legacyDailies: Set<String>,
    )

    private fun applyParse(snapshot: ParseSnapshot) {
        entries.clear()
        entries.putAll(snapshot.entries)
        resetOnAscensionInternal.clear()
        resetOnAscensionInternal.addAll(snapshot.resetOnAscension)
        resetOnFightInternal.clear()
        resetOnFightInternal.addAll(snapshot.resetOnFight)
        legacyDailiesInternal.clear()
        legacyDailiesInternal.addAll(snapshot.legacyDailies)
    }

    private fun parse(text: String): ParseSnapshot {
        val parsedEntries = linkedMapOf<String, Entry>()
        val roa = mutableSetOf<String>()
        val rof = mutableSetOf<String>()
        val ld = mutableSetOf<String>()
        var skipVersionLine = true

        for (rawLine in text.lineSequence()) {
            val line = rawLine.trimEnd('\r', '\n')
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            if (skipVersionLine && trimmed.toIntOrNull() != null) {
                skipVersionLine = false
                continue
            }
            skipVersionLine = false

            val cols = line.split('\t')
            if (cols.size < 2) continue

            val scope = when (cols[0].trim().lowercase()) {
                "global" -> Scope.GLOBAL
                "user" -> Scope.USER
                else -> continue
            }
            val name = cols[1].trim()
            if (name.isEmpty()) continue

            val value = if (cols.size >= 3) cols[2] else ""
            val attributeString = if (cols.size >= 4) cols[3].trim() else ""
            val (attributes, deprecationNotice) = parseAttributes(attributeString)

            parsedEntries[name] = Entry(
                scope = scope,
                name = name,
                value = value,
                attributes = attributes,
                deprecationNotice = deprecationNotice,
            )

            if ("roa" in attributes) roa.add(name)
            if ("rof" in attributes) rof.add(name)
            if ("ld" in attributes) ld.add(name)
        }

        return ParseSnapshot(
            entries = parsedEntries,
            resetOnAscension = roa,
            resetOnFight = rof,
            legacyDailies = ld,
        )
    }

    private fun parseAttributes(attributeString: String): Pair<Set<String>, String> {
        if (attributeString.isEmpty()) return emptySet<String>() to ""

        val attributes = mutableSetOf<String>()
        var deprecationNotice = ""

        for (rawAttr in attributeString.split(',')) {
            val attr = rawAttr.trim()
            if (attr.isEmpty()) continue
            if (attr.startsWith("deprecated")) {
                attributes.add("deprecated")
                val parts = attr.split(':', limit = 2)
                if (parts.size == 2) {
                    deprecationNotice = parts[1].trim()
                }
            } else {
                attributes.add(attr)
            }
        }

        return attributes to deprecationNotice
    }
}
