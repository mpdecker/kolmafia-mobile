package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Fire! I... have made... fire! choice 595.
 */
object FireStartingKitChoiceSync {

    const val CHOICE_ID = 595
    const val CSA_FIRE_STARTING_KIT = 5739

    fun apply(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        preferences.setBoolean("_fireStartingKitUsed", true)
        if (html.contains("rubbing the two stupid sticks together") ||
            html.contains("pile the sticks up on top of the briefcase")
        ) {
            consumeItem(CSA_FIRE_STARTING_KIT, 1)
        }
        return true
    }
}
