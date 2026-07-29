package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.JunkListManager
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.request.CraftRequest

/**
 * Desktop [QuarkCommand] — paste unstable quark with best junk-list item for MP.
 */
class QuarkRunner(
    private val junkListManager: JunkListManager,
    private val inventoryManager: InventoryManager,
    private val craftRequest: CraftRequest,
    private val retrieveItemService: RetrieveItemService,
    private val character: KoLCharacter,
    private val gameDatabase: GameDatabase,
) {

    suspend fun quark(
        itemNames: List<String> = emptyList(),
        print: (String) -> Unit = {},
    ): Boolean {
        if (inventoryCount(UNSTABLE_QUARK) < 1) {
            print("You have no unstable quarks.")
            return false
        }

        val charState = character.state.value
        if (!charState.knollAvailable && !charState.inZombiecore) {
            if (retrieveItemService.retrieve(MEAT_PASTE, 1) < 1) {
                print("Can't afford gluons.")
                return false
            }
        }

        val poolIds = if (itemNames.isEmpty()) {
            junkListManager.itemIds()
        } else {
            itemNames.mapNotNull { name -> gameDatabase.item(name)?.id }
        }

        if (itemNames.isNotEmpty() && poolIds.isEmpty()) {
            return false
        }

        val best = poolIds
            .mapNotNull { itemId -> candidate(itemId, charState) }
            .maxByOrNull { it.price }

        if (best == null) {
            print("No suitable quark-pasteable items found.")
            return false
        }

        print("Pasting unstable quark with ${best.qty} ${best.name}")
        craftRequest.craft("combine", 1, UNSTABLE_QUARK, best.itemId)
        return true
    }

    private data class QuarkCandidate(
        val itemId: Int,
        val name: String,
        val qty: Int,
        val price: Int,
    )

    private fun candidate(itemId: Int, charState: CharacterState): QuarkCandidate? {
        val itemData = gameDatabase.item(itemId) ?: return null
        val qty = inventoryCount(itemId)
        val minQty = if (junkListManager.isSingleton(itemId)) 2 else 1
        if (qty < minQty) return null

        val price = itemData.autosellPrice
        if (price < 20) return null
        if (charState.currentMp + price > charState.maxMp) return null
        if (!isPasteable(itemData.name)) return null

        return QuarkCandidate(itemId, itemData.name, qty, price)
    }

    private fun isPasteable(itemName: String): Boolean {
        for (concoction in ConcoctionDatabase.getByIngredient(itemName)) {
            if (concoction.isCombining || "JEWELRY" in concoction.methods) {
                return true
            }
        }
        return false
    }

    private fun inventoryCount(itemId: Int): Int =
        inventoryManager.state.value.items[itemId]?.quantity ?: 0

    companion object {
        const val UNSTABLE_QUARK = 3743
        const val MEAT_PASTE = 25
    }
}
