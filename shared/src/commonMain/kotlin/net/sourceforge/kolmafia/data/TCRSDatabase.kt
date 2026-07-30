package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Per-run Two Crazy Random Summer item name mappings. Mirrors desktop TCRSDatabase v1
 * (name lookup only; modifiers/size/quality parsed for future phases).
 */
object TCRSDatabase {

    data class TcrsEntry(
        val name: String,
        val size: Int = 0,
        val quality: String = "",
        val modifiers: String = "",
    )

    private val standardSignNames = setOf(
        "Mongoose", "Wallaby", "Vole",
        "Platypus", "Opossum", "Marmot",
        "Wombat", "Blender", "Packrat",
    )

    private var currentClassSign = ""
    private val tcrsMap = mutableMapOf<Int, TcrsEntry>()

    fun getTCRSName(itemId: Int): String {
        val entry = tcrsMap[itemId]
        return entry?.name ?: ItemDatabase.getById(itemId)?.name ?: ""
    }

    fun filename(className: String, signName: String, suffix: String = ""): String {
        if (!validate(className, signName)) return ""
        val classPart = className.replace(' ', '_')
        return "TCRS_${classPart}_${signName}${suffix}.txt"
    }

    fun prefKey(className: String, signName: String): String {
        if (!validate(className, signName)) return ""
        val classPart = className.replace(' ', '_')
        return "tcrs_${classPart}_${signName}"
    }

    fun validate(className: String, signName: String): Boolean {
        val cls = CharacterClass.entries.firstOrNull {
            it.displayName.equals(className, ignoreCase = true)
        } ?: return false
        if (!cls.isStandardClass) return false
        return standardSignNames.any { it.equals(signName, ignoreCase = true) }
    }

    fun parseFromText(text: String): Map<Int, TcrsEntry> {
        val map = linkedMapOf<Int, TcrsEntry>()
        for (line in text.lineSequence()) {
            val trimmed = line.trimEnd('\r', '\n').trimStart()
            if (trimmed.isEmpty() || trimmed.startsWith('#')) continue
            val cols = trimmed.split('\t')
            if (cols.size < 5) continue
            val itemId = cols[0].toIntOrNull() ?: continue
            val name = cols[1]
            val size = cols[2].toIntOrNull() ?: 0
            val quality = cols[3]
            val modifiers = cols[4]
            map[itemId] = TcrsEntry(name, size, quality, modifiers)
        }
        return map
    }

    fun load(className: String, signName: String, text: String?) {
        if (text.isNullOrBlank()) return
        tcrsMap.clear()
        tcrsMap.putAll(parseFromText(text))
        currentClassSign = "$className/$signName"
    }

    fun loadFromPreferences(className: String, signName: String, preferences: Preferences): Boolean {
        if (!validate(className, signName)) {
            reset()
            return false
        }
        val key = prefKey(className, signName)
        val text = preferences.getString(key, "")
        if (text.isBlank()) {
            reset()
            currentClassSign = "$className/$signName"
            return false
        }
        load(className, signName, text)
        return true
    }

    fun saveToPreferences(className: String, signName: String, preferences: Preferences): Boolean {
        if (!validate(className, signName)) return false
        val key = prefKey(className, signName)
        if (tcrsMap.isEmpty()) {
            preferences.setString(key, "")
            return true
        }
        val lines = tcrsMap.entries.sortedBy { it.key }.joinToString("\n") { (itemId, entry) ->
            listOf(
                itemId.toString(),
                entry.name,
                entry.size.toString(),
                entry.quality,
                entry.modifiers,
            ).joinToString("\t")
        }
        preferences.setString(key, lines)
        return true
    }

    fun reset() {
        currentClassSign = ""
        tcrsMap.clear()
    }

    internal fun registerForTest(itemId: Int, name: String) {
        tcrsMap[itemId] = TcrsEntry(name)
    }

    internal fun currentClassSignForTest(): String = currentClassSign

    internal fun mapSizeForTest(): Int = tcrsMap.size
}
