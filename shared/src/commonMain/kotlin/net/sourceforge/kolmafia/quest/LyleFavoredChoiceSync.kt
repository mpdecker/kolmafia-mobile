package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Lyle, Lyle, Crocodile Style choice 1309 —
 * `_lyleFavored` + candy-cane sword visit flag.
 */
object LyleFavoredChoiceSync {

    const val CHOICE_ID = 1309

    fun apply(
        choiceId: Int,
        preferences: Preferences?,
        hasCandyCaneSwordEquipped: Boolean = false,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        preferences.setBoolean("_lyleFavored", true)
        if (hasCandyCaneSwordEquipped) {
            preferences.setBoolean("_candyCaneSwordLyle", true)
        }
        return true
    }
}
