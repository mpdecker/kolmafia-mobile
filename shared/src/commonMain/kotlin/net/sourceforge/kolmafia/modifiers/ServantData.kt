package net.sourceforge.kolmafia.modifiers

/**
 * Ed servant type catalog. Mirrors desktop [EdServantData.SERVANTS] / typeToData fuzzy resolve.
 */
object ServantData {

    data class Servant(
        val type: String,
        val id: Int,
        val image: String = "edserv$id.gif",
        val level1Ability: String = "",
        val level7Ability: String = "",
        val level14Ability: String = "",
        val level21Ability: String = "",
    )

    private val SERVANTS = listOf(
        Servant("Cat", 1, level1Ability = "Gives unpleasant gifts", level7Ability = "Helps find items",
            level14Ability = "Lowers enemy stats", level21Ability = "Teaches you how to find items"),
        Servant("Belly-Dancer", 2, level1Ability = "Lowers enemy stats", level7Ability = "Restores MP",
            level14Ability = "Picks pockets", level21Ability = "Teaches you how to restore MP"),
        Servant("Maid", 3, level1Ability = "Helps find meat", level7Ability = "Attacks enemies",
            level14Ability = "Prevents enemy attacks", level21Ability = "Teaches you how to find meat"),
        Servant("Bodyguard", 4, level1Ability = "Prevents enemy attacks", level7Ability = "Attacks enemies",
            level14Ability = "Attacks enemies even when guarding", level21Ability = "Teaches you how to defend yourself"),
        Servant("Scribe", 5, level1Ability = "Improves stat gains", level7Ability = "Improves spell crit",
            level14Ability = "Improves spell damage", level21Ability = "Teaches you how to improve stat gains"),
        Servant("Priest", 6, level1Ability = "Attacks undead enemies", level7Ability = "Improves evocation spells",
            level14Ability = "Improves Ka drops", level21Ability = "Teaches you how to improve spell damage"),
        Servant("Assassin", 7, level1Ability = "Attacks enemies", level7Ability = "Lowers enemy stats",
            level14Ability = "Staggers enemies", level21Ability = "Teaches you how to improve physical attacks"),
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

    fun servantForId(id: Int): Servant? = SERVANTS.firstOrNull { it.id == id }

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
