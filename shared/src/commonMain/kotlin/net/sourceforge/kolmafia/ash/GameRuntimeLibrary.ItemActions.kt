package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ConcoctionConsumptionType
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.session.ConcoctionQueueRunner

internal suspend fun GameRuntimeLibrary.familiarFeedItem(
    itemId: Int,
    quantity: Int,
    type: ConcoctionConsumptionType,
): Boolean {
    val itemName = gameDatabase?.item(itemId)?.name
        ?: ItemDatabase.getById(itemId)?.name
        ?: return false
    if (!ConcoctionQueueRunner.isFamiliarFeedEligible(itemName, type)) return false
    val activeFamiliarId = familiarManager?.state?.value?.activeFamiliar?.id
    ConcoctionQueueRunner.preflightBingeWithFamiliar(type, activeFamiliarId)
        .onFailure { return false }
    val retrieve = retrieveItemService ?: return false
    if (retrieve.retrieve(itemId, quantity) < quantity) return false
    val use = useItemRequest ?: return false
    return when (type) {
        ConcoctionConsumptionType.STOCKING_MIMIC -> use.feedCandy(itemId, quantity).isSuccess
        ConcoctionConsumptionType.ROBORTENDER -> {
            repeat(quantity) {
                if (use.robooze(itemId).isFailure) return false
            }
            true
        }
        ConcoctionConsumptionType.GLUTTONOUS_GHOST,
        ConcoctionConsumptionType.SPIRIT_HOBO,
        ConcoctionConsumptionType.SLIMELING,
        -> use.binge(itemId, quantity).isSuccess
        else -> false
    }
}

internal fun GameRuntimeLibrary.registerItemActions(scope: AshScope) {

    // helper: resolve item name → game ID (Int), null if unknown
    fun resolveItemId(itemName: String): Int? = gameDatabase?.item(itemName)?.id

    fun registerFamiliarFeedAsh(
        name: String,
        type: ConcoctionConsumptionType,
        fixedQty: Int? = null,
    ) {
        regFn(scope, name, AshType.BOOLEAN,
            listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
            val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
            val qty = fixedQty ?: args[0].toLong().toInt()
            AshValue.of(kotlinx.coroutines.runBlocking { familiarFeedItem(itemId, qty, type) })
        }
        regFn(scope, name, AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { _, args ->
            val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
            val qty = fixedQty ?: 1
            AshValue.of(kotlinx.coroutines.runBlocking { familiarFeedItem(itemId, qty, type) })
        }
    }

    registerFamiliarFeedAsh("ghost", ConcoctionConsumptionType.GLUTTONOUS_GHOST)
    registerFamiliarFeedAsh("hobo", ConcoctionConsumptionType.SPIRIT_HOBO)
    registerFamiliarFeedAsh("slimeling", ConcoctionConsumptionType.SLIMELING)
    registerFamiliarFeedAsh("robo", ConcoctionConsumptionType.ROBORTENDER, fixedQty = 1)

    // 1. use(qty: int, it: item) → boolean
    regFn(scope, "use", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = useItemRequest ?: return@regFn AshValue.of(false)
        val result = kotlinx.coroutines.runBlocking { req.use(itemId, qty) }
        result.getOrNull()?.let { applyItemUseResponse(itemId, it) }
        AshValue.of(result.isSuccess)
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

    // sell(qty: int, it: item) → boolean — autosell alias (desktop economy sell)
    regFn(scope, "sell", AshType.BOOLEAN,
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

    // 8. put_shop(price: int, limit: int, it: item) → boolean
    regFn(scope, "put_shop", AshType.BOOLEAN,
        listOf("price" to AshType.INT, "limit" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[2].toString()) ?: return@regFn AshValue.of(false)
        val price = args[0].toLong().toInt()
        val limit = args[1].toLong().toInt()
        val req = manageStoreRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.addItem(itemId, price, limit).isSuccess })
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
    // Desktop semantic: bypasses the inebriety cap entirely (server allows over-limit).
    // Mobile simplification: identical to drinksilent() — both call DrinkBoozeRequest.drink()
    // since mobile has no client-side inebriety guard. Scripts calling overdrink() get the
    // correct HTTP action; the cap-bypass distinction only matters for desktop UI suppression.
    regFn(scope, "overdrink", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = drinkBoozeRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.drink(itemId, qty) }.isSuccess)
    }

    // 13. put_display(qty: int, it: item) → boolean — move item from backpack to display case
    regFn(scope, "put_display", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = displayCaseRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.putIn(itemId, qty) }.isSuccess)
    }

    // 14. take_display(qty: int, it: item) → boolean — move item from display case to backpack
    regFn(scope, "take_display", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = displayCaseRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.takeOut(itemId, qty) }.isSuccess)
    }

    // 15. put_stash(qty: int, it: item) → boolean — contribute item to clan stash
    regFn(scope, "put_stash", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = clanStashRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.putIn(itemId, qty) }.isSuccess)
    }

    // 16. take_stash(qty: int, it: item) → boolean — take item from clan stash
    regFn(scope, "take_stash", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = clanStashRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.takeOut(itemId, qty) }.isSuccess)
    }

    // 17. empty_closet() → boolean — take all items from closet
    regFn(scope, "empty_closet", AshType.BOOLEAN, emptyList()) { _, _ ->
        val req = closetRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.emptyCloset().isSuccess })
    }
}
