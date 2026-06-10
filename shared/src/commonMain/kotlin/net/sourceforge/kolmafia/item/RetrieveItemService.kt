package net.sourceforge.kolmafia.item

import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.npc.NpcBuyRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.StorageRequest

open class RetrieveItemService(
    private val inventoryManager: InventoryManager?,
    private val closetRequest: ClosetRequest?,
    private val storageRequest: StorageRequest?,
    private val npcBuyRequest: NpcBuyRequest?,
    private val mallManager: MallManager?,
    private val gameDatabase: GameDatabase?
) {
    open suspend fun retrieve(itemId: Int, qty: Int): Int {
        val itemName = gameDatabase?.item(itemId)?.name ?: return 0
        var remaining = qty - inventoryCount(itemId)
        if (remaining <= 0) return qty

        if (remaining > 0 && closetRequest != null) {
            val result = closetRequest.takeOut(itemId, remaining)
            if (result.isSuccess) remaining = 0
        }

        if (remaining > 0 && storageRequest != null) {
            val result = storageRequest.withdraw(itemId, remaining)
            if (result.isSuccess) remaining = 0
        }

        if (remaining > 0 && npcBuyRequest != null) {
            val npcStore = gameDatabase?.npcStoreFor(itemName)
            if (npcStore != null) {
                val bought = npcBuyRequest.buy(npcStore.storeKey, itemId, remaining).getOrDefault(0)
                remaining -= bought
            }
        }

        if (remaining > 0 && mallManager != null) {
            remaining -= mallManager.buy(itemId, remaining)
        }

        return qty - remaining
    }

    private fun inventoryCount(itemId: Int): Int =
        inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0
}
