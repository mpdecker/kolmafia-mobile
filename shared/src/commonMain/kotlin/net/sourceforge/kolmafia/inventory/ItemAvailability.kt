package net.sourceforge.kolmafia.inventory

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DynamicItemModifierSync
import net.sourceforge.kolmafia.request.HermitRequest
import net.sourceforge.kolmafia.shop.CoinmasterDatabase

/** Desktop [InventoryManager.itemAvailable] and [InventoryManager.canUse*] helpers. */
object ItemAvailability {

    fun itemAvailable(
        itemId: Int,
        itemName: String,
        context: DynamicItemModifierSync.CheckContext,
        prefs: Preferences,
        db: GameDatabase,
        state: CharacterState? = null,
        accessibleCount: ((Int) -> Int)? = null,
    ): Boolean {
        if (context.inventoryItemIds.contains(itemId)) return true
        if (canUseStorage(itemId, context, prefs)) return true
        if (canUseMall(itemId, itemName, db, context, prefs)) return true
        if (canUseNPCStores(itemId, itemName, context, prefs, state, accessibleCount)) return true
        if (canUseCoinmasters(itemId, context, prefs, state, accessibleCount)) return true
        if (canUseClanStash(itemId, context, prefs)) return true
        if (canUseCloset(itemId, context, prefs)) return true
        return false
    }

    fun canUseMall(
        itemId: Int,
        itemName: String,
        db: GameDatabase,
        context: DynamicItemModifierSync.CheckContext,
        prefs: Preferences,
    ): Boolean {
        val item = db.item(itemName) ?: ItemDatabase.getById(itemId) ?: return false
        if (!item.isTradeable) return false
        if (!context.canInteract) return false
        if (!prefs.getBoolean("autoSatisfyWithMall", false)) return false
        if (LimitModeGates.limitMall(context.limitMode)) return false
        return true
    }

    fun canUseNPCStores(
        itemId: Int,
        itemName: String,
        context: DynamicItemModifierSync.CheckContext,
        prefs: Preferences,
        state: CharacterState? = null,
        accessibleCount: ((Int) -> Int)? = null,
    ): Boolean {
        if (!prefs.getBoolean("autoSatisfyWithNPCs", false)) return false
        if (LimitModeGates.limitNPCStores(context.limitMode)) return false
        val countFn = accessibleCount ?: contextAccessibleCount(context)
        if (state != null) {
            return NpcStoreDatabase.containsItem(
                itemId,
                validate = true,
                state = state,
                prefs = prefs,
                accessibleCount = countFn,
            )
        }
        return NpcStoreDatabase.storeForItem(itemName) != null
    }

    fun canUseCoinmasters(
        itemId: Int,
        context: DynamicItemModifierSync.CheckContext,
        prefs: Preferences,
        state: CharacterState? = null,
        accessibleCount: ((Int) -> Int)? = null,
    ): Boolean {
        if (!prefs.getBoolean("autoSatisfyWithCoinmasters", false)) return false
        if (LimitModeGates.limitCoinmasters(context.limitMode)) return false
        if (itemId == HermitRequest.ELEVEN_LEAF_CLOVER_ID && context.hermitCloverCount < 1) return false
        val countFn = accessibleCount ?: contextAccessibleCount(context)
        if (state != null) {
            return CoinmasterDatabase.containsBuyItem(
                itemId,
                validate = true,
                state = state,
                prefs = prefs,
                accessibleCount = countFn,
            )
        }
        return CoinmasterDatabase.findBuyRowForItem(itemId) != null
    }

    internal fun contextAccessibleCount(context: DynamicItemModifierSync.CheckContext): (Int) -> Int =
        { itemId ->
            var count = 0
            if (itemId in context.inventoryItemIds) count++
            if (itemId in context.storageItemIds) count++
            if (itemId in context.closetItemIds) count++
            if (itemId in context.stashItemIds) count++
            count
        }

    fun canUseStorage(
        itemId: Int,
        context: DynamicItemModifierSync.CheckContext,
        prefs: Preferences,
    ): Boolean {
        if (!context.canInteract) return false
        if (!prefs.getBoolean("autoSatisfyWithStorage", true)) return false
        if (LimitModeGates.limitStorage(context.limitMode)) return false
        return context.storageItemIds.contains(itemId)
    }

    fun canUseCloset(
        itemId: Int,
        context: DynamicItemModifierSync.CheckContext,
        prefs: Preferences,
    ): Boolean {
        if (!prefs.getBoolean("autoSatisfyWithCloset", false)) return false
        if (LimitModeGates.limitCampground(context.limitMode)) return false
        return context.closetItemIds.contains(itemId)
    }

    fun canUseClanStash(
        itemId: Int,
        context: DynamicItemModifierSync.CheckContext,
        prefs: Preferences,
    ): Boolean {
        if (!context.canInteract) return false
        if (!prefs.getBoolean("autoSatisfyWithStash", false)) return false
        if (!context.hasClan) return false
        if (LimitModeGates.limitClan(context.limitMode)) return false
        return context.stashItemIds.contains(itemId)
    }
}
