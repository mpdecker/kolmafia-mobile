package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Headless Nemesis state shared by cave, combat, and item response handlers.
 *
 * The desktop manager also decorates Relay pages; that UI behavior intentionally
 * remains outside the mobile port.  The preferences are kept compatible so a
 * session can be resumed by either client.
 */
object NemesisManager {
    val paperStripIds = listOf(3144, 4138, 4139, 4140, 4141, 4142, 4143, 4144)

    private val doorItems = mapOf(
        CharacterClass.SEAL_CLUBBER to listOf(37, 316, 2478),
        CharacterClass.TURTLE_TAMER to listOf(37, 316, 2477),
        CharacterClass.PASTAMANCER to listOf(560, 319, 579),
        CharacterClass.SAUCEROR to listOf(560, 319, 420),
        CharacterClass.DISCO_BANDIT to listOf(565, 1256),
        CharacterClass.ACCORDION_THIEF to listOf(565, 1256),
    )

    fun resetForAscension(preferences: Preferences, ascension: Int) {
        if (preferences.getInt("lastNemesisReset", -1) == ascension) return
        for (key in listOf("dbNemesisSkill1", "dbNemesisSkill2", "dbNemesisSkill3")) {
            preferences.setInt(key, 0)
        }
        for (key in listOf(
            "raveCombo1", "raveCombo2", "raveCombo3",
            "raveCombo4", "raveCombo5", "raveCombo6",
            "volcanoMaze1", "volcanoMaze2", "volcanoMaze3",
            "volcanoMaze4", "volcanoMaze5",
        )) {
            preferences.setString(key, "")
        }
        preferences.setInt("lastNemesisReset", ascension)
    }

    fun doorItem(characterClass: CharacterClass, door: Int): Int? =
        doorItems[characterClass]?.getOrNull(door - 1)

    fun parsePaperStripDescription(
        itemId: Int,
        html: String,
        preferences: Preferences,
    ): Boolean {
        if (itemId !in paperStripIds) return false
        val match = Regex(
            """title=["']A (.*?) tear["'].*?title=["']A (.*?) tear["'].*?<b>([A-Z]+)</b>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).find(html) ?: return false
        preferences.setString(
            "lastPaperStrip$itemId",
            "${match.groupValues[1]}:${match.groupValues[3]}:${match.groupValues[2]}",
        )
        return true
    }

    fun password(preferences: Preferences): String? {
        val strips = paperStripIds.map { id ->
            val parts = preferences.getString("lastPaperStrip$id", "").split(':')
            if (parts.size != 3 || parts.any { it.isBlank() }) return null
            Triple(parts[0], parts[1], parts[2])
        }
        val byLeft = strips.associateBy { it.first }
        val rightParts = strips.map { it.third }.toSet()
        val first = strips.firstOrNull { it.first !in rightParts } ?: return null
        val ordered = mutableListOf(first)
        repeat(strips.size - 1) {
            val next = byLeft[ordered.last().third] ?: return null
            ordered += next
        }
        if (ordered.map { it.first }.toSet().size != strips.size) return null
        return ordered.joinToString("") { it.second }
    }
}
