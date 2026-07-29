package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.UseItemRequest

/** Desktop KoLCharacter.findWand / getZapper wand discovery. */
object WandDiscovery {
    const val DEAD_MIMIC = 1267
    const val PINE_WAND = 1268
    const val EBONY_WAND = 1269
    const val HEXAGONAL_WAND = 1270
    const val ALUMINUM_WAND = 1271
    const val MARBLE_WAND = 1272

    val WAND_IDS = intArrayOf(
        PINE_WAND,
        EBONY_WAND,
        HEXAGONAL_WAND,
        ALUMINUM_WAND,
        MARBLE_WAND,
    )

    private const val PREF_LAST_ZAPPER_WAND = "lastZapperWand"

    fun inventoryQty(inventoryManager: InventoryManager?, itemId: Int): Int =
        inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0

    fun hasItem(inventoryManager: InventoryManager?, itemId: Int): Boolean =
        inventoryQty(inventoryManager, itemId) > 0

    fun findWand(
        inventoryManager: InventoryManager?,
        preferences: Preferences?,
        ascensionNumber: Int,
    ): Int? {
        for (wandId in WAND_IDS) {
            if (hasItem(inventoryManager, wandId)) {
                preferences?.setInt(PREF_LAST_ZAPPER_WAND, ascensionNumber)
                return wandId
            }
        }
        return null
    }

    suspend fun getZapper(
        inventoryManager: InventoryManager?,
        preferences: Preferences?,
        ascensionNumber: Int,
        useItemRequest: UseItemRequest?,
    ): Int? {
        findWand(inventoryManager, preferences, ascensionNumber)?.let { return it }

        val lastZapperWand = preferences?.getInt(PREF_LAST_ZAPPER_WAND, 0) ?: 0
        if (ascensionNumber == lastZapperWand) {
            return null
        }

        if (!hasItem(inventoryManager, DEAD_MIMIC)) {
            return null
        }

        val useResult = useItemRequest?.use(DEAD_MIMIC) ?: return null
        if (useResult.isFailure) {
            return null
        }
        inventoryManager?.fetchInventory()

        return findWand(inventoryManager, preferences, ascensionNumber)
    }
}
