package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ConcoctionConsumptionType
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.CollectionCacheSync
import net.sourceforge.kolmafia.session.ConcoctionQueueRunner
import net.sourceforge.kolmafia.session.StoreManager

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

    // helper: resolve item name â†’ game ID (Int), null if unknown
    fun resolveItemId(itemName: String): Int? =
        gameDatabase?.item(itemName)?.id ?: ItemDatabase.getByName(itemName)?.id

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

    // 1. use(qty, it) / use(it, qty) / use(it)
    regFn(scope, "use", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = useItemRequest ?: return@regFn AshValue.of(false)
        val result = kotlinx.coroutines.runBlocking { req.use(itemId, qty) }
        result.getOrNull()?.let { applyItemUseResponse(itemId, it) }
        AshValue.of(result.isSuccess)
    }
    regFn(scope, "use", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "qty" to AshType.INT)) { _, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[1].toLong().toInt()
        val req = useItemRequest ?: return@regFn AshValue.of(false)
        val result = kotlinx.coroutines.runBlocking { req.use(itemId, qty) }
        result.getOrNull()?.let { applyItemUseResponse(itemId, it) }
        AshValue.of(result.isSuccess)
    }
    regFn(scope, "use", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        val req = useItemRequest ?: return@regFn AshValue.of(false)
        val result = kotlinx.coroutines.runBlocking { req.use(itemId, 1) }
        result.getOrNull()?.let { applyItemUseResponse(itemId, it) }
        AshValue.of(result.isSuccess)
    }

    // 2. eat overloads
    regFn(scope, "eat", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = eatFoodRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.eat(itemId, qty) }.isSuccess)
    }
    regFn(scope, "eat", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "qty" to AshType.INT)) { _, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[1].toLong().toInt()
        val req = eatFoodRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.eat(itemId, qty) }.isSuccess)
    }
    regFn(scope, "eat", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        val req = eatFoodRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.eat(itemId, 1) }.isSuccess)
    }

    // 3. drink overloads
    regFn(scope, "drink", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = drinkBoozeRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.drink(itemId, qty) }.isSuccess)
    }
    regFn(scope, "drink", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "qty" to AshType.INT)) { _, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[1].toLong().toInt()
        val req = drinkBoozeRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.drink(itemId, qty) }.isSuccess)
    }
    regFn(scope, "drink", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        val req = drinkBoozeRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.drink(itemId, 1) }.isSuccess)
    }

    // 4. chew overloads
    regFn(scope, "chew", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = chewRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.chew(itemId, qty) }.isSuccess)
    }
    regFn(scope, "chew", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "qty" to AshType.INT)) { _, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[1].toLong().toInt()
        val req = chewRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.chew(itemId, qty) }.isSuccess)
    }
    regFn(scope, "chew", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        val req = chewRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.chew(itemId, 1) }.isSuccess)
    }

    /**
     * Desktop autosell/sell â€” batch coalesce under `sell` with pilcrow params so
     * batch_close flushes a multi-item comma list (`sell 1 Â¶a, 2 Â¶b`).
     */
    fun autosellOrBatch(rt: AshRuntimeContext, itemId: Int, qty: Int): AshValue {
        if (qty <= 0) return AshValue.TRUE
        if (isBatching(rt)) {
            batchCommand(rt, "sell", null, pilcrowItemParams(qty, itemId))
            return AshValue.TRUE
        }
        val req = autosellRequest ?: return AshValue.of(false)
        return AshValue.of(kotlinx.coroutines.runBlocking { req.autosell(itemId, qty) }.isSuccess)
    }

    fun putClosetItem(rt: AshRuntimeContext, itemId: Int, qty: Int): AshValue {
        if (qty <= 0) return AshValue.TRUE
        if (isBatching(rt)) {
            batchCommand(rt, "closet", "put", pilcrowItemParams(qty, itemId))
            // Optimistic cache write-back so mid-batch *_amount reads match desktop list mutations after flush intent.
            preferences?.let { CollectionCacheSync.adjustCloset(it, itemId, qty) }
            return AshValue.TRUE
        }
        val req = closetRequest ?: return AshValue.of(false)
        val ok = kotlinx.coroutines.runBlocking { req.putIn(itemId, qty) }.isSuccess
        if (ok) preferences?.let { CollectionCacheSync.adjustCloset(it, itemId, qty) }
        return AshValue.of(ok)
    }

    fun takeClosetItem(rt: AshRuntimeContext, itemId: Int, qty: Int): AshValue {
        if (qty <= 0) return AshValue.TRUE
        if (isBatching(rt)) {
            batchCommand(rt, "closet", "take", pilcrowItemParams(qty, itemId))
            preferences?.let { CollectionCacheSync.adjustCloset(it, itemId, -qty) }
            return AshValue.TRUE
        }
        val req = closetRequest ?: return AshValue.of(false)
        val ok = kotlinx.coroutines.runBlocking { req.takeOut(itemId, qty) }.isSuccess
        if (ok) preferences?.let { CollectionCacheSync.adjustCloset(it, itemId, -qty) }
        return AshValue.of(ok)
    }

    fun putShop(rt: AshRuntimeContext, itemId: Int, price: Int, limit: Int, qty: Int, usingStorage: Boolean = false): AshValue {
        if (qty <= 0) return AshValue.TRUE
        if (isBatching(rt)) {
            val prefix = if (usingStorage) "put using storage" else "put"
            batchCommand(rt, "shop", prefix, "${pilcrowItemParams(qty, itemId)} @ $price limit $limit")
            return AshValue.TRUE
        }
        val req = manageStoreRequest ?: return AshValue.of(false)
        return AshValue.of(kotlinx.coroutines.runBlocking {
            // Couple put_shop to the same ManageStore sold-item seed as get_shop / refresh_shop.
            ensureSoldItemsRetrieved()
            val ok = req.addItem(itemId, price, limit, quantity = qty, fromStorage = usingStorage).isSuccess
            if (ok) {
                // Ensure local store cache reflects the put even if HTML STOCKED parse missed.
                if (StoreManager.shopAmount(itemId) <= 0) {
                    StoreManager.addItem(itemId, qty, price.toLong(), limit)
                }
                StoreManager.markSoldItemsRetrieved()
            }
            ok
        })
    }

    fun takeStorageItem(rt: AshRuntimeContext, itemId: Int, qty: Int): AshValue {
        if (qty <= 0) return AshValue.TRUE
        if (isBatching(rt)) {
            batchCommand(rt, "hagnk", null, pilcrowItemParams(qty, itemId))
            return AshValue.TRUE
        }
        val req = storageRequest ?: return AshValue.of(false)
        val ok = kotlinx.coroutines.runBlocking { req.withdraw(itemId, qty) }.isSuccess
        if (ok) preferences?.let { CollectionCacheSync.adjustStorage(it, itemId, -qty) }
        return AshValue.of(ok)
    }

    // 5. autosell / sell overloads
    regFn(scope, "autosell", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { rt, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        autosellOrBatch(rt, itemId, args[0].toLong().toInt())
    }
    regFn(scope, "autosell", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "qty" to AshType.INT)) { rt, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        autosellOrBatch(rt, itemId, args[1].toLong().toInt())
    }
    regFn(scope, "autosell", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { rt, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        autosellOrBatch(rt, itemId, 1)
    }

    regFn(scope, "sell", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { rt, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        autosellOrBatch(rt, itemId, args[0].toLong().toInt())
    }
    regFn(scope, "sell", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "qty" to AshType.INT)) { rt, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        autosellOrBatch(rt, itemId, args[1].toLong().toInt())
    }

    // 6. put_closet â€” meat (int) / item / qty+item
    regFn(scope, "put_closet", AshType.BOOLEAN, listOf("meat" to AshType.INT)) { rt, args ->
        val meat = args[0].toLong()
        if (meat <= 0) return@regFn AshValue.TRUE
        if (isBatching(rt)) {
            batchCommand(rt, "closet", "put", "$meat meat")
            return@regFn AshValue.TRUE
        }
        val req = closetRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.putMeat(meat) }.isSuccess)
    }
    regFn(scope, "put_closet", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { rt, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        putClosetItem(rt, itemId, args[0].toLong().toInt())
    }
    regFn(scope, "put_closet", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "qty" to AshType.INT)) { rt, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        putClosetItem(rt, itemId, args[1].toLong().toInt())
    }
    regFn(scope, "put_closet", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { rt, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        putClosetItem(rt, itemId, 1)
    }

    // 7. take_closet â€” meat / item / qty+item
    regFn(scope, "take_closet", AshType.BOOLEAN, listOf("meat" to AshType.INT)) { rt, args ->
        val meat = args[0].toLong()
        if (meat <= 0) return@regFn AshValue.TRUE
        if (isBatching(rt)) {
            batchCommand(rt, "closet", "take", "$meat meat")
            return@regFn AshValue.TRUE
        }
        val req = closetRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.takeMeat(meat) }.isSuccess)
    }
    regFn(scope, "take_closet", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { rt, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        takeClosetItem(rt, itemId, args[0].toLong().toInt())
    }
    regFn(scope, "take_closet", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "qty" to AshType.INT)) { rt, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        takeClosetItem(rt, itemId, args[1].toLong().toInt())
    }
    regFn(scope, "take_closet", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { rt, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        takeClosetItem(rt, itemId, 1)
    }

    // 8. put_shop â€” 3-arg uses full inventory count; 4-arg explicit qty
    regFn(scope, "put_shop", AshType.BOOLEAN,
        listOf("price" to AshType.INT, "limit" to AshType.INT, "it" to AshType.ITEM)) { rt, args ->
        val itemId = resolveItemId(args[2].toString()) ?: return@regFn AshValue.of(false)
        val price = args[0].toLong().toInt()
        val limit = args[1].toLong().toInt()
        val qty = inventoryManager?.getCount(itemId) ?: 0
        putShop(rt, itemId, price, limit, qty)
    }
    regFn(scope, "put_shop", AshType.BOOLEAN,
        listOf("price" to AshType.INT, "limit" to AshType.INT, "qty" to AshType.INT, "it" to AshType.ITEM)) { rt, args ->
        val itemId = resolveItemId(args[3].toString()) ?: return@regFn AshValue.of(false)
        putShop(rt, itemId, args[0].toLong().toInt(), args[1].toLong().toInt(), args[2].toLong().toInt())
    }

    // 9. take_storage(qty, it) / (it, qty) / (it)
    regFn(scope, "take_storage", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { rt, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        takeStorageItem(rt, itemId, args[0].toLong().toInt())
    }
    regFn(scope, "take_storage", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "qty" to AshType.INT)) { rt, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        takeStorageItem(rt, itemId, args[1].toLong().toInt())
    }
    regFn(scope, "take_storage", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { rt, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        takeStorageItem(rt, itemId, 1)
    }

    // 10. eatsilent(qty, it) â€” Same as eat()
    regFn(scope, "eatsilent", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = eatFoodRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.eat(itemId, qty) }.isSuccess)
    }
    regFn(scope, "eatsilent", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        val req = eatFoodRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.eat(itemId, 1) }.isSuccess)
    }

    // 11. drinksilent
    regFn(scope, "drinksilent", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = drinkBoozeRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.drink(itemId, qty) }.isSuccess)
    }
    regFn(scope, "drinksilent", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        val req = drinkBoozeRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.drink(itemId, 1) }.isSuccess)
    }

    // 12. overdrink
    regFn(scope, "overdrink", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        val qty = args[0].toLong().toInt()
        val req = drinkBoozeRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.drink(itemId, qty) }.isSuccess)
    }
    regFn(scope, "overdrink", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { _, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        val req = drinkBoozeRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.drink(itemId, 1) }.isSuccess)
    }

    // 13â€“16. display/stash qty-first + item-first + 1-arg (+ batch cache edges)
    fun putDisplay(rt: AshRuntimeContext, itemId: Int, qty: Int): AshValue {
        if (qty <= 0) return AshValue.TRUE
        if (character?.state?.value?.hasDisplayCase == false) return AshValue.FALSE
        if (isBatching(rt)) {
            batchCommand(rt, "display", "put", pilcrowItemParams(qty, itemId))
            preferences?.let { CollectionCacheSync.adjustDisplay(it, itemId, qty) }
            return AshValue.TRUE
        }
        val req = displayCaseRequest ?: return AshValue.of(false)
        val ok = kotlinx.coroutines.runBlocking { req.putIn(itemId, qty) }.isSuccess
        if (ok) preferences?.let { CollectionCacheSync.adjustDisplay(it, itemId, qty) }
        return AshValue.of(ok)
    }
    fun takeDisplay(rt: AshRuntimeContext, itemId: Int, qty: Int): AshValue {
        if (qty <= 0) return AshValue.TRUE
        if (character?.state?.value?.hasDisplayCase == false) return AshValue.FALSE
        if (isBatching(rt)) {
            batchCommand(rt, "display", "take", pilcrowItemParams(qty, itemId))
            preferences?.let { CollectionCacheSync.adjustDisplay(it, itemId, -qty) }
            return AshValue.TRUE
        }
        val req = displayCaseRequest ?: return AshValue.of(false)
        val ok = kotlinx.coroutines.runBlocking { req.takeOut(itemId, qty) }.isSuccess
        if (ok) preferences?.let { CollectionCacheSync.adjustDisplay(it, itemId, -qty) }
        return AshValue.of(ok)
    }
    fun putStash(rt: AshRuntimeContext, itemId: Int, qty: Int): AshValue {
        if (qty <= 0) return AshValue.TRUE
        if (isBatching(rt)) {
            batchCommand(rt, "stash", "put", pilcrowItemParams(qty, itemId))
            preferences?.let { CollectionCacheSync.adjustStash(it, itemId, qty) }
            return AshValue.TRUE
        }
        val req = clanStashRequest ?: return AshValue.of(false)
        val ok = kotlinx.coroutines.runBlocking { req.putIn(itemId, qty) }.isSuccess
        if (ok) preferences?.let { CollectionCacheSync.adjustStash(it, itemId, qty) }
        return AshValue.of(ok)
    }
    fun takeStash(rt: AshRuntimeContext, itemId: Int, qty: Int): AshValue {
        if (qty <= 0) return AshValue.TRUE
        if (isBatching(rt)) {
            batchCommand(rt, "stash", "take", pilcrowItemParams(qty, itemId))
            preferences?.let { CollectionCacheSync.adjustStash(it, itemId, -qty) }
            return AshValue.TRUE
        }
        val req = clanStashRequest ?: return AshValue.of(false)
        val ok = kotlinx.coroutines.runBlocking { req.takeOut(itemId, qty) }.isSuccess
        if (ok) preferences?.let { CollectionCacheSync.adjustStash(it, itemId, -qty) }
        return AshValue.of(ok)
    }

    regFn(scope, "put_display", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { rt, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        putDisplay(rt, itemId, args[0].toLong().toInt())
    }
    regFn(scope, "put_display", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "qty" to AshType.INT)) { rt, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        putDisplay(rt, itemId, args[1].toLong().toInt())
    }
    regFn(scope, "put_display", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { rt, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        putDisplay(rt, itemId, 1)
    }

    regFn(scope, "take_display", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { rt, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        takeDisplay(rt, itemId, args[0].toLong().toInt())
    }
    regFn(scope, "take_display", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "qty" to AshType.INT)) { rt, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        takeDisplay(rt, itemId, args[1].toLong().toInt())
    }
    regFn(scope, "take_display", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { rt, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        takeDisplay(rt, itemId, 1)
    }

    regFn(scope, "put_stash", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { rt, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        putStash(rt, itemId, args[0].toLong().toInt())
    }
    regFn(scope, "put_stash", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "qty" to AshType.INT)) { rt, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        putStash(rt, itemId, args[1].toLong().toInt())
    }
    regFn(scope, "put_stash", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { rt, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        putStash(rt, itemId, 1)
    }

    regFn(scope, "take_stash", AshType.BOOLEAN,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { rt, args ->
        val itemId = resolveItemId(args[1].toString()) ?: return@regFn AshValue.of(false)
        takeStash(rt, itemId, args[0].toLong().toInt())
    }
    regFn(scope, "take_stash", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "qty" to AshType.INT)) { rt, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        takeStash(rt, itemId, args[1].toLong().toInt())
    }
    regFn(scope, "take_stash", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { rt, args ->
        val itemId = resolveItemId(args[0].toString()) ?: return@regFn AshValue.of(false)
        takeStash(rt, itemId, 1)
    }

    // 17. empty_closet() â†’ boolean â€” take all items from closet
    regFn(scope, "empty_closet", AshType.BOOLEAN, emptyList()) { rt, _ ->
        if (isBatching(rt)) {
            batchCommand(rt, "closet", null, "empty")
            return@regFn AshValue.TRUE
        }
        val req = closetRequest ?: return@regFn AshValue.of(false)
        AshValue.of(kotlinx.coroutines.runBlocking { req.emptyCloset().isSuccess })
    }
}
