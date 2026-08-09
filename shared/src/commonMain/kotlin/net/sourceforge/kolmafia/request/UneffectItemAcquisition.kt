package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.ItemAvailability
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DynamicItemModifierSync

/** Desktop [UneffectRequest.getAction] item acquisition probes for uneffect routing. */
object UneffectItemAcquisition {

    fun hasRemedy(ctx: UneffectActionContext): Boolean =
        ctx.hasItemId(UneffectRemovableMaps.REMEDY) ||
            ctx.hasItemId(UneffectRemovableMaps.ANCIENT_CURE_ALL)

    internal fun mallStashGateOpen(effectId: Int, ctx: UneffectActionContext): Boolean =
        UneffectRemovableMaps.needsCocoa(effectId) || !hasRemedy(ctx)

    fun canAcquireUneffectItem(
        itemId: Int,
        effectId: Int,
        ctx: UneffectActionContext,
        checkContext: DynamicItemModifierSync.CheckContext,
        prefs: Preferences,
        db: GameDatabase?,
        charState: CharacterState?,
        accessibleCount: (Int) -> Int,
    ): Boolean {
        if (ctx.hasItemId(itemId)) return true

        val itemName = ItemDatabase.getItemName(itemId)
        if (itemName.isEmpty()) return false

        if (ItemAvailability.canUseNPCStores(
                itemId,
                itemName,
                checkContext,
                prefs,
                charState,
                accessibleCount,
            )
        ) {
            return true
        }
        if (ItemAvailability.canUseCoinmasters(
                itemId,
                checkContext,
                prefs,
                charState,
                accessibleCount,
            )
        ) {
            return true
        }
        if (mallStashGateOpen(effectId, ctx)) {
            if (db != null &&
                ItemAvailability.canUseMall(itemId, itemName, db, checkContext, prefs)
            ) {
                return true
            }
            if (ItemAvailability.canUseClanStash(itemId, checkContext, prefs)) {
                return true
            }
        }
        return false
    }

    /** Desktop UneffectRequest.run needsCocoa error before generic HTTP shrug. */
    fun shouldBlockNeedsCocoaHttpUneffect(
        effectId: Int,
        ctx: UneffectActionContext,
        checkContext: DynamicItemModifierSync.CheckContext,
        prefs: Preferences,
        db: GameDatabase?,
        charState: CharacterState?,
        accessibleCount: (Int) -> Int,
    ): Boolean {
        if (!UneffectRemovableMaps.needsCocoa(effectId)) return false
        val mappedItem = UneffectRemovableMaps.getUneffectItemId(effectId) ?: return false
        if (mappedItem != UneffectRemovableMaps.HOT_DREADSYLVANIAN_COCOA) return false
        return !canAcquireUneffectItem(
            mappedItem,
            effectId,
            ctx,
            checkContext,
            prefs,
            db,
            charState,
            accessibleCount,
        )
    }
}
