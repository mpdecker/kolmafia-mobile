package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.JourneymanDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [JourneyManager] hub — journeyman zone/skill map, turn-threshold tracking,
 * path gates, skill-learn sync (Phases 3396–3410).
 */
object JourneyManager {

    val TURN_THRESHOLDS = intArrayOf(4, 8, 12, 16, 20, 24)

    fun isJourneymanPath(ascensionPath: AscensionPath?): Boolean =
        ascensionPath == AscensionPath.JOURNEYMAN

    fun zoneNames(): List<String> = JourneymanDatabase.zoneNames

    fun skillsForZone(locationName: String, characterClass: CharacterClass): Array<String?>? =
        JourneymanDatabase.skillsForZone(locationName, characterClass)

    fun skillIndexForTurns(turns: Int): Int? =
        TURN_THRESHOLDS.indexOfFirst { it == turns }.takeIf { it >= 0 }

    fun nextSkillThreshold(currentTurns: Int): Int? =
        TURN_THRESHOLDS.firstOrNull { it > currentTurns }

    fun expectedSkillAtTurn(
        locationName: String,
        characterClass: CharacterClass,
        turns: Int,
    ): String? {
        val index = skillIndexForTurns(turns) ?: return null
        return JourneymanDatabase.skillsForZone(locationName, characterClass)?.getOrNull(index)
    }

    fun adventureIdForZone(locationName: String): Int? =
        AdventureDatabase.getByName(locationName)?.adventureId?.toIntOrNull()

    fun isJourneymanZone(locationName: String): Boolean =
        JourneymanDatabase.zoneNames.any { it.equals(locationName, ignoreCase = true) }

    /**
     * Desktop JourneyCommand unreachableZone parity for zodiac-gated journeyman zones.
     */
    fun isUnreachableForSign(locationName: String, signZone: String?): Boolean {
        val zoneName = AdventureDatabase.getByName(locationName)?.zoneName.orEmpty()
        return when (zoneName) {
            "MoxSign" -> !signZone.equals("gnomads", ignoreCase = true)
            "MusSign" -> !signZone.equals("knoll", ignoreCase = true)
            "Little Canadia" -> !signZone.equals("canadia", ignoreCase = true)
            else -> false
        }
    }

    fun recordAdventureTurn(
        locationName: String,
        turnsSpent: Int,
        characterClass: CharacterClass,
        preferences: Preferences?,
        sessionLog: (String) -> Unit = {},
    ) {
        if (!isJourneymanZone(locationName) || preferences == null) return
        val index = skillIndexForTurns(turnsSpent) ?: return
        val skill = expectedSkillAtTurn(locationName, characterClass, turnsSpent) ?: return
        val prefKey = journeymanTurnPref(locationName, index)
        if (preferences.getBoolean(prefKey, false)) return
        preferences.setBoolean(prefKey, true)
        sessionLog("Journeyman threshold ${TURN_THRESHOLDS[index]} in $locationName: expect \"$skill\"")
    }

    fun applySkillLearn(
        responseText: String,
        locationName: String,
        characterClass: CharacterClass,
        turnsSpent: Int,
        preferences: Preferences?,
        learnSkill: (Int) -> Unit = {},
    ): Boolean {
        if (!isJourneymanZone(locationName) || preferences == null) return false
        if (!responseText.contains("You acquire a skill", ignoreCase = true) &&
            !responseText.contains("NEW SKILL", ignoreCase = true)
        ) {
            return false
        }
        val expected = expectedSkillAtTurn(locationName, characterClass, turnsSpent)
            ?: return false
        val skillId = SkillDefinitionDatabase.getByName(expected)?.id ?: return false
        learnSkill(skillId)
        val index = skillIndexForTurns(turnsSpent) ?: return true
        preferences.setBoolean(journeymanTurnPref(locationName, index), true)
        return true
    }

    fun journeymanTurnPref(locationName: String, skillIndex: Int): String {
        val slug = locationName.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
        return "_journeyman_${slug}_skill${skillIndex + 1}"
    }
}
