package net.sourceforge.kolmafia.data

import kotlin.math.max

data class ConcoctionAdventuresContext(
    val initialCount: (String) -> Int = { 0 },
    val isPermitted: (ConcoctionData) -> Boolean = { true },
    val freeCraftingTurns: Int = 0,
    val freeSmithingTurns: Int = 0,
    val freeCookingTurns: Int = 0,
    val freeMixingTurns: Int = 0,
) {
    fun initialFor(concoction: ConcoctionData): Int = initialCount(concoction.result.lowercase())

    companion object {
        val EMPTY = ConcoctionAdventuresContext()
    }
}

/** Desktop Concoction.getAdventuresNeeded — recursive craft-turn cost for average-adventure cache. */
fun getAdventuresNeeded(
    concoction: ConcoctionData,
    quantityNeeded: Int,
    considerFree: Boolean,
    context: ConcoctionAdventuresContext = ConcoctionAdventuresContext.EMPTY,
    visiting: MutableSet<String> = mutableSetOf(),
): Int {
    if (!context.isPermitted(concoction)) {
        return 0
    }

    val create = quantityNeeded - context.initialFor(concoction)
    if (create <= 0) {
        return 0
    }

    if (concoction.resultQuantity > 1) {
        return 0
    }

    val key = concoction.result.lowercase()
    if (key in visiting) {
        return 0
    }
    visiting.add(key)

    var runningTotal = ConcoctionAdventureUsage.adventureUsageForConcoction(concoction) * create
    val yield = concoction.craftYield
    if (yield > 1) {
        runningTotal = (runningTotal + yield - 1) / yield
    }

    if (runningTotal == 0) {
        visiting.remove(key)
        return 0
    }

    for (ingredient in concoction.ingredients) {
        val child = ConcoctionDatabase.getByResult(ingredient.name) ?: run {
            visiting.remove(key)
            return 0
        }
        runningTotal += getAdventuresNeeded(
            child,
            create,
            considerFree = false,
            context = context,
            visiting = visiting,
        )
    }

    visiting.remove(key)

    if (!considerFree) {
        return runningTotal
    }

    var freeCrafts = context.freeCraftingTurns
    when {
        "SMITH" in concoction.methods || "SSMITH" in concoction.methods ->
            freeCrafts += context.freeSmithingTurns
        "COOK_FANCY" in concoction.methods ->
            freeCrafts += context.freeCookingTurns
        "MIX_FANCY" in concoction.methods ->
            freeCrafts += context.freeMixingTurns
    }
    return max(runningTotal - freeCrafts, 0)
}
