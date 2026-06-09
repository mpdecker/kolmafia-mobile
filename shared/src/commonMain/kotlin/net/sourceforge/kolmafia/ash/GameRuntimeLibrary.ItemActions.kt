package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerItemActions(scope: AshScope) {

    // helper: resolve item name → game ID (Int), null if unknown
    fun resolveItemId(itemName: String): Int? = gameDatabase?.item(itemName)?.id

    // 1. use(qty: int, it: item) → boolean
    regFn(scope, "use", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = useItemRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.use(itemId, qty) }.isSuccess)
    }

    // 2. eat(qty: int, it: item) → boolean
    regFn(scope, "eat", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = eatFoodRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.eat(itemId, qty) }.isSuccess)
    }

    // 3. drink(qty: int, it: item) → boolean
    regFn(scope, "drink", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = drinkBoozeRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.drink(itemId, qty) }.isSuccess)
    }

    // 4. chew(qty: int, it: item) → boolean
    regFn(scope, "chew", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = chewRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.chew(itemId, qty) }.isSuccess)
    }

    // 5. autosell(qty: int, it: item) → boolean
    regFn(scope, "autosell", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = autosellRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.autosell(itemId, qty) }.isSuccess)
    }

    // 6. put_closet(qty: int, it: item) → boolean
    regFn(scope, "put_closet", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = closetRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.putIn(itemId, qty) }.isSuccess)
    }

    // 7. take_closet(qty: int, it: item) → boolean
    regFn(scope, "take_closet", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = closetRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.takeOut(itemId, qty) }.isSuccess)
    }

    // 8. put_shop(price: int, limit: int, it: item) → boolean  [STUB - return false]
    regFn(scope, "put_shop", AshType.BOOLEAN,
        listOf("price" to AshType.INT, "limit" to AshType.INT, "it" to AshType.ITEM)) { _, _ ->
        AshValue.of(false)
    }

    // 9. take_storage(qty: int, it: item) → boolean
    regFn(scope, "take_storage", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = storageRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.withdraw(itemId, qty) }.isSuccess)
    }

    // 10. eatsilent(qty: int, it: item) → boolean
    // Same as eat() — mobile has no client-side fullness guard; server enforces cap.
    regFn(scope, "eatsilent", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = eatFoodRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.eat(itemId, qty) }.isSuccess)
    }

    // 11. drinksilent(qty: int, it: item) → boolean
    // Same as drink() — mobile has no client-side inebriety guard.
    regFn(scope, "drinksilent", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = drinkBoozeRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.drink(itemId, qty) }.isSuccess)
    }

    // 12. overdrink(qty: int, it: item) → boolean
    // Allows drinking past the inebriety limit — same HTTP call as drink().
    regFn(scope, "overdrink", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = drinkBoozeRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.drink(itemId, qty) }.isSuccess)
    }
}
