package net.sourceforge.kolmafia.inventory

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.request.ClanStashRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.DisplayCaseRequest
import net.sourceforge.kolmafia.request.HermitRequest
import net.sourceforge.kolmafia.request.StorageRequest

/** Desktop [InventoryManager.getAccessibleCount] physical locations (no mall/NPC/coinmaster). */
object AccessibleItemCount {

    suspend fun physicalCount(
        itemId: Int,
        itemName: String,
        inventoryManager: InventoryManager?,
        closetRequest: ClosetRequest?,
        storageRequest: StorageRequest?,
        displayCaseRequest: DisplayCaseRequest?,
        clanStashRequest: ClanStashRequest?,
        equipment: Map<EquipmentSlot, String>,
        context: AccessCountContext = AccessCountContext(),
    ): Int {
        if (itemId == HermitRequest.WORTHLESS_ITEM_ID) {
            return HermitRequest.availableWorthlessItemCount(
                inventoryManager = inventoryManager,
                closetRequest = closetRequest,
                storageRequest = storageRequest,
            )
        }

        if (!ItemRestriction.isAllowed(itemId, itemName, context.characterState, context.gameDatabase)) {
            return 0
        }

        var total = inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0
        total += closetRequest?.fetchContents()?.get(itemId) ?: 0
        if (PullableItems.storagePullAllowed(context.characterState, itemId, context.gameDatabase)) {
            val classified = storageRequest?.fetchClassifiedContents(context.characterState)
            total += classified?.freepulls?.get(itemId) ?: 0
            if (StorageRequest.canUseStorage(context.characterState)) {
                total += classified?.storage?.get(itemId) ?: 0
            }
        }
        total += displayCaseRequest?.fetchContents()?.get(itemId) ?: 0
        total += clanStashRequest?.fetchContents()?.get(itemId) ?: 0
        total += EquippedItemCount.totalEquippedCount(
            itemId = itemId,
            itemName = itemName,
            equipment = equipment,
            characterState = context.characterState,
            gameDatabase = context.gameDatabase,
            familiarManager = context.familiarManager,
        )
        return total
    }
}
