package net.sourceforge.kolmafia.item

import net.sourceforge.kolmafia.data.ConcoctionCreationCost
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ItemDatabase

/** Desktop Concoction.getAdventuresNeeded with ingredient-tree recursion. */
object CreatableTurns {

    data class Context(
        val inventoryCount: (Int) -> Int,
        val isPermitted: (Int) -> Boolean,
        val considerFreeCrafting: Boolean = false,
        val freeCrafting: FreeCraftingTurns.Context = FreeCraftingTurns.Context(),
    )

    fun adventuresNeeded(
        itemId: Int,
        quantityNeeded: Int,
        inventoryCount: () -> Int,
        isPermitted: () -> Boolean,
        considerFreeCrafting: Boolean = false,
        freeCrafting: FreeCraftingTurns.Context = FreeCraftingTurns.Context(),
    ): Int = adventuresNeeded(
        itemId = itemId,
        quantityNeeded = quantityNeeded,
        context = Context(
            inventoryCount = { inventoryCount() },
            isPermitted = { isPermitted() },
            considerFreeCrafting = considerFreeCrafting,
            freeCrafting = freeCrafting,
        ),
    )

    fun adventuresNeeded(
        itemId: Int,
        quantityNeeded: Int,
        context: Context,
        visited: MutableSet<Int> = mutableSetOf(),
    ): Int {
        if (itemId in visited) return 0
        if (!context.isPermitted(itemId)) return 0

        val itemName = ItemDatabase.getById(itemId)?.name ?: return 0
        val concoction = ConcoctionDatabase.getByResult(itemName) ?: return 0

        val initial = context.inventoryCount(itemId)
        if (initial > 1) return 0

        val create = quantityNeeded - initial
        if (create <= 0) return 0

        var runningTotal = ConcoctionCreationCost.adventureUsage(concoction.methods) * create
        val yield = concoction.resultQuantity.coerceAtLeast(1)
        if (yield > 1) {
            runningTotal = (runningTotal + yield - 1) / yield
        }

        if (runningTotal == 0) return 0

        visited.add(itemId)
        for (ingredient in concoction.ingredients) {
            val ingId = ItemDatabase.getByName(ingredient.name)?.id ?: run {
                visited.remove(itemId)
                return 0
            }
            val ingInitial = context.inventoryCount(ingId)
            val ingNeeded = ingInitial + ingredient.quantity * create
            runningTotal += adventuresNeeded(
                itemId = ingId,
                quantityNeeded = ingNeeded,
                context = context,
                visited = visited,
            )
        }
        visited.remove(itemId)

        if (context.considerFreeCrafting) {
            val method = ConcoctionCreationCost.primaryMethod(concoction.methods)
            val freeCrafts = FreeCraftingTurns.freeTurnsForMethod(method, context.freeCrafting)
            runningTotal = maxOf(runningTotal - freeCrafts, 0)
        }

        return runningTotal
    }
}
