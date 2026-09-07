package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.data.SpeakeasyAvailability
import net.sourceforge.kolmafia.data.SpeakeasyDatabase
import net.sourceforge.kolmafia.item.RetrievePricing

internal fun GameRuntimeLibrary.registerPricingQueries(scope: AshScope) {

    regFn(scope, "autosell_price", AshType.INT,
        listOf("it" to AshType.ITEM)) { _, args ->
        val price = gameDatabase?.item(args[0].toString())?.autosellPrice ?: 0
        AshValue.of(price.toLong())
    }

    // Desktop: NPC store price, else available speakeasy drink cost
    regFn(scope, "npc_price", AshType.INT,
        listOf("it" to AshType.ITEM)) { _, args ->
        val itemName = args[0].toString()
        val item = gameDatabase?.item(itemName) ?: ItemDatabase.getByName(itemName)
        val npc = when {
            item != null && NpcStoreDatabase.containsItem(item.id) ->
                NpcStoreDatabase.npcPrice(item.name).takeIf { it > 0 }
                    ?: gameDatabase?.npcPrice(item.name)?.takeIf { it > 0 }
            else -> gameDatabase?.npcPrice(itemName)?.takeIf { it > 0 }
                ?: NpcStoreDatabase.npcPrice(itemName).takeIf { it > 0 }
        }
        if (npc != null && npc > 0) return@regFn AshValue.of(npc.toLong())
        val speakName = item?.name ?: itemName
        if (SpeakeasyAvailability.isAvailable(speakName)) {
            val cost = SpeakeasyDatabase.nameToCost(speakName)
            if (cost > 0) return@regFn AshValue.of(cost.toLong())
        }
        AshValue.ZERO
    }

    // mall_price(it: item) → int — desktop anti-mallbot fifth-cheapest price
    regFn(scope, "mall_price", AshType.INT,
        listOf("it" to AshType.ITEM)) { _, args ->
        val itemName = args[0].toString()
        val itemId = gameDatabase?.item(itemName)?.id ?: return@regFn AshValue.ZERO
        val price = kotlinx.coroutines.runBlocking {
            if (mallPriceManager != null) mallManager?.getMallPrice(itemId) ?: -1L
            else mallManager?.cheapestPrice(itemName) ?: -1L
        }
        AshValue.of(price)
    }

    regFn(scope, "mall_price", AshType.INT,
        listOf("it" to AshType.ITEM, "maxAge" to AshType.FLOAT)) { _, args ->
        val itemId = gameDatabase?.item(args[0].toString())?.id ?: return@regFn AshValue.ZERO
        val price = kotlinx.coroutines.runBlocking {
            mallManager?.getMallPrice(itemId, args[1].toDouble()) ?: -1L
        }
        AshValue.of(price)
    }

    // retrieve_price(it: item) → int — cheapest acquisition (mall/NPC/create)
    fun priceContextFor(itemId: Int, itemName: String): RetrievePricing.PriceContext {
        val mall = kotlinx.coroutines.runBlocking {
            if (mallPriceManager != null) mallManager?.getMallPrice(itemId) ?: -1L
            else mallManager?.cheapestPrice(itemName) ?: -1L
        }
        val historical = mallPriceManager?.getHistoricalPrice(itemId) ?: 0L
        return RetrievePricing.PriceContext(
            inventoryCount = { id ->
                inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0
            },
            mallPrice = { id ->
                if (id == itemId) mall else (mallPriceManager?.getMallPrice(id) ?: -1L)
            },
            historicalMallPrice = { id ->
                if (id == itemId && historical > 0) historical
                else mallPriceManager?.getHistoricalPrice(id) ?: 0L
            },
            npcPrice = { id ->
                val name = ItemDatabase.getItemName(id)
                if (name.isBlank()) 0L else NpcStoreDatabase.npcPrice(name).toLong()
            },
            prefs = preferences,
            canCreate = { true },
        )
    }

    fun resolvePriceItem(name: String): Int? =
        gameDatabase?.item(name)?.id ?: ItemDatabase.getByName(name)?.id

    regFn(scope, "retrieve_price", AshType.INT,
        listOf("it" to AshType.ITEM)) { _, args ->
        val itemName = args[0].toString()
        val itemId = resolvePriceItem(itemName) ?: return@regFn AshValue.of(-1L)
        AshValue.of(RetrievePricing.retrievePrice(itemId, priceContextFor(itemId, itemName)))
    }
    regFn(scope, "retrieve_price", AshType.INT,
        listOf("it" to AshType.ITEM, "count" to AshType.INT)) { _, args ->
        val itemName = args[0].toString()
        val itemId = resolvePriceItem(itemName) ?: return@regFn AshValue.of(-1L)
        val qty = args[1].toLong().toInt()
        val price = RetrievePricing.priceToAcquire(itemId, qty, exact = true, priceContextFor(itemId, itemName))
        AshValue.of(if (price >= RetrievePricing.UNAVAILABLE) -1L else price)
    }
    regFn(scope, "retrieve_price", AshType.INT,
        listOf("count" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemName = args[1].toString()
        val itemId = resolvePriceItem(itemName) ?: return@regFn AshValue.of(-1L)
        val qty = args[0].toLong().toInt()
        val price = RetrievePricing.priceToAcquire(itemId, qty, exact = true, priceContextFor(itemId, itemName))
        AshValue.of(if (price >= RetrievePricing.UNAVAILABLE) -1L else price)
    }
    regFn(scope, "retrieve_price", AshType.INT,
        listOf("it" to AshType.ITEM, "count" to AshType.INT, "exact" to AshType.BOOLEAN)) { _, args ->
        val itemName = args[0].toString()
        val itemId = resolvePriceItem(itemName) ?: return@regFn AshValue.of(-1L)
        val qty = args[1].toLong().toInt()
        val exact = args[2].toBoolean()
        val price = RetrievePricing.priceToAcquire(itemId, qty, exact, priceContextFor(itemId, itemName))
        AshValue.of(if (price >= RetrievePricing.UNAVAILABLE) -1L else price)
    }
    regFn(scope, "retrieve_price", AshType.INT,
        listOf("count" to AshType.INT, "it" to AshType.ITEM, "exact" to AshType.BOOLEAN)) { _, args ->
        val itemName = args[1].toString()
        val itemId = resolvePriceItem(itemName) ?: return@regFn AshValue.of(-1L)
        val qty = args[0].toLong().toInt()
        val exact = args[2].toBoolean()
        val price = RetrievePricing.priceToAcquire(itemId, qty, exact, priceContextFor(itemId, itemName))
        AshValue.of(if (price >= RetrievePricing.UNAVAILABLE) -1L else price)
    }

    regFn(scope, "historical_price", AshType.INT,
        listOf("it" to AshType.ITEM)) { _, args ->
        val itemId = gameDatabase?.item(args[0].toString())?.id ?: return@regFn AshValue.ZERO
        AshValue.of(mallPriceManager?.getHistoricalPrice(itemId) ?: 0L)
    }

    regFn(scope, "historical_age", AshType.INT,
        listOf("it" to AshType.ITEM)) { _, args ->
        val itemId = gameDatabase?.item(args[0].toString())?.id ?: return@regFn AshValue.of(-1L)
        AshValue.of(mallPriceManager?.getHistoricalAge(itemId) ?: -1L)
    }
}
