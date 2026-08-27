package net.sourceforge.kolmafia.quest

/** Choice 985 is explicitly registered by desktop ChoiceControl but has no state mutation. */
object OddJobsBoardChoiceSync {
    const val CHOICE_ID = 985
    fun apply(choiceId: Int): Boolean = choiceId == CHOICE_ID
}
