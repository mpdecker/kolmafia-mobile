package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.updateQuestData] Daily Dungeon room increment.
 */
object DailyDungeonCombatSync {

    const val THE_DAILY_DUNGEON = 325
    const val PREF = "_lastDailyDungeonRoom"

    private val CHAMBER_PATTERN = Regex("""chamber <b>#(\d+)</b>""")

    fun apply(adventureId: String, preferences: Preferences?): Boolean {
        if (preferences == null) return false
        if (adventureId.toIntOrNull() != THE_DAILY_DUNGEON) return false
        preferences.setInt(PREF, preferences.getInt(PREF, 0) + 1)
        return true
    }

    /**
     * Desktop [FightRequest] fight-start chamber parse: `_lastDailyDungeonRoom = N-1`.
     */
    fun applyFightStart(html: String, preferences: Preferences?): Boolean {
        if (preferences == null) return false
        val round = CHAMBER_PATTERN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return false
        preferences.setInt(PREF, round - 1)
        return true
    }
}
