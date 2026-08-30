package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.JunkListManager
import net.sourceforge.kolmafia.request.ManageStoreRequest
import net.sourceforge.kolmafia.request.AutoMallRequest
import net.sourceforge.kolmafia.request.StoragePullRules

/**
 * Desktop [AutoMallCommand.automall] — mall profitable non-memento inventory items.
 */
class AutoMallRunner(
    private val junkListManager: JunkListManager,
    private val inventoryManager: InventoryManager,
    private val manageStoreRequest: ManageStoreRequest,
    private val character: KoLCharacter,
    private val gameDatabase: GameDatabase,
    private val autoMallRequest: AutoMallRequest? = null,
) {

    suspend fun automall() {
        val canInteract = StoragePullRules.canInteract(character.state.value)

        val offers = mutableListOf<AutoMallRequest.Offer>()
        for (itemId in junkListManager.profitableIds()) {
            if (junkListManager.isMemento(itemId)) continue
            if (itemId in SKIP_MALL_IDS) continue
            if (junkListManager.isSingleton(itemId) && !canInteract) continue

            val qty = inventoryCount(itemId)
            if (qty <= 0) continue

            val price = gameDatabase.item(itemId)?.autosellPrice ?: continue
            offers += AutoMallRequest.Offer(itemId, qty, price.toLong())
        }
        if (autoMallRequest != null) {
            autoMallRequest.addItems(offers)
        } else {
            offers.forEach { manageStoreRequest.addItem(it.itemId, it.price.toInt(), it.limit, it.quantity) }
        }
    }

    private fun inventoryCount(itemId: Int): Int =
        inventoryManager.state.value.items[itemId]?.quantity ?: 0

    companion object {
        internal val SKIP_MALL_IDS = setOf(
            MEAT_PASTE,
            MEAT_STACK,
            DENSE_MEAT_STACK,
        )

        const val MEAT_PASTE = 25
        const val MEAT_STACK = 88
        const val DENSE_MEAT_STACK = 258
    }
}
