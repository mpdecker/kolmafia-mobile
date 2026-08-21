package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Prepare your Meal (BW Apron) choice 1518.
 */
object BwApronChoiceSync {

    const val CHOICE_ID = 1518

    const val MEAL_KIT = 11472
    const val MEALS_PREF = "bwApronMealsEaten"

    fun apply(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (!html.contains("You cook and quickly consume your")) return false
        consumeItem(MEAL_KIT, 1)
        val eaten = preferences.getInt(MEALS_PREF, -1)
        if (eaten >= 0) {
            preferences.setInt(MEALS_PREF, eaten + 1)
        }
        return true
    }
}
