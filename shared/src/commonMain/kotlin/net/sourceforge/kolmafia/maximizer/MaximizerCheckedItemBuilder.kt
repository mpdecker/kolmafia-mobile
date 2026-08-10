package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.FoldGroupDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.ItemAvailability
import net.sourceforge.kolmafia.inventory.PullableItems
import net.sourceforge.kolmafia.item.CreatableAmount
import net.sourceforge.kolmafia.item.CreatableTurns
import net.sourceforge.kolmafia.mall.MallPriceManager
import net.sourceforge.kolmafia.preferences.Preferences

/** Builds [MaximizerCheckedItem] from inventory snapshots + goal constraints. */
object MaximizerCheckedItemBuilder {

    data class Context(
        val spec: MaximizeSpec,
        val gameDatabase: GameDatabase,
        val characterState: CharacterState,
        val preferences: Preferences?,
        val mallPriceManager: MallPriceManager?,
        val inventoryCount: (Int) -> Int,
        val closetContents: Map<Int, Int>,
        val storageContents: Map<Int, Int>,
        val displayContents: Map<Int, Int>,
        val stashContents: Map<Int, Int>,
        val priceLevel: MaximizerPriceLevel = MaximizerPriceLevel.DONT_CHECK,
    ) {
        fun physicalAccessible(itemId: Int): Int =
            inventoryCount(itemId) +
                (closetContents[itemId] ?: 0) +
                (storageContents[itemId] ?: 0) +
                (displayContents[itemId] ?: 0) +
                (stashContents[itemId] ?: 0)

        fun initialCount(itemId: Int): Int =
            inventoryCount(itemId) +
                (closetContents[itemId] ?: 0) +
                (displayContents[itemId] ?: 0) +
                (stashContents[itemId] ?: 0)
    }

    fun build(itemId: Int, name: String, ctx: Context): MaximizerCheckedItem {
        val initial = ctx.initialCount(itemId)
        if (initial >= 3) {
            return MaximizerCheckedItem(itemId = itemId, name = name, initial = initial)
        }

        var creatable = 0
        var npcBuyable = 0
        var mallBuyable = 0
        var foldable = 0
        var pullable = 0
        var pullfoldable = 0
        var pullBuyable = 0
        var foldItemId = 0
        var buyableFlag = false

        val concoction = ConcoctionDatabase.getByResult(name)
        if (concoction != null) {
            creatable = CreatableAmount.quantityPossible(
                itemId,
                accessibleCount = { id, _ -> ctx.physicalAccessible(id) },
                preferRuntime = true,
            )
            if (creatable > 0 && noAdventures(ctx) &&
                adventuresNeeded(itemId, ctx) > 0
            ) {
                creatable = 0
            }

            val npcPrice = ctx.gameDatabase.npcPrice(name)
            val maxPrice = ctx.spec.maxPrice
            if (npcPrice > 0 && maxPrice != null && maxPrice >= npcPrice) {
                npcBuyable = (maxPrice / npcPrice).coerceAtMost(Int.MAX_VALUE)
            }
        }

        if (foldablesEnabled(ctx)) {
            val fold = computeFoldable(itemId, name, ctx, storageOnly = false)
            foldable = fold.count
            if (fold.foldItemId != 0) foldItemId = fold.foldItemId
        }

        if (pullAllowed(itemId, ctx)) {
            pullable = ctx.storageContents[itemId] ?: 0
            if (foldablesEnabled(ctx)) {
                val pullFold = computeFoldable(itemId, name, ctx, storageOnly = true)
                pullfoldable = pullFold.count
                if (pullFold.foldItemId != 0) foldItemId = pullFold.foldItemId
            }
        }

        val preBuyTotal = initial + creatable + npcBuyable + foldable + pullable + pullfoldable

        if (initial == 0 && ctx.spec.maxPrice != null && preBuyTotal == 0 &&
            canUseMall(itemId, name, ctx)
        ) {
            val maxPrice = ctx.spec.maxPrice.toLong()
            val priceBudget = minOf(maxPrice, ctx.characterState.meat.toLong())
            val historical = ctx.mallPriceManager?.getHistoricalPrice(itemId) ?: 0L
            if (mallHistoricalAllowed(historical, priceBudget, ctx.priceLevel)) {
                mallBuyable = 1
                buyableFlag = true
            }
        }

        if (pullAllowed(itemId, ctx) &&
            preBuyTotal + mallBuyable == 0 &&
            ctx.spec.maxPrice != null &&
            canUseMallToStorage(itemId, ctx)
        ) {
            val maxPrice = ctx.spec.maxPrice.toLong()
            val priceBudget = minOf(maxPrice, ctx.characterState.storageMeat)
            val historical = ctx.mallPriceManager?.getHistoricalPrice(itemId) ?: 0L
            if (mallHistoricalAllowed(historical, priceBudget, ctx.priceLevel)) {
                pullBuyable = 1
                buyableFlag = true
            }
        }

        return MaximizerCheckedItem(
            itemId = itemId,
            name = name,
            initial = initial,
            creatable = creatable,
            npcBuyable = npcBuyable,
            mallBuyable = mallBuyable,
            foldable = foldable,
            pullable = pullable,
            pullfoldable = pullfoldable,
            pullBuyable = pullBuyable,
            foldItemId = foldItemId,
            buyableFlag = buyableFlag,
        )
    }

