package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.adventure.ChoiceRequest

/** Desktop SkeletonCommand — use skeleton → choice 603. */
class SkeletonRequest(
    private val useItemRequest: UseItemRequest,
    private val choiceRequest: ChoiceRequest,
) {
    suspend fun useSkeleton(
        option: Int,
        inventoryCounts: (Int) -> Int,
    ): Result<String> {
        if (option !in 1..5) {
            return Result.failure(
                IllegalArgumentException("I don't understand that skeleton type."),
            )
        }
        if (inventoryCounts(SKELETON_ITEM_ID) <= 0) {
            return Result.failure(
                IllegalStateException("You have no skeletons and can't get any with your current settings."),
            )
        }
        useItemRequest.use(SKELETON_ITEM_ID, 1).onFailure { return Result.failure(it) }
        return choiceRequest.choose(CHOICE_ID, option).map { (html, _) -> html }
    }

    companion object {
        const val SKELETON_ITEM_ID = 5881
        const val CHOICE_ID = 603

        private val TYPES = mapOf(
            "warrior" to 1,
            "cleric" to 2,
            "wizard" to 3,
            "rogue" to 4,
            "buddy" to 5,
        )

        fun findSkeleton(name: String): Int =
            TYPES[name.trim().lowercase()] ?: 0
    }
}
