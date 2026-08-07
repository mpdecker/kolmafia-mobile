package net.sourceforge.kolmafia.item

import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionInterchangeableIngredients
import net.sourceforge.kolmafia.data.ItemDatabase

object CreatableAmount {

    fun quantityPossible(
        itemId: Int,
        accessibleCount: (Int, String) -> Int,
        preferRuntime: Boolean = true,
    ): Int {
        val itemName = ItemDatabase.getById(itemId)?.name ?: return 0
        val concoction = ConcoctionDatabase.getByResult(itemName) ?: return 0
        if (concoction.ingredients.isEmpty()) return 0

        if (preferRuntime && ConcoctionDatabase.getRuntime(itemName) != null) {
            return ConcoctionDatabase.creatableCount(itemName)
        }

        return quantityPossibleFlat(concoction, accessibleCount)
    }

    private fun quantityPossibleFlat(
        concoction: ConcoctionData,
        accessibleCount: (Int, String) -> Int,
    ): Int {
        val resultItemId = ItemDatabase.getByName(concoction.result)?.id
        val ingredients = ConcoctionInterchangeableIngredients.resolve(
            concoction,
            resultItemId,
            { id -> accessibleCount(id, ItemDatabase.getById(id)?.name ?: "") },
        )
        var crafts = Int.MAX_VALUE
        for (ingredient in ingredients) {
            val ingId = ItemDatabase.getByName(ingredient.name)?.id ?: return 0
            if (ingredient.quantity <= 0) return 0
            val available = accessibleCount(ingId, ingredient.name)
            crafts = minOf(crafts, available / ingredient.quantity)
        }
        if (crafts == Int.MAX_VALUE || crafts <= 0) return 0
        val yield = concoction.resultQuantity.coerceAtLeast(1)
        return crafts * yield
    }
}
