package net.sourceforge.kolmafia.item

/** Non-destructive retrieve method labels for maximizer emit (desktop InventoryManager.simRetrieveItem). */
object RetrieveItemSimulator {

    data class Context(
        val inventoryCount: (Int) -> Int,
        val closetContents: Map<Int, Int>,
        val storageContents: Map<Int, Int>,
        val displayContents: Map<Int, Int>,
        val stashContents: Map<Int, Int>,
        val pullAllowed: (Int) -> Boolean = { true },
        val canUseCloset: Boolean = true,
        val canUseStorage: Boolean = true,
        val canUseStash: Boolean = true,
    )

    fun simRetrieve(itemId: Int, qty: Int, ctx: Context): String {
        if (qty <= 0) return "have"
        if (ctx.inventoryCount(itemId) >= qty) return "have"

        if (ctx.canUseCloset && (ctx.closetContents[itemId] ?: 0) > 0) {
            return "uncloset"
        }
        if (ctx.canUseStorage && ctx.pullAllowed(itemId) && (ctx.storageContents[itemId] ?: 0) > 0) {
            return "pull"
        }
        if ((ctx.displayContents[itemId] ?: 0) > 0) {
            return "undisplay"
        }
        if (ctx.canUseStash && (ctx.stashContents[itemId] ?: 0) > 0) {
            return "unstash"
        }
        return "fail"
    }
}
