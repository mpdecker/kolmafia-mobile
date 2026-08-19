package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.updateQuestData] Mer-kin Colosseum round increment.
 */
object MerkinColosseumCombatSync {

    const val MERKIN_COLOSSEUM = 210

    private val COLOSSEUM_MONSTERS = setOf(
        "mer-kin balldodger",
        "mer-kin netdragger",
        "mer-kin bladeswitcher",
        "georgepaul, the balldodger",
        "johnringo, the netdragger",
        "ringogeorge, the bladeswitcher",
    )

    fun apply(
        adventureId: String,
        monster: String,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        if (adventureId.toIntOrNull() != MERKIN_COLOSSEUM) return false
        if (monster.trim().lowercase() !in COLOSSEUM_MONSTERS) return false
        val next = preferences.getInt("lastColosseumRoundWon", 0) + 1
        preferences.setInt("lastColosseumRoundWon", next)
        if (next == 15) {
            preferences.setBoolean("isMerkinGladiatorChampion", true)
            preferences.setString("merkinQuestPath", "gladiator")
        }
        return true
    }
}
