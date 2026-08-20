package net.sourceforge.kolmafia.quest

/**
 * Desktop [ChoiceControl] Skeletons and The Closet choice 603.
 */
object SkeletonClosetChoiceSync {

    const val CHOICE_ID = 603
    const val SKELETON = 5881

    fun apply(
        choiceId: Int,
        decision: Int,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (choiceId != CHOICE_ID) return false
        if (decision == 6) return false
        consumeItem(SKELETON, 1)
        return true
    }
}
