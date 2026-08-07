package net.sourceforge.kolmafia.data

/** Physical item locations merged for ConcoctionDatabase.refreshConcoctionsNow (desktop getAvailableIngredients). */
data class ConcoctionIngredientSources(
    val inventory: Map<Int, Int> = emptyMap(),
    val closet: Map<Int, Int> = emptyMap(),
    val storage: Map<Int, Int> = emptyMap(),
    val freepulls: Map<Int, Int> = emptyMap(),
    val stash: Map<Int, Int> = emptyMap(),
)

object ConcoctionAvailableIngredients {

    /** Sum per-item quantities across all supplied location maps plus queued ingredient credits. */
    fun aggregate(
        sources: ConcoctionIngredientSources,
        queuedCredits: Map<Int, Int> = ConcoctionQueuedIngredients.creditForRefresh(),
    ): Map<Int, Int> {
        val merged = mutableMapOf<Int, Int>()
        val inventoryWithCredits = sources.inventory.toMutableMap()
        for ((itemId, qty) in queuedCredits) {
            if (qty == 0) continue
            inventoryWithCredits[itemId] = (inventoryWithCredits[itemId] ?: 0) + qty
        }
        for (map in listOf(
            inventoryWithCredits,
            sources.closet,
            sources.storage,
            sources.freepulls,
            sources.stash,
        )) {
            for ((itemId, qty) in map) {
                if (qty <= 0) continue
                merged[itemId] = (merged[itemId] ?: 0) + qty
            }
        }
        return merged
    }
}