    fun foldPeerItemIds(itemId: Int, name: String, ctx: Context): Set<Int> {
        if (!foldablesEnabled(ctx)) return emptySet()
        val group = FoldGroupDatabase.groupFor(name) ?: ctx.gameDatabase.foldGroup(name) ?: return emptySet()
        return group.items.mapNotNull { peer ->
            ctx.gameDatabase.item(peer)?.id ?: ItemDatabase.getByName(peer)?.id
        }.filter { it != itemId }.toSet()
    }

    private data class FoldResult(val count: Int, val foldItemId: Int)

    private fun computeFoldable(
        itemId: Int,
        name: String,
        ctx: Context,
        storageOnly: Boolean,
    ): FoldResult {
        val group = FoldGroupDatabase.groupFor(name) ?: ctx.gameDatabase.foldGroup(name) ?: return FoldResult(0, 0)
        var total = 0
        var foldId = 0
        for (peer in group.items) {
            if (peer.equals(name, ignoreCase = true)) continue
            val peerId = ctx.gameDatabase.item(peer)?.id ?: ItemDatabase.getByName(peer)?.id ?: continue
            val count = if (storageOnly) {
                ctx.storageContents[peerId] ?: 0
            } else {
                ctx.physicalAccessible(peerId)
            }
            if (count <= 0) continue
            total += count
            foldId = peerId
        }
        return FoldResult(total, foldId)
    }

    private fun pullAllowed(itemId: Int, ctx: Context): Boolean {
        if (ctx.characterState.isHardcore) return false
        return PullableItems.storagePullAllowed(ctx.characterState, itemId, ctx.gameDatabase)
    }

    private fun mallHistoricalAllowed(
        historical: Long,
        priceBudget: Long,
        priceLevel: MaximizerPriceLevel,
    ): Boolean {
        if (priceLevel == MaximizerPriceLevel.DONT_CHECK) return true
        return historical in 1 until priceBudget * 2
    }

    private fun canUseMall(itemId: Int, name: String, ctx: Context): Boolean {
        val prefs = ctx.preferences ?: return false
        return ItemAvailability.canUseMall(
            itemId = itemId,
            itemName = name,
            db = ctx.gameDatabase,
            prefs = prefs,
            limitMode = ctx.characterState.limitMode,
        )
    }

    private fun canUseMallToStorage(itemId: Int, ctx: Context): Boolean {
        val prefs = ctx.preferences ?: return false
        return ItemAvailability.canUseMallToStorage(
            itemId = itemId,
            db = ctx.gameDatabase,
            prefs = prefs,
            limitMode = ctx.characterState.limitMode,
        )
    }

    private fun foldablesEnabled(ctx: Context): Boolean =
        ctx.preferences?.getBoolean("maximizerFoldables", true) ?: true

    private fun noAdventures(ctx: Context): Boolean =
        ctx.preferences?.getBoolean("maximizerNoAdventures", false) ?: false

    private fun adventuresNeeded(itemId: Int, ctx: Context): Int =
        CreatableTurns.adventuresNeeded(
            itemId = itemId,
            quantityNeeded = 1,
            inventoryCount = { ctx.physicalAccessible(itemId) },
            isPermitted = { true },
        )
}
