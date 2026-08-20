package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Autopsy Auturvy choice 589.
 */
object AutopsyChoiceSync {

    const val CHOICE_ID = 589
    const val AUTOPSY_TWEEZERS = 5687

    fun apply(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (!html.contains("dissolve in the caustic fluid")) return false
        consumeItem(AUTOPSY_TWEEZERS, 1)
        preferences.setInt(
            "autopsyTweezersUsed",
            preferences.getInt("autopsyTweezersUsed", 0) + 1,
        )
        return true
    }
}
