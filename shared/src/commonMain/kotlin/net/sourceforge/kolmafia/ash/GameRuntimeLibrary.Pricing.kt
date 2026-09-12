package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.data.SpeakeasyAvailability
import net.sourceforge.kolmafia.data.SpeakeasyDatabase
import net.sourceforge.kolmafia.data.isCreateSupported
import net.sourceforge.kolmafia.item.RetrievePricing
import net.sourceforge.kolmafia.mall.MallPriceDatabase

internal fun GameRuntimeLibrary.registerPricingQueries(scope: AshScope) {

    regFn(scope, "autosell_price", AshType.INT,
        listOf("it" to AshType.ITEM)) { _, args ->
        val price = gameDatabase?.item(args[0].toString())?.autosellPrice ?: 0
        AshValue.of(price.toLong())
    }

    // Desktop: NPC store price with validate=true, else available speakeasy drink cost
    regFn(scope, "npc_price", AshType.INT,
        listOf("it" to AshType.ITEM)) { _, args ->
        val itemName = args[0].toString()
        val item = gameDatabase?.item(itemName) ?: ItemDatabase.getByName(itemName)
        val itemId = item?.id
        val state = character?.state?.value ?: CharacterState()
        val npcAccessible = if (itemId != null && itemId > 0) {
            NpcStoreDatabase.containsItem(
                itemId,
                validate = true,
                state = state,
                prefs = preferences,
                accessibleCount = { id ->
                    inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0
                },
            )
        } else {
            false
        }
        if (npcAccessible) {
            val npc = NpcStoreDatabase.npcPrice(item?.name ?: itemName).takeIf { it > 0 }
                ?: gameDatabase?.npcPrice(item?.name ?: itemName)?.takeIf { it > 0 }
            if (npc != null && npc > 0) return@regFn AshValue.of(npc.toLong())
        }
        // Speakeasy residual: lounge-visit availability (item id or canonical name).
        val speakName = item?.name ?: itemName
        val speakeasyOk = when {
            itemId != null && itemId > 0 && SpeakeasyAvailability.isAvailableItemId(itemId) -> true
            SpeakeasyAvailability.isAvailable(speakName) -> true
            else -> false
        }
        if (speakeasyOk) {
            val cost = SpeakeasyDatabase.nameToCost(speakName)
            if (cost > 0) return@regFn AshValue.of(cost.toLong())
        }
        // Soft fallback when validate gates are unavailable (headless / no character):
        // still honor static NPC catalog prices without accessibility checks.
        if (character == null) {
            val npc = when {
                item != null && NpcStoreDatabase.containsItem(item.id) ->
                    NpcStoreDatabase.npcPrice(item.name).takeIf { it > 0 }
                        ?: gameDatabase?.npcPrice(item.name)?.takeIf { it > 0 }
                else -> gameDatabase?.npcPrice(itemName)?.takeIf { it > 0 }
                    ?: NpcStoreDatabase.npcPrice(itemName).takeIf { it > 0 }
            }
            if (npc != null && npc > 0) return@regFn AshValue.of(npc.toLong())
        }
        AshValue.ZERO
    }

    fun resolveMallItemId(name: String): Int? =
        gameDatabase?.item(name)?.id ?: ItemDatabase.getByName(name)?.id

    // mall_price(it: item) → int — desktop anti-mallbot fifth-cheapest; forceUpdate prefetch on miss
    regFn(scope, "mall_price", AshType.INT,
        listOf("it" to AshType.ITEM)) { _, args ->
        val itemName = args[0].toString()
        val itemId = resolveMallItemId(itemName) ?: return@regFn AshValue.ZERO
        val mpm = mallPriceManager
        if (mpm != null && !mpm.validMallItem(itemId)) return@regFn AshValue.ZERO
        val price = kotlinx.coroutines.runBlocking {
            mpm?.getMallPrice(itemId)?.takeIf { it > 0 }
                ?: mpm?.prefetchMallPrice(itemId)?.takeIf { it > 0 }
                ?: mallManager?.getMallPrice(itemId)?.takeIf { it > 0 }
                ?: mallManager?.cheapestPrice(itemName)?.takeIf { it > 0 }
                ?: 0L
        }
        AshValue.of(price.coerceAtLeast(0L))
    }

    // mall_price(item, maxAge) — age gate in fractional days; stale → forceUpdate prefetch
    regFn(scope, "mall_price", AshType.INT,
        listOf("it" to AshType.ITEM, "maxAge" to AshType.FLOAT)) { _, args ->
        val itemId = resolveMallItemId(args[0].toString()) ?: return@regFn AshValue.ZERO
        val maxAgeDays = args[1].toDouble()
        val mpm = mallPriceManager
        if (mpm != null && !mpm.validMallItem(itemId)) return@regFn AshValue.ZERO
        val price = kotlinx.coroutines.runBlocking {
            if (mpm != null) {
                val ageDays = mpm.getHistoricalAgeDays(itemId)
                val fresh = ageDays.isFinite() && ageDays <= maxAgeDays
                val cached = mpm.getHistoricalPrice(itemId)
                when {
                    fresh && cached > 0 -> cached
                    else -> mpm.prefetchMallPrice(itemId).takeIf { it > 0 }
                        ?: mpm.getMallPriceDays(itemId, maxAgeDays).takeIf { it > 0 }
                        ?: 0L
                }
            } else {
                mallManager?.getMallPrice(itemId, maxAgeDays)?.takeIf { it > 0 } ?: 0L
            }
        }
        AshValue.of(price.coerceAtLeast(0L))
    }

    // retrieve_price — desktop InventoryManager.priceToAcquire (accessible on-hand + canCreate)
    fun priceContextFor(itemId: Int, itemName: String): RetrievePricing.PriceContext {
        val mall = kotlinx.coroutines.runBlocking {
            mallPriceManager?.getMallPrice(itemId)?.takeIf { it > 0 }
                ?: mallPriceManager?.prefetchMallPrice(itemId)?.takeIf { it > 0 }
                ?: mallManager?.getMallPrice(itemId)?.takeIf { it > 0 }
                ?: mallManager?.cheapestPrice(itemName)?.takeIf { it > 0 }
                ?: -1L
        }
        val historical = mallPriceManager?.getHistoricalPrice(itemId)
            ?: MallPriceDatabase.getPrice(itemId)
        return RetrievePricing.PriceContext(
            inventoryCount = { id ->
                RetrievePricing.accessibleOnHandCount(
                    itemId = id,
                    inventoryCount = { inventoryManager?.getCount(it) ?: 0 },
                    physicalAccessible = { aid ->
                        val name = gameDatabase?.item(aid)?.name
                            ?: ItemDatabase.getItemName(aid)
                        if (name.isBlank()) 0
                        else kotlinx.coroutines.runBlocking { physicalAccessibleCount(aid, name) }
                    },
                )
            },
            mallPrice = { id ->
                if (id == itemId) mall else (mallPriceManager?.getMallPrice(id) ?: -1L)
            },
            historicalMallPrice = { id ->
                if (id == itemId && historical > 0) historical
                else mallPriceManager?.getHistoricalPrice(id)
                    ?: MallPriceDatabase.getPrice(id)
            },
            npcPrice = { id ->
                val name = gameDatabase?.item(id)?.name
                    ?: ItemDatabase.getItemName(id)
                if (name.isBlank()) 0L
                else {
                    gameDatabase?.npcPrice(name)?.takeIf { it > 0 }?.toLong()
                        ?: NpcStoreDatabase.npcPrice(name).toLong()
                }
            },
            prefs = preferences,
            canCreate = { id ->
                val name = gameDatabase?.item(id)?.name ?: ItemDatabase.getItemName(id)
                if (name.isBlank()) false
                else {
                    val conc = ConcoctionDatabase.getByResult(name)
                    conc?.isCreateSupported() == true
                }
            },
        )
    }

    fun resolvePriceItem(name: String): Int? =
        gameDatabase?.item(name)?.id ?: ItemDatabase.getByName(name)?.id

    fun retrievePriceValue(itemId: Int, itemName: String, qty: Int, exact: Boolean): AshValue {
        if (qty <= 0) return AshValue.ZERO
        val price = RetrievePricing.priceToAcquire(
            itemId, qty, exact, priceContextFor(itemId, itemName),
        )
        return AshValue.of(if (price >= RetrievePricing.UNAVAILABLE) -1L else price)
    }

    regFn(scope, "retrieve_price", AshType.INT,
        listOf("it" to AshType.ITEM)) { _, args ->
        val itemName = args[0].toString()
        val itemId = resolvePriceItem(itemName) ?: return@regFn AshValue.of(-1L)
        AshValue.of(RetrievePricing.retrievePrice(itemId, priceContextFor(itemId, itemName)))
    }
    // Desktop 2-arg defaults exact=false (historical mall age soft path)
    regFn(scope, "retrieve_price", AshType.INT,
        listOf("it" to AshType.ITEM, "count" to AshType.INT)) { _, args ->
        val qty = args[1].toLong().toInt()
        if (qty <= 0) return@regFn AshValue.ZERO
        val itemName = args[0].toString()
        val itemId = resolvePriceItem(itemName) ?: return@regFn AshValue.of(-1L)
        retrievePriceValue(itemId, itemName, qty, exact = false)
    }
    regFn(scope, "retrieve_price", AshType.INT,
        listOf("count" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val qty = args[0].toLong().toInt()
        if (qty <= 0) return@regFn AshValue.ZERO
        val itemName = args[1].toString()
        val itemId = resolvePriceItem(itemName) ?: return@regFn AshValue.of(-1L)
        retrievePriceValue(itemId, itemName, qty, exact = false)
    }
    regFn(scope, "retrieve_price", AshType.INT,
        listOf("it" to AshType.ITEM, "count" to AshType.INT, "exact" to AshType.BOOLEAN)) { _, args ->
        val qty = args[1].toLong().toInt()
        if (qty <= 0) return@regFn AshValue.ZERO
        val itemName = args[0].toString()
        val itemId = resolvePriceItem(itemName) ?: return@regFn AshValue.of(-1L)
        retrievePriceValue(itemId, itemName, qty, args[2].toBoolean())
    }
    regFn(scope, "retrieve_price", AshType.INT,
        listOf("count" to AshType.INT, "it" to AshType.ITEM, "exact" to AshType.BOOLEAN)) { _, args ->
        val qty = args[0].toLong().toInt()
        if (qty <= 0) return@regFn AshValue.ZERO
        val itemName = args[1].toString()
        val itemId = resolvePriceItem(itemName) ?: return@regFn AshValue.of(-1L)
        retrievePriceValue(itemId, itemName, qty, args[2].toBoolean())
    }

    // Desktop historical_price → MallPriceDatabase.getPrice only (no session-cache fallback)
    regFn(scope, "historical_price", AshType.INT,
        listOf("it" to AshType.ITEM)) { _, args ->
        val itemId = resolveMallItemId(args[0].toString()) ?: return@regFn AshValue.ZERO
        AshValue.of(MallPriceDatabase.getPrice(itemId).coerceAtLeast(0L))
    }

    // Desktop historical_age → FLOAT fractional days (Infinity when unknown / no DB row);
    // session cache age only when mallprices.txt has no row (day-gate polish residual).
    regFn(scope, "historical_age", AshType.FLOAT,
        listOf("it" to AshType.ITEM)) { _, args ->
        val itemId = resolveMallItemId(args[0].toString())
            ?: return@regFn AshValue.of(Double.POSITIVE_INFINITY)
        val dbAge = MallPriceDatabase.getAgeDays(itemId)
        if (dbAge.isFinite()) return@regFn AshValue.of(dbAge)
        val age = mallPriceManager?.getHistoricalAgeDays(itemId)
            ?: Double.POSITIVE_INFINITY
        AshValue.of(age)
    }
}
