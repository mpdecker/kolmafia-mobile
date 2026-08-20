package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Time-Spinner choices 1195–1196.
 */
object TimeSpinnerChoiceSync {

    const val SPINNING = 1195
    const val RECENT_FIGHT = 1196
    const val MINUTES_PREF = "_timeSpinnerMinutesUsed"

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
        choiceUrl: String = "",
    ): Boolean {
        if (preferences == null) return false
        val delta = when (choiceId) {
            SPINNING -> when (decision) {
                3 -> 1
                4 -> 2
                else -> return false
            }
            RECENT_FIGHT -> {
                if (decision != 1) return false
                if (choiceUrl.contains("monid=0")) return false
                3
            }
            else -> return false
        }
        preferences.setInt(MINUTES_PREF, preferences.getInt(MINUTES_PREF, 0) + delta)
        return true
    }
}
