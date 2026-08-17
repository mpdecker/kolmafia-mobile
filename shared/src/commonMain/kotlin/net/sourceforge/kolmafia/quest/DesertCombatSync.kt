package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager] ARID_DESERT combat exploration increments.
 */
object DesertCombatSync {

    const val ARID_DESERT = 364
    const val UV_RESISTANT_COMPASS = 6729
    const val DOWSING_ROD = 7150
    const val SURVIVAL_KNIFE = 10903
    const val MELODRAMEDARY = 279
    const val ULTRAHYDRATED = 275

    data class DesertCombatContext(
        val hasEquipped: (Int) -> Boolean = { false },
        val hasEffect: (Int) -> Boolean = { false },
        val familiarId: Int = 0,
    )

    fun applyCombatWin(
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        adventureId: String,
        responseText: String,
        won: Boolean,
        context: DesertCombatContext = DesertCombatContext(),
    ): Boolean {
        if (preferences == null || !won) return false
        if (adventureId != ARID_DESERT.toString()) return false
        var changed = false
        val oasisJustUnlocked = responseText.contains("discover a verdant oasis")
        if (oasisJustUnlocked) {
            if (!preferences.getBoolean("oasisAvailable", false)) {
                preferences.setBoolean("oasisAvailable", true)
                changed = true
            } else {
                preferences.setBoolean("oasisAvailable", true)
            }
        }
        // Clingy monsters do not increment exploration
        if (!responseText.contains("Desert exploration")) return changed

        var explored = 1
        when {
            context.hasEquipped(UV_RESISTANT_COMPASS) -> explored += 1
            context.hasEquipped(DOWSING_ROD) -> explored += 2
        }
        if (context.hasEquipped(SURVIVAL_KNIFE) &&
            context.hasEffect(ULTRAHYDRATED) &&
            !oasisJustUnlocked
        ) {
            explored += 2
        }
        if (context.familiarId == MELODRAMEDARY) {
            explored += 1
        }
        if (preferences.getString("peteMotorbikeHeadlight", "") == "Blacklight Bulb") {
            explored += 2
        } else if (preferences.getBoolean("bondDesert", false) &&
            preferences.getInt("desertExploration", 0) > 0
        ) {
            explored += 2
        }
        return DesertVisitSync.incrementExploration(preferences, questDatabase, explored) || changed
    }
}
