package net.sourceforge.kolmafia.item

import net.sourceforge.kolmafia.data.ConcoctionCreationCost
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [InventoryManager.cheaperToBuy] / [priceToMake] / [priceToAcquire] / [itemValue]
 * subset (Phases 2541–2555).
 */
object RetrievePricing {

    const val UNAVAILABLE = Long.MAX_VALUE / 4

    data class PriceContext(
        val inventoryCount: (Int) -> Int = { 0 },
        val mallPrice: (Int) -> Long = { -1L },
        val historicalMallPrice: (Int) -> Long = { -1L },
        val npcPrice: (Int) -> Long = { id ->
            val name = ItemDatabase.getItemName(id)
            if (name.isBlank()) 0L else NpcStoreDatabase.npcPrice(name).toLong()
        },
        val prefs: Preferences? = null,
        val canCreate: (Int) -> Boolean = { true },
    )

    fun itemValue(itemId: Int, exact: Boolean, ctx: PriceContext): Long {
        val factor = ctx.prefs?.getFloat("valueOfInventory", 1.8f) ?: 1.8f
        if (factor <= 0f) return 0L
        val autosell = ItemDatabase.getById(itemId)?.autosellPrice?.toLong()?.coerceAtLeast(0L) ?: 0L
        if (factor <= 1f) {
            return (autosell * factor).toLong()
        }
        var upper = autosell
        val mall = if (exact) {
            ctx.mallPrice(itemId)
        } else {
            ctx.historicalMallPrice(itemId).takeIf { it > 0 } ?: ctx.mallPrice(itemId)
        }
        if (mall > 0) {
            upper = if (factor <= 2f) {
                val minish = maxOf(autosell, mall / 2)
                autosell + ((minish - autosell) * (factor - 1f)).toLong()
            } else {
                mall
            }
        }
        if (factor <= 2f) return upper
        val mallFull = mall.takeIf { it > 0 } ?: upper
        return upper + ((mallFull - upper) * (factor - 2f)).toLong()
    }

    fun priceToMake(itemId: Int, qty: Int, exact: Boolean, ctx: PriceContext, depth: Int = 0): Long {
        if (qty <= 0) return 0L
        if (depth > 10) return UNAVAILABLE
        val name = ItemDatabase.getItemName(itemId)
        if (name.isBlank()) return UNAVAILABLE
        val concoction = ConcoctionDatabase.getByResult(name) ?: return UNAVAILABLE
        if (!ctx.canCreate(itemId)) return UNAVAILABLE
        if (concoction.methods.isEmpty()) return UNAVAILABLE
        val yield = concoction.craftYield.coerceAtLeast(1)
        val batches = (qty + yield - 1) / yield
        var price = ConcoctionCreationCost.creationCost(concoction.methods) * batches
        val ingredients = concoction.ingredients
        if (ingredients.isEmpty()) return UNAVAILABLE
        for (ing in ingredients) {
            val ingId = ItemDatabase.getByName(ing.name)?.id ?: continue
            val needed = ing.quantity * batches
            val owned = ctx.inventoryCount(ingId)
            val stillNeed = (needed - owned).coerceAtLeast(0)
            val ownedValue = if (owned > 0) {
                itemValue(ingId, exact, ctx) * minOf(owned, needed)
            } else {
                0L
            }
            val acquire = if (stillNeed > 0) {
                priceToAcquire(ingId, stillNeed, exact, ctx, depth + 1)
            } else {
                0L
            }
            if (acquire >= UNAVAILABLE && stillNeed > 0) return UNAVAILABLE
            price += ownedValue + acquire
        }
        return price
    }

    fun priceToAcquire(
        itemId: Int,
        qty: Int,
        exact: Boolean,
        ctx: PriceContext,
        depth: Int = 0,
    ): Long {
        if (qty <= 0) return 0L
        if (depth > 10) return UNAVAILABLE
        val have = ctx.inventoryCount(itemId)
        if (have >= qty) {
            return itemValue(itemId, exact, ctx) * qty
        }
        val need = qty - have
        val ownedPart = if (have > 0) itemValue(itemId, exact, ctx) * have else 0L

        val npc = ctx.npcPrice(itemId)
        val mall = if (exact) {
            ctx.mallPrice(itemId)
        } else {
            ctx.historicalMallPrice(itemId).takeIf { it > 0 } ?: ctx.mallPrice(itemId)
        }
        var buy = UNAVAILABLE
        if (npc > 0) buy = minOf(buy, npc * need)
        if (mall > 0) buy = minOf(buy, mall * need)

        val make = priceToMake(itemId, need, exact, ctx, depth + 1)
        val bestNeed = when {
            buy >= UNAVAILABLE -> make
            make >= UNAVAILABLE -> buy
            else -> minOf(buy, make)
        }
        if (bestNeed >= UNAVAILABLE) return UNAVAILABLE
        return ownedPart + bestNeed
    }

    fun cheaperToBuy(itemId: Int, qty: Int, ctx: PriceContext): Boolean {
        if (!ItemDatabase.isTradeable(itemId)) return false
        val mall = ctx.mallPrice(itemId)
        if (mall <= 0) return false
        val make = priceToMake(itemId, qty, exact = false, ctx = ctx)
        if (make >= UNAVAILABLE) return true
        if (mall / 2 < make && make / 2 < mall) {
            val mallExact = ctx.mallPrice(itemId)
            if (mallExact <= 0) return false
            val makeExact = priceToMake(itemId, qty, exact = true, ctx = ctx)
            if (makeExact >= UNAVAILABLE) return true
            return mallExact < makeExact
        }
        return mall < make
    }

    /** Desktop [InventoryManager.invokeBuyScript] — returns [defaultBuy] when no buyScript. */
    fun invokeBuyScript(
        prefs: Preferences?,
        itemName: String,
        qty: Int,
        ingredientLevel: Int,
        defaultBuy: Boolean,
        runScript: ((String, List<String>) -> Boolean)? = null,
    ): Boolean {
        val script = prefs?.getString("buyScript", "")?.trim().orEmpty()
        if (script.isEmpty()) return defaultBuy
        val runner = runScript ?: return defaultBuy
        return try {
            runner(
                script,
                listOf(itemName, qty.toString(), ingredientLevel.toString(), defaultBuy.toString()),
            )
        } catch (_: Exception) {
            defaultBuy
        }
    }

    fun retrievePrice(itemId: Int, ctx: PriceContext): Long {
        val price = priceToAcquire(itemId, 1, exact = true, ctx = ctx)
        return if (price >= UNAVAILABLE) -1L else price
    }
}
