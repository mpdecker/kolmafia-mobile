package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Drippy House on the Prairie choice 1406 —
 * bat unlock, truncheon→stake discard, trees adventure schedule pad.
 */
object DrippyTreeChoiceSync {

    const val CHOICE_ID = 1406

    const val DRIPPY_TRUNCHEON = 10442
    const val BATS_PREF = "drippyBatsUnlocked"
    const val ADVENTURES_PREF = "drippingTreesAdventuresSinceAscension"

    fun apply(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        discardItem: (Int) -> Unit = {},
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (html.contains("vile bat-things")) {
            preferences.setInt(BATS_PREF, preferences.getInt(BATS_PREF, 0) + 7)
        }
        if (html.contains("sharp stake")) {
            discardItem(DRIPPY_TRUNCHEON)
        }
        padAdventureSchedule(preferences)
        return true
    }

    /** Desktop pads dripping-trees adventure count onto a 16-then-every-15 schedule. */
    private fun padAdventureSchedule(preferences: Preferences) {
        var advs = preferences.getInt(ADVENTURES_PREF, 0)
        if (advs < 16) {
            preferences.setInt(ADVENTURES_PREF, 16)
            advs = 16
        }
        val mod = (advs - 1) % 15
        if (mod != 0) {
            preferences.setInt(ADVENTURES_PREF, advs + (15 - mod))
        }
    }
}
