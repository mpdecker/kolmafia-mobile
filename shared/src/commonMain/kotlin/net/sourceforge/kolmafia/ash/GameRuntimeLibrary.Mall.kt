package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerMallFunctions(scope: AshScope) {

    fun resolveItemId(itemName: String): Int? = gameDatabase?.item(itemName)?.id

    // buy(count: int, it: item) → int
    regFn(scope, "buy", AshType.INT,
        listOf("count" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(0L)
        val count = args[0].toLong().toInt()
        val purchased = kotlinx.coroutines.runBlocking {
            mallManager?.buy(itemId, count) ?: 0
        }
        AshValue.of(purchased.toLong())
    }

    // buy(count: int, it: item, maxPrice: int) → int
    regFn(scope, "buy", AshType.INT,
        listOf("count" to AshType.INT, "it" to AshType.ITEM, "maxPrice" to AshType.INT)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(0L)
        val count = args[0].toLong().toInt()
        val maxPrice = args[2].toLong().toInt()
        val purchased = kotlinx.coroutines.runBlocking {
            mallManager?.buy(itemId, count, maxPrice) ?: 0
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
    // Desktop: retrieve=false means "check only". Mobile: always attempts retrieval.
    regFn(scope, "retrieve_item", AshType.BOOLEAN,
        listOf("count" to AshType.INT, "it" to AshType.ITEM, "retrieve" to AshType.BOOLEAN)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val count = args[0].toLong().toInt()
        val retrieved = kotlinx.coroutines.runBlocking {
            retrieveItemService?.retrieve(itemId, count) ?: 0
        }
        AshValue.of(retrieved >= count)
    }
}
