package net.sourceforge.kolmafia.modifiers

/**
 * VYKEA companion catalog and parsing. Mirrors desktop [VYKEACompanionData].
 */
object VykeaCompanionData {

    enum class VykeaType {
        NONE,
        BOOKSHELF,
        DRESSER,
        CEILING_FAN,
        COUCH,
        LAMP,
        DISHRACK,
    }

    enum class VykeaRune {
        NONE,
        FRENZY,
        BLOOD,
        LIGHTNING,
    }

    data class Companion(
        val type: VykeaType,
        val level: Int,
        val rune: VykeaRune,
        val name: String,
    ) {
        val modifiers: String
            get() = when (type) {
                VykeaType.COUCH -> "Meat Drop: +${level * 10}"
                VykeaType.LAMP -> "Item Drop: +${level * 10}"
                else -> ""
            }
    }

    val catalog: List<String> = listOf(
        "level 1 bookshelf",
        "level 1 frenzy bookshelf",
        "level 1 blood bookshelf",
        "level 1 lightning bookshelf",
        "level 2 bookshelf",
        "level 2 frenzy bookshelf",
        "level 2 blood bookshelf",
        "level 2 lightning bookshelf",
        "level 3 bookshelf",
        "level 3 frenzy bookshelf",
        "level 3 blood bookshelf",
        "level 3 lightning bookshelf",
        "level 4 bookshelf",
        "level 4 frenzy bookshelf",
        "level 4 blood bookshelf",
        "level 4 lightning bookshelf",
        "level 5 bookshelf",
        "level 5 frenzy bookshelf",
        "level 5 blood bookshelf",
        "level 5 lightning bookshelf",
        "level 1 ceiling fan",
        "level 1 frenzy ceiling fan",
        "level 1 blood ceiling fan",
        "level 1 lightning ceiling fan",
        "level 2 ceiling fan",
        "level 2 frenzy ceiling fan",
        "level 2 blood ceiling fan",
        "level 2 lightning ceiling fan",
        "level 3 ceiling fan",
        "level 3 frenzy ceiling fan",
        "level 3 blood ceiling fan",
        "level 3 lightning ceiling fan",
        "level 4 ceiling fan",
        "level 4 frenzy ceiling fan",
        "level 4 blood ceiling fan",
        "level 4 lightning ceiling fan",
        "level 5 ceiling fan",
        "level 5 frenzy ceiling fan",
        "level 5 blood ceiling fan",
        "level 5 lightning ceiling fan",
        "level 1 couch",
        "level 1 frenzy couch",
        "level 1 blood couch",
        "level 1 lightning couch",
        "level 2 couch",
        "level 2 frenzy couch",
        "level 2 blood couch",
        "level 2 lightning couch",
        "level 3 couch",
        "level 3 frenzy couch",
        "level 3 blood couch",
        "level 3 lightning couch",
        "level 4 couch",
        "level 4 frenzy couch",
        "level 4 blood couch",
        "level 4 lightning couch",
        "level 5 couch",
        "level 5 frenzy couch",
        "level 5 blood couch",
        "level 5 lightning couch",
        "level 1 dishrack",
        "level 1 frenzy dishrack",
        "level 1 blood dishrack",
        "level 1 lightning dishrack",
        "level 2 dishrack",
        "level 2 frenzy dishrack",
        "level 2 blood dishrack",
        "level 2 lightning dishrack",
        "level 3 dishrack",
        "level 3 frenzy dishrack",
        "level 3 blood dishrack",
        "level 3 lightning dishrack",
        "level 4 dishrack",
        "level 4 frenzy dishrack",
        "level 4 blood dishrack",
        "level 4 lightning dishrack",
        "level 5 dishrack",
        "level 5 frenzy dishrack",
        "level 5 blood dishrack",
        "level 5 lightning dishrack",
        "level 1 dresser",
        "level 1 frenzy dresser",
        "level 1 blood dresser",
        "level 1 lightning dresser",
        "level 2 dresser",
        "level 2 frenzy dresser",
        "level 2 blood dresser",
        "level 2 lightning dresser",
        "level 3 dresser",
        "level 3 frenzy dresser",
        "level 3 blood dresser",
        "level 3 lightning dresser",
        "level 4 dresser",
        "level 4 frenzy dresser",
        "level 4 blood dresser",
        "level 4 lightning dresser",
        "level 5 dresser",
        "level 5 frenzy dresser",
        "level 5 blood dresser",
        "level 5 lightning dresser",
        "level 1 lamp",
        "level 1 frenzy lamp",
        "level 1 blood lamp",
        "level 1 lightning lamp",
        "level 2 lamp",
        "level 2 frenzy lamp",
        "level 2 blood lamp",
        "level 2 lightning lamp",
        "level 3 lamp",
        "level 3 frenzy lamp",
        "level 3 blood lamp",
        "level 3 lightning lamp",
        "level 4 lamp",
        "level 4 frenzy lamp",
        "level 4 blood lamp",
        "level 4 lightning lamp",
        "level 5 lamp",
        "level 5 frenzy lamp",
        "level 5 blood lamp",
        "level 5 lightning lamp",
    )

    private val companionPattern = Regex(
        """ *(?:(.*?), the )?level ([12345])(?: (blood|frenzy|lightning))? (bookshelf|ceiling fan|couch|dishrack|dresser|lamp) *""",
        RegexOption.IGNORE_CASE,
    )

    fun fromString(string: String): Companion? {
        val match = companionPattern.find(string.lowercase()) ?: return null
        val name = match.groupValues[1]
        val level = match.groupValues[2].toIntOrNull() ?: return null
        val rune = runeFromString(match.groupValues[3])
        val type = typeFromString(match.groupValues[4]) ?: return null
        return Companion(type, level, rune, name)
    }

    fun resolve(string: String): String? {
        val trimmed = string.trim()
        if (trimmed.isEmpty() || trimmed.equals("none", ignoreCase = true)) return null
        fromString(trimmed)?.let { return trimmed }
        return catalog.firstOrNull { it.equals(trimmed, ignoreCase = true) }
    }

    fun isValid(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        if (fromString(trimmed) != null) return true
        return catalog.any { it.equals(trimmed, ignoreCase = true) }
    }

    fun companionFor(name: String): Companion? {
        val trimmed = name.trim()
        fromString(trimmed)?.let { return it }
        val catalogEntry = catalog.firstOrNull { it.equals(trimmed, ignoreCase = true) } ?: return null
        return fromString(catalogEntry)
    }

    private fun runeFromString(value: String): VykeaRune = when (value.lowercase()) {
        "frenzy" -> VykeaRune.FRENZY
        "blood" -> VykeaRune.BLOOD
        "lightning" -> VykeaRune.LIGHTNING
        else -> VykeaRune.NONE
    }

    private fun typeFromString(value: String): VykeaType? = when (value.lowercase()) {
        "bookshelf" -> VykeaType.BOOKSHELF
        "ceiling fan" -> VykeaType.CEILING_FAN
        "couch" -> VykeaType.COUCH
        "dishrack" -> VykeaType.DISHRACK
        "dresser" -> VykeaType.DRESSER
        "lamp" -> VykeaType.LAMP
        else -> null
    }
}
