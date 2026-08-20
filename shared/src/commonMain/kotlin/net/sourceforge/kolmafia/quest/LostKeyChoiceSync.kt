package net.sourceforge.kolmafia.quest

/**
 * Desktop [ChoiceControl] A Lost Room choice 594.
 */
object LostKeyChoiceSync {

    const val CHOICE_ID = 594
    const val LOST_KEY = 5680

    fun apply(
        choiceId: Int,
        html: String,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (choiceId != CHOICE_ID) return false
        if (!html.contains("You acquire")) return false
        consumeItem(LOST_KEY, 1)
        return true
    }
}
