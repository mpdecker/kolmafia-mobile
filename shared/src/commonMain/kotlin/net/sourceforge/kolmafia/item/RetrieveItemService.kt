package net.sourceforge.kolmafia.item

import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.craftMode
import net.sourceforge.kolmafia.data.isAutoCraftable
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.npc.NpcBuyRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.ClanStashRequest
import net.sourceforge.kolmafia.request.CraftRequest
import net.sourceforge.kolmafia.request.DisplayCaseRequest
import net.sourceforge.kolmafia.request.StorageRequest
import net.sourceforge.kolmafia.request.UseItemRequest
import net.sourceforge.kolmafia.shop.CoinmasterManager

open class RetrieveItemService(
    private val inventoryManager: InventoryManager?,
    private val closetRequest: ClosetRequest?,
    private val storageRequest: StorageRequest?,
    private val displayCaseRequest: DisplayCaseRequest? = null,
    private val clanStashRequest: ClanStashRequest? = null,
    private val npcBuyRequest: NpcBuyRequest?,
    private val mallManager: MallManager?,
    private val coinmasterManager: CoinmasterManager? = null,
    private val craftRequest: CraftRequest? = null,
    private val useItemRequest: UseItemRequest? = null,
    private val gameDatabase: GameDatabase?,
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

        if (remaining > 0 && displayCaseRequest != null) {
            val result = displayCaseRequest.takeOut(itemId, remaining)
            if (result.isSuccess) remaining = 0
        }

        if (remaining > 0 && clanStashRequest != null) {
            val result = clanStashRequest.takeOut(itemId, remaining)
            if (result.isSuccess) remaining = 0
        }

        if (remaining > 0) {
            remaining -= craftMissing(itemName, itemId, remaining)
        }

        if (remaining > 0 && npcBuyRequest != null) {
            val npcStore = gameDatabase?.npcStoreFor(itemName)
            if (npcStore != null) {
                val bought = npcBuyRequest.buy(npcStore.storeKey, itemId, remaining).getOrDefault(0)
                remaining -= bought
            }
        }

        if (remaining > 0 && coinmasterManager != null) {
            remaining -= coinmasterManager.buyItem(itemId, remaining)
        }

        if (remaining > 0 && mallManager != null) {
            remaining -= mallManager.buy(itemId, remaining)
        }

        return qty - remaining
    }

    private suspend fun craftMissing(itemName: String, itemId: Int, qty: Int): Int {
        val concoction = ConcoctionDatabase.getByResult(itemName) ?: return 0
        if (!concoction.isAutoCraftable()) return 0

        if (concoction.methods.contains("SUSE") && useItemRequest != null) {
            val source = concoction.ingredients.firstOrNull()?.name ?: return 0
            val sourceId = gameDatabase?.item(source)?.id ?: return 0
            var made = 0
            while (made < qty) {
                if (retrieve(sourceId, 1) < 1) break
                if (useItemRequest.use(sourceId, 1).isFailure) break
                inventoryManager?.fetchInventory()
                if (inventoryCount(itemId) > made) made = inventoryCount(itemId)
                else made++
            }
            return made.coerceAtMost(qty)
        }

        val mode = concoction.craftMode() ?: return 0
        val craft = craftRequest ?: return 0
        if (concoction.ingredients.size < 2) return 0

        val ing1 = gameDatabase?.item(concoction.ingredients[0].name)?.id ?: return 0
        val ing2 = gameDatabase?.item(concoction.ingredients[1].name)?.id ?: return 0
        val before = inventoryCount(itemId)
        for (ing in concoction.ingredients) {
            val ingId = gameDatabase.item(ing.name)?.id ?: return 0
            if (retrieve(ingId, ing.quantity * qty) < ing.quantity * qty) return inventoryCount(itemId) - before
        }
        val created = craft.craft(mode, qty, ing1, ing2)
        inventoryManager?.fetchInventory()
        return (inventoryCount(itemId) - before).coerceAtLeast(created)
    }

    private fun inventoryCount(itemId: Int): Int =
        inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0
}
