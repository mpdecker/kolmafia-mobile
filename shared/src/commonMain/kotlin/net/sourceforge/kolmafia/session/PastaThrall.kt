package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences

/** Pasta thrall bind skills and pref helpers (subset of desktop PastaThrallData). */
object PastaThrall {

    val TYPES = listOf(
        "Vampieroghi",
        "Vermincelli",
        "Angel Hair Wisp",
        "Elbow Macaroni",
        "Penne Dreadful",
        "Lasagmbie",
        "Spice Ghost",
        "Spaghetti Elemental",
    )

    private val BIND_SKILL_IDS = mapOf(
        "Vampieroghi" to 3027,
        "Vermincelli" to 3029,
        "Angel Hair Wisp" to 3031,
        "Elbow Macaroni" to 3033,
        "Penne Dreadful" to 3035,
        "Lasagmbie" to 3037,
        "Spice Ghost" to 3039,
        "Spaghetti Elemental" to 3041,
    )

    fun bindSkillId(thrallName: String): Int? =
        BIND_SKILL_IDS.entries.firstOrNull { it.key.equals(thrallName, ignoreCase = true) }?.value

    fun prefKey(index: Int): String = "pastaThrall$index"

    fun parsePref(value: String): Pair<Int, String>? {
        val trimmed = value.trim()
        if (trimmed.isBlank() || trimmed.equals("roa", ignoreCase = true)) return null
        val comma = trimmed.indexOf(',')
        if (comma <= 0) return null
        val level = trimmed.substring(0, comma).trim().toIntOrNull() ?: return null
        val name = trimmed.substring(comma + 1).trim()
        if (name.isBlank()) return null
        return level to name
    }

    fun levelFromPref(preferences: Preferences, index: Int): Int? =
        parsePref(preferences.getString(prefKey(index), ""))?.first

    fun thrallLevel(preferences: Preferences, thrallName: String): Int {
        for (index in 1..8) {
            val parsed = parsePref(preferences.getString(prefKey(index), "")) ?: continue
            if (parsed.second.equals(thrallName, ignoreCase = true)) return parsed.first
        }
        return 1
    }

    fun formatTable(preferences: Preferences): String = buildString {
        for (index in 1..8) {
            val parsed = parsePref(preferences.getString(prefKey(index), ""))
            if (parsed != null) {
                appendLine("${TYPES.getOrNull(index - 1) ?: "Thrall $index"}: level ${parsed.first}")
            }
        }
    }.trim()
}
