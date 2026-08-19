package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Daily Dungeon choices 689–693.
 */
object DailyDungeonChoiceSync {

    const val FINAL_REWARD = 689
    const val FIRST_CHEST = 690
    const val SECOND_CHEST = 691
    const val I_WANNA_BE_A_DOOR = 692
    const val ALMOST_CERTAINLY_A_TRAP = 693
    const val SKELETON_KEY = 642
    const val ROOM_PREF = DailyDungeonCombatSync.PREF

    fun apply(
        choiceId: Int,
        html: String,
        decision: Int,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        val prefs = preferences ?: return false
        return when (choiceId) {
            FINAL_REWARD -> {
                if (!html.contains("claim your rightful reward")) return false
                prefs.setBoolean("dailyDungeonDone", true)
                prefs.setInt(ROOM_PREF, 15)
                true
            }
            FIRST_CHEST, SECOND_CHEST -> {
                val delta = if (decision == 2) 3 else 1
                prefs.setInt(ROOM_PREF, prefs.getInt(ROOM_PREF, 0) + delta)
                true
            }
            I_WANNA_BE_A_DOOR -> {
                if (html.contains("key breaks off in the lock")) {
                    consumeItem(SKELETON_KEY, 1)
                }
                if (decision != 8) {
                    prefs.setInt(ROOM_PREF, prefs.getInt(ROOM_PREF, 0) + 1)
                }
                true
            }
            ALMOST_CERTAINLY_A_TRAP -> {
                if (decision != 3) {
                    prefs.setInt(ROOM_PREF, prefs.getInt(ROOM_PREF, 0) + 1)
                }
                true
            }
            else -> false
        }
    }
}
