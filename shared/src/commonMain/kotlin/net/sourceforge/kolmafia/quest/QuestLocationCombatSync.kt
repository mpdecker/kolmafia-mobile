package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.updateQuestData] 8-Bit bonus-turn and villain-lair writers.
 */
object QuestLocationCombatSync {

    const val SUPER_VILLAIN_LAIR = 495
    const val FUNGUS_PLAINS = 563
    const val HEROS_FIELD = 564
    const val VANYAS_CASTLE = 565
    const val MEGALO_CITY = 566

    private val BIT_REALM = setOf(FUNGUS_PLAINS, HEROS_FIELD, VANYAS_CASTLE, MEGALO_CITY)

    fun apply(
        adventureId: String,
        monster: String,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        val area = adventureId.toIntOrNull() ?: return false
        if (area in BIT_REALM) {
            preferences.setInt("8BitBonusTurns", preferences.getInt("8BitBonusTurns", 0) + 1)
            return true
        }
        if (area != SUPER_VILLAIN_LAIR) return false
        return when (monster.trim().lowercase()) {
            "villainous minion" -> {
                preferences.setInt(
                    "_villainLairProgress",
                    preferences.getInt("_villainLairProgress", 0) + 1,
                )
                true
            }
            "villainous villain" -> {
                preferences.setInt("_villainLairProgress", 999)
                preferences.setInt(
                    "bondVillainsDefeated",
                    preferences.getInt("bondVillainsDefeated", 0) + 1,
                )
                true
            }
            else -> false
        }
    }
}
