package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Devil some Candy choice 1544.
 */
object CandyDevilerChoiceSync {

    const val CHOICE_ID = 1544

    private val ITEM_FIELD = Regex("""(?:^|[?&])a=(\d+)""", RegexOption.IGNORE_CASE)

    fun apply(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        choiceUrl: String = "",
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (!html.contains("You place your candy in the deviler")) return false
        preferences.setInt(
            "_candyEggsDeviled",
            preferences.getInt("_candyEggsDeviled", 0) + 1,
        )
        val itemId = ITEM_FIELD.find(choiceUrl)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (itemId != null) {
            consumeItem(itemId, 1)
        }
        return true
    }
}
