package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.updateQuestData] Haunted Kitchen drawer combat-win writer.
 */
object ManorDrawerCombatSync {

    const val HAUNTED_KITCHEN = 388
    const val BILLIARDS_KEY = 7301
    const val PREF = "manorDrawerCount"

    private val DRAWER_PATTERN = Regex("""search through <b>(\d+)</b> drawers""")

    fun apply(
        adventureId: String,
        html: String,
        preferences: Preferences?,
        hasItemId: (Int) -> Boolean = { false },
    ): Boolean {
        if (preferences == null) return false
        if (adventureId.toIntOrNull() != HAUNTED_KITCHEN) return false
        if (hasItemId(BILLIARDS_KEY)) return false
        val amount = DRAWER_PATTERN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
        preferences.setInt(PREF, preferences.getInt(PREF, 0) + amount)
        return true
    }
}
