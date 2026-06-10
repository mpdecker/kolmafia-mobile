package net.sourceforge.kolmafia.ash

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

    // mall_price(it: item) → int — cheapest listed mall price; -1 if not found
    regFn(scope, "mall_price", AshType.INT,
        listOf("it" to AshType.ITEM)) { _, args ->
        val itemName = args[0].toString()
        val price = kotlinx.coroutines.runBlocking {
            mallManager?.cheapestPrice(itemName) ?: -1L
        }
        AshValue.of(price)
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
