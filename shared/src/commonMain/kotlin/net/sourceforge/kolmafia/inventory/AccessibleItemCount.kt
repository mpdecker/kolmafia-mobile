package net.sourceforge.kolmafia.inventory

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.item.RetrieveSourceGates
import net.sourceforge.kolmafia.request.ClanStashRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.DisplayCaseRequest
import net.sourceforge.kolmafia.request.HermitRequest
import net.sourceforge.kolmafia.request.StorageRequest
import net.sourceforge.kolmafia.ash.CollectionCache
import net.sourceforge.kolmafia.preferences.Preferences

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
        if (itemId <= 0) return 0

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

        val prefs = context.preferences
        var total = inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0

        // Desktop: closet only when InventoryManager.canUseCloset()
        if (RetrieveSourceGates.canUseCloset(prefs, context.characterState)) {
            total += closetAmount(itemId, closetRequest, prefs)
        }

        if (PullableItems.storagePullAllowed(context.characterState, itemId, context.gameDatabase)) {
            val (storageQty, freepullQty) = storageAmounts(itemId, storageRequest, context)
            // Free pulls always accessible (desktop)
            total += freepullQty
            if (StorageRequest.canUseStorage(context.characterState) &&
                RetrieveSourceGates.canUseStorage(prefs, context.characterState)
            ) {
                total += storageQty
            }
        }

        // Desktop getAccessibleCount does NOT include display case.
        // Keep [displayCaseRequest] in the signature for call-site stability.
        @Suppress("UNUSED_VARIABLE")
        val unusedDisplay = displayCaseRequest

        if (RetrieveSourceGates.canUseClanStash(prefs, context.characterState)) {
            total += stashAmount(itemId, clanStashRequest, prefs)
        }

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

    private suspend fun closetAmount(
        itemId: Int,
        closetRequest: ClosetRequest?,
        prefs: Preferences?,
    ): Int {
        if (prefs != null && CollectionCacheSync.closetRetrieved) {
            return CollectionCache.load(prefs, Preferences.CACHED_CLOSET)[itemId] ?: 0
        }
        val live = closetRequest?.fetchContents() ?: return 0
        if (prefs != null) CollectionCacheSync.saveCloset(prefs, live)
        return live[itemId] ?: 0
    }

    private suspend fun storageAmounts(
        itemId: Int,
        storageRequest: StorageRequest?,
        context: AccessCountContext,
    ): Pair<Int, Int> {
        val prefs = context.preferences
        if (prefs != null && CollectionCacheSync.storageRetrieved) {
            val storage = CollectionCache.load(prefs, Preferences.CACHED_STORAGE)[itemId] ?: 0
            val freepull = CollectionCache.load(prefs, Preferences.CACHED_FREEPULLS)[itemId] ?: 0
            return storage to freepull
        }
        val classified = storageRequest?.fetchClassifiedContents(
            context.characterState,
            prefs,
        ) ?: return 0 to 0
        if (prefs != null) {
            CollectionCacheSync.saveStorage(
                prefs, classified.storage, classified.freepulls, classified.nopulls,
            )
        }
        return (classified.storage[itemId] ?: 0) to (classified.freepulls[itemId] ?: 0)
    }

    private suspend fun stashAmount(
        itemId: Int,
        clanStashRequest: ClanStashRequest?,
        prefs: Preferences?,
    ): Int {
        if (prefs != null && net.sourceforge.kolmafia.clan.ClanManager.stashRetrieved) {
            return CollectionCache.load(prefs, Preferences.CACHED_STASH)[itemId] ?: 0
        }
        val live = clanStashRequest?.fetchContents() ?: return 0
        if (prefs != null) CollectionCacheSync.saveStash(prefs, live)
        return live[itemId] ?: 0
    }
}
