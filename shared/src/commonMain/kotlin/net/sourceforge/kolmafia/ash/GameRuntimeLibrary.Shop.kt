package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.session.StoreManager

internal fun GameRuntimeLibrary.registerShopFunctions(scope: AshScope) {

    fun resolveItemId(itemName: String): Int? = gameDatabase?.item(itemName)?.id

    suspend fun ensureShopInventory() {
        val req = manageStoreRequest ?: return
        if (!StoreManager.soldItemsRetrieved) {
            req.fetchSoldItems()
        }
    }

    fun takeShop(rt: AshRuntimeContext, itemId: Int, qty: Int?, takeAll: Boolean): AshValue {
        if (!takeAll && qty != null && qty <= 0) return AshValue.TRUE
        if (isBatching(rt)) {
            val params = if (takeAll) pilcrowItemParamsAll(itemId) else pilcrowItemParams(qty ?: 1, itemId)
            batchCommand(rt, "shop", "take", params)
            return AshValue.TRUE
        }
        val req = manageStoreRequest ?: return AshValue.of(false)
        return AshValue.of(kotlinx.coroutines.runBlocking {
            ensureShopInventory()
            val count = if (takeAll) StoreManager.shopAmount(itemId) else (qty ?: 1)
            if (count <= 0) true else req.removeItem(itemId, count).isSuccess
        })
    }

    fun repriceShop(rt: AshRuntimeContext, itemId: Int, price: Int, limit: Int): AshValue {
        if (isBatching(rt)) {
            batchCommand(rt, "shop", "reprice", "\u00B6$itemId @ $price limit $limit")
            return AshValue.TRUE
        }
        val req = manageStoreRequest ?: return AshValue.of(false)
        return AshValue.of(kotlinx.coroutines.runBlocking {
            ensureShopInventory()
            req.repriceItem(itemId, price, limit).isSuccess
        })
    }

    // take_shop(item) — remove entire stocked quantity
    regFn(scope, "take_shop", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { rt, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        takeShop(rt, itemId, qty = null, takeAll = true)
    }

    regFn(scope, "take_shop", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { rt, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        takeShop(rt, itemId, args[0].toLong().toInt(), takeAll = false)
    }

    regFn(scope, "take_shop", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "qty" to AshType.INT)) { rt, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        takeShop(rt, itemId, args[1].toLong().toInt(), takeAll = false)
    }

    // reprice_shop(price, item) — keep existing daily limit
    regFn(scope, "reprice_shop", AshType.BOOLEAN,
        listOf("price" to AshType.INT, "it" to AshType.ITEM)) { rt, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val price = args[0].toLong().toInt()
        val limit = if (isBatching(rt)) {
            StoreManager.getLimit(itemId)
        } else {
            kotlinx.coroutines.runBlocking {
                ensureShopInventory()
                StoreManager.getLimit(itemId)
            }
        }
        repriceShop(rt, itemId, price, limit)
    }

    regFn(scope, "reprice_shop", AshType.BOOLEAN,
        listOf("price" to AshType.INT, "limit" to AshType.INT, "it" to AshType.ITEM)) { rt, args ->
        val itemId = resolveItemId(args[2].toString()) ?: return@regFn AshValue.of(false)
        repriceShop(rt, itemId, args[0].toLong().toInt(), args[1].toLong().toInt())
    }

    // Desktop refresh_shop posts ManageStoreRequest (sold-item fetch)
    regFn(scope, "refresh_shop", AshType.BOOLEAN, emptyList()) { _, _ ->
        val req = manageStoreRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking {
            req.fetchSoldItems().isSuccess
        })
    }
}
