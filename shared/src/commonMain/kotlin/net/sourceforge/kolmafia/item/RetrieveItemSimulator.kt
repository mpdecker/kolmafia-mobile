package net.sourceforge.kolmafia.item

/** Non-destructive retrieve method labels for maximizer emit (desktop InventoryManager.simRetrieveItem). */
object RetrieveItemSimulator {

    data class Context(
        val inventoryCount: (Int) -> Int,
        val closetContents: Map<Int, Int>,
        val storageContents: Map<Int, Int>,
        val freepullContents: Map<Int, Int> = emptyMap(),
        val displayContents: Map<Int, Int>,
        val stashContents: Map<Int, Int>,
        val pullAllowed: (Int) -> Boolean = { true },
        val canUseCloset: Boolean = true,
        val canUseStorage: Boolean = true,
        val canUseStash: Boolean = true,
        val canUseDisplay: Boolean = true,
        val equippedCount: (Int) -> Int = { 0 },
        val useEquipped: Boolean = true,
        val familiarHasItem: (Int) -> Boolean = { false },
        val canCreate: (Int) -> Boolean = { false },
        val cheaperToBuy: (Int, Int) -> Boolean = { _, _ -> false },
        val canBuyNpc: (Int) -> Boolean = { false },
        val canBuyMall: (Int) -> Boolean = { false },
        val canBuyCoinmaster: (Int) -> Boolean = { false },
    )

    fun simRetrieve(itemId: Int, qty: Int, ctx: Context): String {
        if (qty <= 0) return "have"
        if (ctx.inventoryCount(itemId) >= qty) return "have"

        val need = qty - ctx.inventoryCount(itemId)

        if (ctx.familiarHasItem(itemId)) return "steal"

        if (ctx.useEquipped && ctx.equippedCount(itemId) > 0) return "remove"

        if (ctx.canUseCloset && (ctx.closetContents[itemId] ?: 0) > 0) {
            return "uncloset"
        }
        if ((ctx.freepullContents[itemId] ?: 0) > 0) {
            return "free pull"
        }
        if (ctx.canUseStorage && ctx.pullAllowed(itemId) && (ctx.storageContents[itemId] ?: 0) > 0) {
            return "pull"
        }
        if (ctx.canUseDisplay && (ctx.displayContents[itemId] ?: 0) > 0) {
            return "undisplay"
        }
        if (ctx.canUseStash && (ctx.stashContents[itemId] ?: 0) > 0) {
            return "unstash"
        }
        if (ctx.canCreate(itemId)) {
            return if (ctx.cheaperToBuy(itemId, need)) "create or buy" else "create"
        }
        if (ctx.canBuyNpc(itemId)) return "buy from NPC"
        if (ctx.canBuyCoinmaster(itemId)) return "coinmaster"
        if (ctx.canBuyMall(itemId)) return "buy"
        return "fail"
    }
}
