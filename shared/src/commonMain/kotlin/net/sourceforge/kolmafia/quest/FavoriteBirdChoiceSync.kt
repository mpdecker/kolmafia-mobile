package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] New Favorite Bird? choice 1399.
 * Defers DebugDatabase.readEffectDescriptionText.
 */
object FavoriteBirdChoiceSync {

    const val CHOICE_ID = 1399
    const val VISIT_FAVORITE_BIRD_SKILL_ID = 190

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
        learnSkill: (Int) -> Unit = {},
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        preferences.setInt("_birdsSoughtToday", 6)
        if (decision == 1) {
            val bird = preferences.getString("_birdOfTheDay", "")
            preferences.setString("yourFavoriteBird", bird)
            learnSkill(VISIT_FAVORITE_BIRD_SKILL_ID)
        }
        return true
    }
}
