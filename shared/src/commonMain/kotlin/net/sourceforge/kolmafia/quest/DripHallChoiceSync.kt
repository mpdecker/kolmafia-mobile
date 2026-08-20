package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] The Hall in the Hall choice 1411.
 */
object DripHallChoiceSync {

    const val CHOICE_ID = 1411

    const val DRIPPY_STEIN = 10524
    const val ADVENTURES_PREF = "drippingHallAdventuresSinceAscension"
    const val ORBS_PREF = "drippyOrbsClaimed"

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        estimatedPoolSkill: Int = 0,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        when (decision) {
            1 -> {
                preferences.setBoolean("_drippingHallDoor1", true)
                if (html.contains("drippy orb")) {
                    preferences.setInt(ORBS_PREF, preferences.getInt(ORBS_PREF, 0) + 1)
                } else {
                    val known = preferences.getInt(ORBS_PREF, 0)
                    val min = estimatedPoolSkill / 20
                    preferences.setInt(ORBS_PREF, maxOf(known, min))
                }
            }
            2 -> preferences.setBoolean("_drippingHallDoor2", true)
            3 -> preferences.setBoolean("_drippingHallDoor3", true)
            4 -> {
                preferences.setBoolean("_drippingHallDoor4", true)
                if (html.contains("drippy pilsner")) {
                    consumeItem(DRIPPY_STEIN, 1)
                }
            }
            else -> return false
        }
        padAdventureSchedule(preferences)
        return true
    }

    /** Desktop pads dripping-hall adventure count onto a 12-turn schedule. */
    private fun padAdventureSchedule(preferences: Preferences) {
        var advs = preferences.getInt(ADVENTURES_PREF, 0) + 1
        preferences.setInt(ADVENTURES_PREF, advs)
        if (advs < 12) {
            preferences.setInt(ADVENTURES_PREF, 12)
            advs = 12
        }
        val mod = advs % 12
        if (mod != 0) {
            preferences.setInt(ADVENTURES_PREF, advs + (12 - mod))
        }
    }
}
