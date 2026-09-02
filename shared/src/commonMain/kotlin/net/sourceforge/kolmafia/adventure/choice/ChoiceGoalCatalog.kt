package net.sourceforge.kolmafia.adventure.choice

/** Desktop ChoiceManager.hasGoalButton — maze/goal choice adventures. */
object ChoiceGoalCatalog {

    private val GOAL_BUTTON_CHOICES = setOf(
        48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70,
        535, 536, 546, 594, 665,
        904, 905, 906, 907, 908, 909, 910, 911, 912, 913,
    )

    fun hasGoalButton(choiceId: Int): Boolean = choiceId in GOAL_BUTTON_CHOICES
}
