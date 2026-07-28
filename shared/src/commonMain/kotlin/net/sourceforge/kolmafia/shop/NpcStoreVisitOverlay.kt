package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.data.NpcStoreData
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.data.NpcStoreItem

/** Runtime NPC store rows learned from shop visits (desktop ShopRequest newMeatRows). */
object NpcStoreVisitOverlay {

    private data class OverlayEntry(
        val storeKey: String,
        val storeName: String,
        val itemId: Int,
        val itemName: String,
        val price: Int,
        val rowId: Int,
    )

    private val byItemId = mutableMapOf<Int, OverlayEntry>()
    private val storeItems = mutableMapOf<String, MutableList<NpcStoreItem>>()
    private val storeNames = mutableMapOf<String, String>()

    fun registerMeatRow(
        storeKey: String,
        storeName: String,
        itemId: Int,
        itemName: String,
        price: Int,
        rowId: Int,
    ): Boolean {
        if (itemId <= 0) return false
        if (NpcStoreDatabase.itemEntry(itemId) != null) return false
        if (byItemId.containsKey(itemId)) return false

        val key = storeKey.lowercase()
        byItemId[itemId] = OverlayEntry(storeKey, storeName, itemId, itemName, price, rowId)
        storeNames[key] = storeName
        storeItems.getOrPut(key) { mutableListOf() }.add(NpcStoreItem(itemName, price))
        return true
    }

    fun itemEntry(itemId: Int): Pair<NpcStoreData, NpcStoreItem>? {
        val entry = byItemId[itemId] ?: return null
        val store = storeDataFor(entry.storeKey, entry.storeName)
        return store to NpcStoreItem(entry.itemName, entry.price)
    }

    fun storeForItem(itemName: String): NpcStoreData? {
        val normalized = itemName.lowercase()
        for ((key, items) in storeItems) {
            if (items.any { it.itemName.equals(normalized, ignoreCase = true) }) {
                return storeDataFor(key, storeNames[key].orEmpty())
            }
        }
        return null
    }

    fun toNpcStoreLine(itemId: Int): String? {
        val entry = byItemId[itemId] ?: return null
        return "${entry.storeName}\t${entry.storeKey}\t${entry.itemName}\t${entry.price}\tROW${entry.rowId}"
    }

    private fun storeDataFor(storeKey: String, storeName: String): NpcStoreData {
        val key = storeKey.lowercase()
        return NpcStoreData(
            storeKey = storeKey,
            storeName = storeName,
            storeType = "NPC",
            items = storeItems[key].orEmpty(),
        )
    }

    internal fun resetForTest() {
        byItemId.clear()
        storeItems.clear()
        storeNames.clear()
    }
}
