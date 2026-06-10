package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.equipment.OutfitCheckpoint

internal fun GameRuntimeLibrary.registerMallFunctions(scope: AshScope) {

    fun resolveItemId(itemName: String): Int? = gameDatabase?.item(itemName)?.id

    fun canInteract(): Boolean {
        val cs = character?.state?.value ?: return true
        return !cs.isHardcore && !cs.isInRonin
    }

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

    // buy(count: int, it: item) → int
    regFn(scope, "buy", AshType.INT,
        listOf("count" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(0L)
        val count = args[0].toLong().toInt()
        val purchased = kotlinx.coroutines.runBlocking { buyFromMall(itemId, count) }
        AshValue.of(purchased.toLong())
    }

    // buy(count: int, it: item, maxPrice: int) → int
    regFn(scope, "buy", AshType.INT,
        listOf("count" to AshType.INT, "it" to AshType.ITEM, "maxPrice" to AshType.INT)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(0L)
        val count = args[0].toLong().toInt()
        val maxPrice = args[2].toLong().toInt()
        val purchased = kotlinx.coroutines.runBlocking { buyFromMall(itemId, count, maxPrice) }
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

    // buy_using_storage(count: int, it: item) → boolean
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

    // buy_using_storage(count: int, it: item, maxPrice: int) → int
    regFn(scope, "buy_using_storage", AshType.INT,
        listOf("count" to AshType.INT, "it" to AshType.ITEM, "maxPrice" to AshType.INT)) { _, args ->
        if (canInteract()) return@regFn AshValue.of(0L)
        val count = args[0].toLong().toInt()
        if (count <= 0) return@regFn AshValue.of(0L)
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(0L)
        val maxPrice = args[2].toLong().toInt()
        val purchased = kotlinx.coroutines.runBlocking {
            val initial = storageCount(itemId)
            val bought = buyFromMall(itemId, count, maxPrice)
            storageCount(itemId) - initial
        }
        AshValue.of(purchased.toLong())
    }

    // retrieve_item(count: int, it: item) → boolean
    regFn(scope, "retrieve_item", AshType.BOOLEAN,
        listOf("count" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val count = args[0].toLong().toInt()
        val retrieved = kotlinx.coroutines.runBlocking {
            retrieveItemService?.retrieve(itemId, count) ?: 0
        }
        AshValue.of(retrieved >= count)
    }

    // retrieve_item(count: int, it: item, retrieve: boolean) → boolean
    regFn(scope, "retrieve_item", AshType.BOOLEAN,
        listOf("count" to AshType.INT, "it" to AshType.ITEM, "retrieve" to AshType.BOOLEAN)) { _, args ->
        val itemName = args[1].toString()
        val itemId = resolveItemId(itemName) ?: return@regFn AshValue.of(false)
        val count = args[0].toLong().toInt()
        val doRetrieve = args[2].toBoolean()
        if (!doRetrieve) {
            val accessible = kotlinx.coroutines.runBlocking {
                outfitManager?.accessibleCount(itemId, itemName) ?: 0
            }
            return@regFn AshValue.of(accessible >= count)
        }
        val retrieved = kotlinx.coroutines.runBlocking {
            retrieveItemService?.retrieve(itemId, count) ?: 0
        }
        AshValue.of(retrieved >= count)
    }
}
