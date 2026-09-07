package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.equipment.OutfitCheckpoint

internal fun GameRuntimeLibrary.registerMallFunctions(scope: AshScope) {

    fun resolveItemId(itemName: String): Int? = gameDatabase?.item(itemName)?.id

    fun canInteract(): Boolean {
        val cs = character?.state?.value ?: return true
        return !cs.isHardcore && !cs.isInRonin
    }

    fun invCount(itemId: Int): Int =
        inventoryManager?.getCount(itemId) ?: 0

    suspend fun storageCount(itemId: Int): Int =
        storageRequest?.fetchContents()?.get(itemId) ?: 0

    suspend fun buyFromMall(itemId: Int, count: Int, maxPrice: Int = Int.MAX_VALUE): Int {
        val checkpoint = if (character != null && equipmentRequest != null && gameDatabase != null) {
            OutfitCheckpoint.snapshot(character!!, equipmentRequest!!, gameDatabase!!)
        } else null
        return if (checkpoint != null) {
            checkpoint.use { mallManager?.buy(itemId, count, maxPrice) ?: 0 }
        } else {
            mallManager?.buy(itemId, count, maxPrice) ?: 0
        }
    }

    suspend fun buyViaCli(itemId: Int, itemName: String, count: Int, maxPrice: Int = Int.MAX_VALUE): Int =
        buyOneCliItem(
            itemId = itemId,
            itemName = itemName,
            qty = count,
            maxPrice = maxPrice,
            forceMall = false,
            npcOnly = false,
            canInteract = canInteract(),
        )

    // Desktop: buy(item) → boolean via inventory delta after CLI NPC/mall routing
    regFn(scope, "buy", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { _, args ->
        val itemName = args[0].toString()
        val itemId = resolveItemId(itemName) ?: return@regFn AshValue.FALSE
        val ok = kotlinx.coroutines.runBlocking {
            val initial = invCount(itemId)
            val bought = buyViaCli(itemId, itemName, 1)
            invCount(itemId) >= initial + 1 && bought >= 1
        }
        AshValue.of(ok)
    }

    // Desktop: buy(count, item) / buy(item, count) → boolean
    regFn(scope, "buy", AshType.BOOLEAN,
        listOf("count" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val count = args[0].toLong().toInt()
        if (count <= 0) return@regFn AshValue.TRUE
        val itemName = args[1].toString()
        val itemId = resolveItemId(itemName) ?: return@regFn AshValue.FALSE
        val ok = kotlinx.coroutines.runBlocking {
            val initial = invCount(itemId)
            val bought = buyViaCli(itemId, itemName, count)
            invCount(itemId) >= initial + count && bought >= count
        }
        AshValue.of(ok)
    }
    regFn(scope, "buy", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "count" to AshType.INT)) { _, args ->
        val count = args[1].toLong().toInt()
        if (count <= 0) return@regFn AshValue.TRUE
        val itemName = args[0].toString()
        val itemId = resolveItemId(itemName) ?: return@regFn AshValue.FALSE
        val ok = kotlinx.coroutines.runBlocking {
            val initial = invCount(itemId)
            val bought = buyViaCli(itemId, itemName, count)
            invCount(itemId) >= initial + count && bought >= count
        }
        AshValue.of(ok)
    }

    // Desktop: buy(count, item, maxPrice) / buy(item, count, maxPrice) → int purchased (delta)
    regFn(scope, "buy", AshType.INT,
        listOf("count" to AshType.INT, "it" to AshType.ITEM, "maxPrice" to AshType.INT)) { _, args ->
        val count = args[0].toLong().toInt()
        if (count <= 0) return@regFn AshValue.of(0L)
        val itemName = args[1].toString()
        val itemId = resolveItemId(itemName) ?: return@regFn AshValue.of(0L)
        val maxPrice = args[2].toLong().toInt()
        val purchased = kotlinx.coroutines.runBlocking {
            val initial = invCount(itemId)
            buyViaCli(itemId, itemName, count, maxPrice)
            (invCount(itemId) - initial).coerceAtLeast(0)
        }
        AshValue.of(purchased.toLong())
    }
    regFn(scope, "buy", AshType.INT,
        listOf("it" to AshType.ITEM, "count" to AshType.INT, "maxPrice" to AshType.INT)) { _, args ->
        val count = args[1].toLong().toInt()
        if (count <= 0) return@regFn AshValue.of(0L)
        val itemName = args[0].toString()
        val itemId = resolveItemId(itemName) ?: return@regFn AshValue.of(0L)
        val maxPrice = args[2].toLong().toInt()
        val purchased = kotlinx.coroutines.runBlocking {
            val initial = invCount(itemId)
            buyViaCli(itemId, itemName, count, maxPrice)
            (invCount(itemId) - initial).coerceAtLeast(0)
        }
        AshValue.of(purchased.toLong())
    }

    // buy_using_storage(it: item) → boolean
    regFn(scope, "buy_using_storage", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM)) { _, args ->
        if (canInteract()) return@regFn AshValue.FALSE
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.FALSE
        val ok = kotlinx.coroutines.runBlocking {
            val initial = storageCount(itemId)
            val bought = buyFromMall(itemId, 1)
            bought > 0 && storageCount(itemId) == initial + bought
        }
        AshValue.of(ok)
    }

    // buy_using_storage(count, item) / (item, count) → boolean
    regFn(scope, "buy_using_storage", AshType.BOOLEAN,
        listOf("count" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        if (canInteract()) return@regFn AshValue.FALSE
        val count = args[0].toLong().toInt()
        if (count <= 0) return@regFn AshValue.TRUE
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.FALSE
        val ok = kotlinx.coroutines.runBlocking {
            val initial = storageCount(itemId)
            val bought = buyFromMall(itemId, count)
            storageCount(itemId) == initial + bought
        }
        AshValue.of(ok)
    }
    regFn(scope, "buy_using_storage", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "count" to AshType.INT)) { _, args ->
        if (canInteract()) return@regFn AshValue.FALSE
        val count = args[1].toLong().toInt()
        if (count <= 0) return@regFn AshValue.TRUE
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.FALSE
        val ok = kotlinx.coroutines.runBlocking {
            val initial = storageCount(itemId)
            val bought = buyFromMall(itemId, count)
            storageCount(itemId) == initial + bought
        }
        AshValue.of(ok)
    }

    // buy_using_storage(count, item, maxPrice) / (item, count, maxPrice) → int
    regFn(scope, "buy_using_storage", AshType.INT,
        listOf("count" to AshType.INT, "it" to AshType.ITEM, "maxPrice" to AshType.INT)) { _, args ->
        if (canInteract()) return@regFn AshValue.of(0L)
        val count = args[0].toLong().toInt()
        if (count <= 0) return@regFn AshValue.of(0L)
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(0L)
        val maxPrice = args[2].toLong().toInt()
        val purchased = kotlinx.coroutines.runBlocking {
            val initial = storageCount(itemId)
            buyFromMall(itemId, count, maxPrice)
            storageCount(itemId) - initial
        }
        AshValue.of(purchased.toLong())
    }
    regFn(scope, "buy_using_storage", AshType.INT,
        listOf("it" to AshType.ITEM, "count" to AshType.INT, "maxPrice" to AshType.INT)) { _, args ->
        if (canInteract()) return@regFn AshValue.of(0L)
        val count = args[1].toLong().toInt()
        if (count <= 0) return@regFn AshValue.of(0L)
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(0L)
        val maxPrice = args[2].toLong().toInt()
        val purchased = kotlinx.coroutines.runBlocking {
            val initial = storageCount(itemId)
            buyFromMall(itemId, count, maxPrice)
            storageCount(itemId) - initial
        }
        AshValue.of(purchased.toLong())
    }

    // retrieve_item(item) / (count, item) / (item, count)
    regFn(scope, "retrieve_item", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        val retrieved = kotlinx.coroutines.runBlocking {
            retrieveItemService?.retrieve(itemId, 1) ?: 0
        }
        AshValue.of(retrieved >= 1)
    }
    regFn(scope, "retrieve_item", AshType.BOOLEAN,
        listOf("count" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val count = args[0].toLong().toInt()
        val retrieved = kotlinx.coroutines.runBlocking {
            retrieveItemService?.retrieve(itemId, count) ?: 0
        }
        AshValue.of(retrieved >= count)
    }
    regFn(scope, "retrieve_item", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "count" to AshType.INT)) { _, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        val count = args[1].toLong().toInt()
        val retrieved = kotlinx.coroutines.runBlocking {
            retrieveItemService?.retrieve(itemId, count) ?: 0
        }
        AshValue.of(retrieved >= count)
    }

    // retrieve_item(count, item, retrieve) — mobile check-only extension
    regFn(scope, "retrieve_item", AshType.BOOLEAN,
        listOf("count" to AshType.INT, "it" to AshType.ITEM, "retrieve" to AshType.BOOLEAN)) { _, args ->
        val itemName = args[1].toString()
        val itemId = resolveItemId(itemName) ?: return@regFn AshValue.of(false)
        val count = args[0].toLong().toInt()
        val doRetrieve = args[2].toBoolean()
        if (!doRetrieve) {
            val accessible = kotlinx.coroutines.runBlocking {
                physicalAccessibleCount(itemId, itemName)
            }
            return@regFn AshValue.of(accessible >= count)
        }
        val retrieved = kotlinx.coroutines.runBlocking {
            retrieveItemService?.retrieve(itemId, count) ?: 0
        }
        AshValue.of(retrieved >= count)
    }
}
