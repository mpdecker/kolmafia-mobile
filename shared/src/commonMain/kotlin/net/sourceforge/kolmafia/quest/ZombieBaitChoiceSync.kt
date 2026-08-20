package net.sourceforge.kolmafia.quest

/**
 * Desktop [ChoiceControl] A Zombie Master's Bait choice 599.
 */
object ZombieBaitChoiceSync {

    const val CHOICE_ID = 599
    const val CRAPPY_BRAIN = 5752
    const val DECENT_BRAIN = 5753
    const val GOOD_BRAIN = 5754
    const val BOSS_BRAIN = 5755

    private val QUANTITY_FIELD = Regex("(?:^|[&?])quantity=", RegexOption.IGNORE_CASE)
    private val QUANTITY_VALUE = Regex("(?:^|[&?])quantity=([^&]*)", RegexOption.IGNORE_CASE)

    fun apply(
        choiceId: Int,
        decision: Int,
        choiceUrl: String,
        itemCount: (Int) -> Int,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (choiceId != CHOICE_ID) return false
        if (!QUANTITY_FIELD.containsMatchIn(choiceUrl)) return false
        val brainId = when (decision) {
            1 -> CRAPPY_BRAIN
            2 -> DECENT_BRAIN
            3 -> GOOD_BRAIN
            4 -> BOSS_BRAIN
            else -> return false
        }
        val requested = QUANTITY_VALUE.find(choiceUrl)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        val qty = minOf(requested, itemCount(brainId))
        if (qty > 0) consumeItem(brainId, qty)
        return true
    }
}
