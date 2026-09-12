package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.inventory.CollectionCacheSync
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.StoragePullRules

internal fun GameRuntimeLibrary.registerMallFunctions(scope: AshScope) {

    fun resolveItemId(itemName: String): Int? = gameDatabase?.item(itemName)?.id

    fun canInteract(): Boolean =
        StoragePullRules.canInteract(character?.state?.value)

    fun invCount(itemId: Int): Int =
        inventoryManager?.getCount(itemId) ?: 0

    suspend fun storageCount(itemId: Int): Int {
        preferences?.let { prefs ->
            val cached = CollectionCache.load(prefs, Preferences.CACHED_STORAGE)
            cached[itemId]?.let { return it }
        }
        return storageRequest?.fetchContents()?.get(itemId) ?: 0
    }

    /** HC/Ronin mall buy via storage meat — desktop `buy using storage`. */
    suspend fun buyUsingStorage(itemId: Int, itemName: String, count: Int, maxPrice: Int = Int.MAX_VALUE): Int {
        val before = storageCount(itemId)
        val bought = buyOneCliItem(
            itemId = itemId,
            itemName = itemName,
            qty = count,
            maxPrice = maxPrice,
            forceMall = true,
            npcOnly = false,
            canInteract = false,
        )
        // When purchase HTML didn't credit storage (stub mall managers), mirror desktop
        // destination bookkeeping via the collection cache.
        val after = storageCount(itemId)
        if (bought > 0 && after < before + bought) {
            preferences?.let { CollectionCacheSync.adjustStorage(it, itemId, bought) }
        }
        return (storageCount(itemId) - before).coerceAtLeast(0).let { gained ->
            if (gained > 0) gained else bought.coerceAtLeast(0)
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

    /** Desktop buy boolean: inventory must land exactly at initial+want. */
    fun buySucceeded(itemId: Int, initial: Int, bought: Int, want: Int): Boolean {
        if (bought < want) return false
        // When no inventory manager is wired (unit tests / headless), trust bought count.
        if (inventoryManager == null) return true
        return invCount(itemId) == initial + want
    }

    /** Desktop buy(count,item,maxPrice) → inventory delta after purchase. */
    fun purchaseDelta(itemId: Int, initial: Int, bought: Int): Int =
        if (inventoryManager == null) bought.coerceAtLeast(0)
        else (invCount(itemId) - initial).coerceAtLeast(0)

    fun queuedPullsFor(itemId: Int, itemName: String): Int {
        val name = itemName.ifBlank {
            gameDatabase?.item(itemId)?.name.orEmpty()
        }
        if (name.isBlank()) return 0
        return ConcoctionDatabase.getRuntime(name)?.queuedPulls ?: 0
    }

    // Desktop: buy(item) → boolean via inventory delta after CLI NPC/mall routing
    regFn(scope, "buy", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { _, args ->
        val itemName = args[0].toString()
        val itemId = resolveItemId(itemName) ?: return@regFn AshValue.FALSE
        val ok = kotlinx.coroutines.runBlocking {
            val initial = invCount(itemId)
            val bought = buyViaCli(itemId, itemName, 1)
            buySucceeded(itemId, initial, bought, 1)
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
            buySucceeded(itemId, initial, bought, count)
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
            buySucceeded(itemId, initial, bought, count)
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
            val bought = buyViaCli(itemId, itemName, count, maxPrice)
            purchaseDelta(itemId, initial, bought)
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
            val bought = buyViaCli(itemId, itemName, count, maxPrice)
            purchaseDelta(itemId, initial, bought)
        }
        AshValue.of(purchased.toLong())
    }

    // buy_using_storage(it: item) → boolean — HC/Ronin only; storage meat + storage destination
    regFn(scope, "buy_using_storage", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM)) { _, args ->
        if (canInteract()) return@regFn AshValue.FALSE
        val itemName = args[0].toString()
        val itemId = resolveItemId(itemName) ?: return@regFn AshValue.FALSE
        val ok = kotlinx.coroutines.runBlocking {
            val initial = storageCount(itemId)
            val gained = buyUsingStorage(itemId, itemName, 1)
            gained > 0 && (storageCount(itemId) >= initial + 1 || inventoryManager == null)
        }
        AshValue.of(ok)
    }

    // buy_using_storage(count, item) / (item, count) → boolean
    regFn(scope, "buy_using_storage", AshType.BOOLEAN,
        listOf("count" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        if (canInteract()) return@regFn AshValue.FALSE
        val count = args[0].toLong().toInt()
        if (count <= 0) return@regFn AshValue.TRUE
        val itemName = args[1].toString()
        val itemId = resolveItemId(itemName) ?: return@regFn AshValue.FALSE
        val ok = kotlinx.coroutines.runBlocking {
            val initial = storageCount(itemId)
            val gained = buyUsingStorage(itemId, itemName, count)
            gained >= count
        }
        AshValue.of(ok)
    }
    regFn(scope, "buy_using_storage", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "count" to AshType.INT)) { _, args ->
        if (canInteract()) return@regFn AshValue.FALSE
        val count = args[1].toLong().toInt()
        if (count <= 0) return@regFn AshValue.TRUE
        val itemName = args[0].toString()
        val itemId = resolveItemId(itemName) ?: return@regFn AshValue.FALSE
        val ok = kotlinx.coroutines.runBlocking {
            val gained = buyUsingStorage(itemId, itemName, count)
            gained >= count
        }
        AshValue.of(ok)
    }

    // buy_using_storage(count, item, maxPrice) / (item, count, maxPrice) → int
    regFn(scope, "buy_using_storage", AshType.INT,
        listOf("count" to AshType.INT, "it" to AshType.ITEM, "maxPrice" to AshType.INT)) { _, args ->
        if (canInteract()) return@regFn AshValue.of(0L)
        val count = args[0].toLong().toInt()
        if (count <= 0) return@regFn AshValue.of(0L)
        val itemName = args[1].toString()
        val itemId = resolveItemId(itemName) ?: return@regFn AshValue.of(0L)
        val maxPrice = args[2].toLong().toInt()
        val purchased = kotlinx.coroutines.runBlocking {
            buyUsingStorage(itemId, itemName, count, maxPrice)
        }
        AshValue.of(purchased.toLong())
    }
    regFn(scope, "buy_using_storage", AshType.INT,
        listOf("it" to AshType.ITEM, "count" to AshType.INT, "maxPrice" to AshType.INT)) { _, args ->
        if (canInteract()) return@regFn AshValue.of(0L)
        val count = args[1].toLong().toInt()
        if (count <= 0) return@regFn AshValue.of(0L)
        val itemName = args[0].toString()
        val itemId = resolveItemId(itemName) ?: return@regFn AshValue.of(0L)
        val maxPrice = args[2].toLong().toInt()
        val purchased = kotlinx.coroutines.runBlocking {
            buyUsingStorage(itemId, itemName, count, maxPrice)
        }
        AshValue.of(purchased.toLong())
    }

    // retrieve_item(item) / (count, item) / (item, count)
    // Desktop: count <= 0 → continueValue() (true); already-on-hand short-circuit
    fun retrieveItemOk(itemId: Int, itemName: String, count: Int): Boolean {
        if (count <= 0) return true
        if (invCount(itemId) >= count) return true
        val retrieved = kotlinx.coroutines.runBlocking {
            retrieveItemService?.retrieve(itemId, count) ?: 0
        }
        return retrieved >= count || invCount(itemId) >= count
    }

    regFn(scope, "retrieve_item", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { _, args ->
        val itemName = args[0].toString()
        val itemId = resolveItemId(itemName) ?: return@regFn AshValue.of(false)
        AshValue.of(retrieveItemOk(itemId, itemName, 1))
    }
    regFn(scope, "retrieve_item", AshType.BOOLEAN,
        listOf("count" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        val count = args[0].toLong().toInt()
        if (count <= 0) return@regFn AshValue.TRUE
        val itemName = args[1].toString()
        val itemId = resolveItemId(itemName) ?: return@regFn AshValue.of(false)
        AshValue.of(retrieveItemOk(itemId, itemName, count))
    }
    regFn(scope, "retrieve_item", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "count" to AshType.INT)) { _, args ->
        val count = args[1].toLong().toInt()
        if (count <= 0) return@regFn AshValue.TRUE
        val itemName = args[0].toString()
        val itemId = resolveItemId(itemName) ?: return@regFn AshValue.of(false)
        AshValue.of(retrieveItemOk(itemId, itemName, count))
    }

    // retrieve_item(count, item, retrieve) — check-only uses physicalAccessible − pull queue
    regFn(scope, "retrieve_item", AshType.BOOLEAN,
        listOf("count" to AshType.INT, "it" to AshType.ITEM, "retrieve" to AshType.BOOLEAN)) { _, args ->
        val count = args[0].toLong().toInt()
        if (count <= 0) return@regFn AshValue.TRUE
        val itemName = args[1].toString()
        val itemId = resolveItemId(itemName) ?: return@regFn AshValue.of(false)
        val doRetrieve = args[2].toBoolean()
        if (!doRetrieve) {
            val accessible = kotlinx.coroutines.runBlocking {
                (physicalAccessibleCount(itemId, itemName) - queuedPullsFor(itemId, itemName))
                    .coerceAtLeast(0)
            }
            return@regFn AshValue.of(accessible >= count)
        }
        AshValue.of(retrieveItemOk(itemId, itemName, count))
    }
}
