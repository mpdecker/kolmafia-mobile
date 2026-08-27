package net.sourceforge.kolmafia.quest

/**
 * Desktop [ChoiceControl] Dart Perks choice 1525 → [InventoryManager.checkDartPerks].
 */
object DartPerksChoiceSync {

    const val CHOICE_ID = 1525
    const val HOLSTER_NAME = "Everfull Dart Holster"

    fun apply(
        choiceId: Int,
        checkDartPerks: () -> Unit = {},
    ): Boolean {
        if (choiceId != CHOICE_ID) return false
        checkDartPerks()
        return true
    }
}
