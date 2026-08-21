package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Take your Pills choice 1395.
 */
object PillKeeperChoiceSync {

    const val CHOICE_ID = 1395

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        adjustSpleen: (Int) -> Unit = {},
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (!html.contains("day's worth of pills")) return false
        var changed = false
        if (decision in 1..8) {
            if (!preferences.getBoolean("_freePillKeeperUsed", false)) {
                preferences.setBoolean("_freePillKeeperUsed", true)
            } else {
                adjustSpleen(3)
            }
            changed = true
        }
        if (decision == 3) {
            preferences.setBoolean("noncombatForcerActive", true)
            changed = true
        }
        return changed
    }
}
