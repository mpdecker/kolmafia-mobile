package net.sourceforge.kolmafia.modifiers

/**
 * Ed servant type catalog. Mirrors desktop [EdServantData.SERVANTS] / typeToData fuzzy resolve.
 */
object ServantData {

    data class Servant(val type: String, val id: Int)

    private val SERVANTS = listOf(
        Servant("Cat", 1),
        Servant("Belly-Dancer", 2),
        Servant("Maid", 3),
        Servant("Bodyguard", 4),
        Servant("Scribe", 5),
        Servant("Priest", 6),
        Servant("Assassin", 7),
    )

    private val CANONICAL_TYPES = SERVANTS.map { canonicalType(it.type) }.sorted()

    fun canonicalType(type: String): String = type.trim().lowercase()

    fun resolve(name: String): Servant? {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.equals("none", ignoreCase = true)) return null

        SERVANTS.firstOrNull { it.type.equals(trimmed, ignoreCase = true) }?.let { return it }

        val search = canonicalType(trimmed)
        if (search.isEmpty()) return null

        CANONICAL_TYPES.firstOrNull { it == search }?.let { canonical ->
            return SERVANTS.firstOrNull { canonicalType(it.type) == canonical }
        }

        val wordStartMatches = SERVANTS.filter { substringMatches(canonicalType(it.type), search, wordStart = true) }
        if (wordStartMatches.size == 1) return wordStartMatches[0]

        val substringMatches = SERVANTS.filter { substringMatches(canonicalType(it.type), search, wordStart = false) }
        if (substringMatches.size == 1) return substringMatches[0]

        return null
    }

    fun isValid(name: String): Boolean = resolve(name) != null

    fun typeForId(id: Int): String? = SERVANTS.firstOrNull { it.id == id }?.type

    private fun substringMatches(name: String, search: String, wordStart: Boolean): Boolean {
        if (search.isEmpty()) return false
        var index = name.indexOf(search)
        while (index >= 0) {
            if (!wordStart || index == 0) return true
            index = name.indexOf(search, index + 1)
        }
        return false
    }
}
