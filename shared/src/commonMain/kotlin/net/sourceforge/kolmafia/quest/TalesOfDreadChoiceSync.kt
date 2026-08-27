package net.sourceforge.kolmafia.quest

/** Choice 767 is explicitly registered by desktop ChoiceControl but has no state mutation. */
object TalesOfDreadChoiceSync {
    const val CHOICE_ID = 767
    fun apply(choiceId: Int): Boolean = choiceId == CHOICE_ID
}
