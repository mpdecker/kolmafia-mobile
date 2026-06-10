package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerShopFunctions(scope: AshScope) {

    fun resolveItemId(itemName: String): Int? = gameDatabase?.item(itemName)?.id

    regFn(scope, "take_shop", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = manageStoreRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.removeItem(itemId, qty).isSuccess })
    }

    regFn(scope, "reprice_shop", AshType.BOOLEAN,
        listOf("price" to AshType.INT, "limit" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[2].toString()) ?: return@regFn AshValue.of(false)
        val price = args[0].toLong().toInt()
        val limit = args[1].toLong().toInt()
        val req = manageStoreRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.repriceItem(itemId, price, limit).isSuccess })
    }

    regFn(scope, "refresh_shop", AshType.BOOLEAN, emptyList()) { _, _ ->
        val req = manageStoreRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.refreshPrices().isSuccess })
    }
}
