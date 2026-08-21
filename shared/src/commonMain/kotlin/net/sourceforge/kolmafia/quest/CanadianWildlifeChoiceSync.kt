package net.sourceforge.kolmafia.quest

/**
 * Desktop [ChoiceControl] Canadian Wildlife choices 1332–1333 item consumes.
 */
object CanadianWildlifeChoiceSync {

    val CHOICE_IDS = setOf(1332, 1333)

    const val GOVERNMENT_REQUISITION_FORM = 10003
    const val WALRUS_BLUBBER = 10034
    const val TINY_BOMB = 10036
    const val MOOSEFLANK = 10038

    fun apply(
        choiceId: Int,
        decision: Int,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (choiceId !in CHOICE_IDS) return false
        return when (choiceId) {
            1332 -> {
                consumeItem(GOVERNMENT_REQUISITION_FORM, 1)
                true
            }
            1333 -> when (decision) {
                2 -> {
                    consumeItem(MOOSEFLANK, 1)
                    true
                }
                3 -> {
                    consumeItem(WALRUS_BLUBBER, 10)
                    true
                }
                4 -> {
                    consumeItem(TINY_BOMB, 10)
                    true
                }
                else -> false
            }
            else -> false
        }
    }
}
