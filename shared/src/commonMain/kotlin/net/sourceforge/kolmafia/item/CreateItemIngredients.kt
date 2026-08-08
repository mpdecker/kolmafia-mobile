package net.sourceforge.kolmafia.item

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionBuyables
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ConcoctionMeatPasteNeeded
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase

/** Desktop [net.sourceforge.kolmafia.request.concoction.CreateItemRequest.makeIngredients]. */
class CreateItemIngredients(
    private val retrieveItemService: RetrieveItemService,
    private val gameDatabase: GameDatabase?,
) {
    suspend fun makeIngredients(
        concoction: ConcoctionData,
        quantityNeeded: Int,
        state: CharacterState? = null,
        initialCount: Int = ConcoctionDatabase.getRuntime(concoction.result)?.initial ?: 0,
    ): Boolean {
        if (quantityNeeded <= 0) return true
        if (concoction.ingredients.isEmpty()) return false

        if (ConcoctionMeatPasteNeeded.needsPaste(concoction, state)) {
            val pasteQty = ConcoctionMeatPasteNeeded.getMeatPasteNeeded(
                concoction = concoction,
                quantityNeeded = quantityNeeded,
                initialCount = initialCount,
                state = state,
            )
            if (pasteQty > 0 &&
                retrieveItemService.retrieve(ConcoctionBuyables.MEAT_PASTE, pasteQty) < pasteQty
            ) {
                return false
            }
        }

        val yield = concoction.resultQuantity.coerceAtLeast(1)
        val sorted = concoction.ingredients
            .map { ingredient -> ingredient to itemIdFor(ingredient) }
            .filter { (_, itemId) -> itemId != null }
            .sortedBy { (ingredient, _) ->
                ConcoctionDatabase.creatableCount(ingredient.name)
            }

        if (sorted.size != concoction.ingredients.size) return false

        val retrievals = sorted.map { (ingredient, itemId) ->
            val multiplier = concoction.ingredients
                .filter { other -> itemIdFor(other) == itemId }
                .sumOf { it.quantity }
            var quantity = quantityNeeded * multiplier
            if (yield > 1) {
                quantity = (quantity + yield - 1) / yield
            }
            RetrievalPlan(itemId!!, quantity)
        }

        for (plan in retrievals) {
            if (retrieveItemService.retrieve(plan.itemId, plan.quantity) < plan.quantity) {
                return false
            }
        }

        for (plan in retrievals) {
            if (retrieveItemService.retrieve(plan.itemId, plan.quantity) < plan.quantity) {
                return false
            }
        }

        return true
    }

    private fun itemIdFor(ingredient: ConcoctionIngredient): Int? =
        gameDatabase?.item(ingredient.name)?.id ?: ItemDatabase.getByName(ingredient.name)?.id

    private data class RetrievalPlan(val itemId: Int, val quantity: Int)
}
