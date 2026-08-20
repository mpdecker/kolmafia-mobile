package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Meatsmith choices 1059–1060.
 */
object MeatsmithChoiceSync {

    const val HELPING_MAKE_ENDS_MEAT = 1059
    const val TEMPORARILY_OUT_OF_SKELETONS = 1060
    const val MEATSMITH_CHECK = 8156
    const val SKELETON_KEY = 642

    fun apply(
        choiceId: Int,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        return when (choiceId) {
            HELPING_MAKE_ENDS_MEAT -> applyMeatsmith(html, questDatabase, preferences, consumeItem)
            TEMPORARILY_OUT_OF_SKELETONS -> {
                if (!html.contains("it snaps off")) return false
                consumeItem(SKELETON_KEY, 1)
                true
            }
            else -> false
        }
    }

    private fun applyMeatsmith(
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit,
    ): Boolean {
        if (questDatabase == null) return false
        when {
            html.contains("excitedly takes the check") -> {
                questDatabase.setProgress(Quest.MEATSMITH, QuestDatabase.FINISHED)
                consumeItem(MEATSMITH_CHECK, 1)
            }
            html.contains("skeleton store is right next door") || html.contains("I'll be here") -> {
                questDatabase.setProgress(Quest.MEATSMITH, QuestDatabase.STARTED)
                preferences?.setBoolean("skeletonStoreAvailable", true)
            }
            else -> return false
        }
        return true
    }
}
