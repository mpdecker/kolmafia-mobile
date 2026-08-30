package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.item.RetrievePricing

internal fun GameRuntimeLibrary.registerPricingQueries(scope: AshScope) {

    regFn(scope, "autosell_price", AshType.INT,
        listOf("it" to AshType.ITEM)) { _, args ->
        val price = gameDatabase?.item(args[0].toString())?.autosellPrice ?: 0
        AshValue.of(price.toLong())
    }

    regFn(scope, "npc_price", AshType.INT,
        listOf("it" to AshType.ITEM)) { _, args ->
        val price = gameDatabase?.npcPrice(args[0].toString()) ?: 0
        AshValue.of(price.toLong())
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
    regFn(scope, "retrieve_price", AshType.INT,
        listOf("it" to AshType.ITEM)) { _, args ->
        val itemName = args[0].toString()
        val itemId = gameDatabase?.item(itemName)?.id
            ?: ItemDatabase.getByName(itemName)?.id
            ?: return@regFn AshValue.of(-1L)
        val mall = kotlinx.coroutines.runBlocking {
            if (mallPriceManager != null) mallManager?.getMallPrice(itemId) ?: -1L
            else mallManager?.cheapestPrice(itemName) ?: -1L
        }
        val historical = mallPriceManager?.getHistoricalPrice(itemId) ?: 0L
        val ctx = RetrievePricing.PriceContext(
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
        AshValue.of(RetrievePricing.retrievePrice(itemId, ctx))
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
