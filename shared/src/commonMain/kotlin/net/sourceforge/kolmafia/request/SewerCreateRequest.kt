package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionPermitted
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.item.CreateItemIngredients
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [SewerRequest] — chewing-gum sewer retrieval create for SEWER concoctions. */
class SewerCreateRequest(
    private val useItemRequest: UseItemRequest,
    private val closetRequest: ClosetRequest,
    private val createItemIngredients: CreateItemIngredients,
    private val gameDatabase: GameDatabase?,
    private val inventoryCountById: (Int) -> Int = { 0 },
) {
    suspend fun create(
        concoction: ConcoctionData,
        quantity: Int,
        state: CharacterState?,
        preferences: Preferences?,
    ): Result<Int> {
        if (quantity <= 0) return Result.success(0)
        if (state != null &&
            !ConcoctionPermitted.isPermittedMethod(
                concoction,
                state,
                prefs = preferences,
                limitMode = state.limitMode,
            )
        ) {
            return Result.failure(IllegalStateException("Sewer craft not permitted: ${concoction.result}"))
        }

        val goalIds = goalItemIds(concoction)
            ?: return Result.failure(IllegalStateException("Unknown sewer goal item: ${concoction.result}"))

        val initialCount = currentGoalCount(goalIds)
        var count = initialCount
        val needed = initialCount + quantity

        while (count < needed) {
            if (!createItemIngredients.makeIngredients(concoction, 1, state)) {
                return failurePartial(concoction.result, quantity, count - initialCount)
            }
            closetGoalItems(goalIds)
            val response = useItemRequest.use(CHEWING_GUM_ID, 1)
            if (response.isFailure) {
                return failurePartial(concoction.result, quantity, count - initialCount)
            }
            val body = response.getOrThrow()
            val afterUse = currentGoalCount(goalIds)
            if (afterUse <= 0 && !body.contains("You acquire")) {
                return failurePartial(concoction.result, quantity, count - initialCount)
            }
            count += afterUse.coerceAtLeast(if (body.contains("You acquire")) 1 else 0)
        }
        return Result.success((count - initialCount).coerceAtMost(quantity))
    }

    private suspend fun closetGoalItems(goalIds: List<Int>) {
        for (itemId in goalIds) {
            val available = inventoryCountById(itemId)
            if (available > 0) {
                closetRequest.putIn(itemId, available)
            }
        }
    }

    private fun currentGoalCount(goalIds: List<Int>): Int =
        goalIds.sumOf { id -> inventoryCountById(id) }

    private fun goalItemIds(concoction: ConcoctionData): List<Int>? {
        if (concoction.result.equals(WORTHLESS_ITEM_NAME, ignoreCase = true)) {
            return WORTHLESS_COMPONENT_IDS.toList()
        }
        val itemId = gameDatabase?.item(concoction.result)?.id
            ?: ItemDatabase.getByName(concoction.result)?.id
            ?: return null
        return listOf(itemId)
    }

    private fun failurePartial(result: String, quantity: Int, created: Int): Result<Int> =
        if (created > 0) {
            Result.failure(IllegalStateException("Could not create $quantity of $result (got $created)"))
        } else {
            Result.failure(IllegalStateException("Sewer retrieval was unsuccessful for: $result"))
        }

    companion object {
        const val CHEWING_GUM_ID = 23
        const val WORTHLESS_ITEM_NAME = "worthless item"

        val WORTHLESS_COMPONENT_IDS = setOf(
            HermitRequest.WORTHLESS_TRINKET_ID,
            HermitRequest.WORTHLESS_GEWGAW_ID,
            HermitRequest.WORTHLESS_KNICK_KNACK_ID,
        )
    }
}
