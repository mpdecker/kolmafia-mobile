package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.clan.ClanManager
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.CollectionCacheSync
import net.sourceforge.kolmafia.preferences.Preferences

internal fun GameRuntimeLibrary.registerCollectionQueries(scope: AshScope) {

    // int[item] — maps item names to quantities
    val itemIntType = AggregateType(AshType.ITEM, AshType.INT)

    // ── get_inventory() → int[item] ───────────────────────────────────────────
    regFn(scope, "get_inventory", itemIntType, emptyList()) { _, _ ->
        val result = AggregateValue(itemIntType)
        inventoryManager?.state?.value?.items?.values?.forEach { item ->
            result[AshValue.item(item.name)] = AshValue.of(item.quantity.toLong())
        }
        result
    }

    // ── Helper: convert fetchContents() Map<Int, Int> → AggregateValue ───────
    fun mapToAggregate(contents: Map<Int, Int>): AggregateValue {
        val result = AggregateValue(itemIntType)
        contents.forEach { (itemId, qty) ->
            val itemName = gameDatabase?.item(itemId)?.name
                ?: ItemDatabase.getById(itemId)?.name
                ?: "Item #$itemId"
            result[AshValue.item(itemName)] = AshValue.of(qty.toLong())
        }
        return result
    }

    fun cachedAggregate(prefKey: String): AggregateValue {
        val prefs = preferences ?: return AggregateValue(itemIntType)
        return mapToAggregate(CollectionCache.load(prefs, prefKey))
    }

    fun resolveItemId(itemName: String): Int? {
        return itemName.toIntOrNull()?.takeIf { it > 0 }
            ?: gameDatabase?.item(itemName)?.id
            ?: inventoryManager?.state?.value?.items?.values
                ?.find { it.name.equals(itemName, ignoreCase = true) }?.itemId
    }

    fun cachedItemAmount(prefKey: String, itemName: String): Long {
        val prefs = preferences ?: return 0L
        val itemId = resolveItemId(itemName) ?: return 0L
        return CollectionCache.load(prefs, prefKey)[itemId]?.toLong() ?: 0L
    }

    fun cachedItemAmountById(prefKey: String, itemId: Int): Long {
        if (itemId <= 0) return 0L
        val prefs = preferences ?: return 0L
        return CollectionCache.load(prefs, prefKey)[itemId]?.toLong() ?: 0L
    }

    /** Desktop-shaped lazy refresh when collection was never retrieved. */
    fun ensureClosetRetrieved() {
        if (CollectionCacheSync.closetRetrieved) return
        val prefs = preferences ?: return
        val req = closetRequest ?: return
        runBlocking { CollectionCacheSync.refreshCloset(req, prefs) }
    }

    fun ensureStorageRetrieved() {
        if (CollectionCacheSync.storageRetrieved) return
        val prefs = preferences ?: return
        val req = storageRequest ?: return
        runBlocking { CollectionCacheSync.refreshStorage(req, character?.state?.value, prefs) }
    }

    fun ensureStashRetrieved() {
        if (ClanManager.stashRetrieved) return
        val prefs = preferences ?: return
        val req = clanStashRequest ?: return
        runBlocking { CollectionCacheSync.refreshStash(req, prefs) }
    }

    fun ensureDisplayRetrieved() {
        if (CollectionCacheSync.collectionRetrieved) return
        val prefs = preferences ?: return
        val req = displayCaseRequest ?: return
        runBlocking { CollectionCacheSync.refreshDisplay(req, prefs) }
    }

    // ── get_closet() → int[item] (live — fetches from api.php?what=closet) ───
    regFn(scope, "get_closet", itemIntType, emptyList()) { _, _ ->
        val contents = runBlocking {
            closetRequest?.fetchContents() ?: emptyMap()
        }
        preferences?.let { CollectionCacheSync.saveCloset(it, contents) }
        mapToAggregate(contents)
    }

    regFn(scope, "get_cached_closet", itemIntType, emptyList()) { _, _ ->
        cachedAggregate(Preferences.CACHED_CLOSET)
    }

    regFn(scope, "closet_amount", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
        ensureClosetRetrieved()
        AshValue.of(cachedItemAmount(Preferences.CACHED_CLOSET, args[0].toString()))
    }
    regFn(scope, "closet_amount", AshType.INT, listOf("it" to AshType.INT)) { _, args ->
        val itemId = args[0].toLong().toInt()
        if (itemId <= 0) return@regFn AshValue.of(0L)
        ensureClosetRetrieved()
        AshValue.of(cachedItemAmountById(Preferences.CACHED_CLOSET, itemId))
    }

    // ── get_storage() → int[item] (live — fetches from api.php?what=storage) ─
    regFn(scope, "get_storage", itemIntType, emptyList()) { _, _ ->
        val classified = runBlocking {
            storageRequest?.fetchClassifiedContents(
                character?.state?.value,
                preferences,
            )
        }
        val contents = classified?.storage ?: emptyMap()
        preferences?.let { prefs ->
            CollectionCacheSync.saveStorage(
                prefs,
                contents,
                classified?.freepulls ?: emptyMap(),
                classified?.nopulls ?: emptyMap(),
            )
        }
        mapToAggregate(contents)
    }

    regFn(scope, "get_cached_storage", itemIntType, emptyList()) { _, _ ->
        cachedAggregate(Preferences.CACHED_STORAGE)
    }

    regFn(scope, "storage_amount", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
        ensureStorageRetrieved()
        val itemName = args[0].toString()
        val storage = cachedItemAmount(Preferences.CACHED_STORAGE, itemName)
        val freepull = cachedItemAmount(Preferences.CACHED_FREEPULLS, itemName)
        AshValue.of(storage + freepull)
    }
    regFn(scope, "storage_amount", AshType.INT, listOf("it" to AshType.INT)) { _, args ->
        val itemId = args[0].toLong().toInt()
        if (itemId <= 0) return@regFn AshValue.of(0L)
        ensureStorageRetrieved()
        val storage = cachedItemAmountById(Preferences.CACHED_STORAGE, itemId)
        val freepull = cachedItemAmountById(Preferences.CACHED_FREEPULLS, itemId)
        AshValue.of(storage + freepull)
    }

    // ── get_free_pulls() → int[item] (live — non-storage bucket from storage.php) ─
    regFn(scope, "get_free_pulls", itemIntType, emptyList()) { _, _ ->
        // Lazy-refresh when never retrieved (same contract as get_no_pulls).
        if (!CollectionCacheSync.storageRetrieved) {
            val prefs = preferences
            val req = storageRequest
            if (prefs != null && req != null) {
                runBlocking {
                    CollectionCacheSync.refreshStorage(
                        req, character?.state?.value, prefs,
                    )
                }
            }
        }
        val classified = runBlocking {
            storageRequest?.fetchClassifiedContents(
                character?.state?.value,
                preferences,
            )
        }
        val contents = classified?.freepulls
            ?: preferences?.let { CollectionCache.load(it, Preferences.CACHED_FREEPULLS) }
            ?: emptyMap()
        preferences?.let { prefs ->
            if (classified != null) {
                CollectionCacheSync.saveStorage(
                    prefs,
                    classified.storage,
                    contents,
                    classified.nopulls,
                )
            }
        }
        mapToAggregate(contents)
    }

    regFn(scope, "get_cached_free_pulls", itemIntType, emptyList()) { _, _ ->
        cachedAggregate(Preferences.CACHED_FREEPULLS)
    }

    // ── get_stash() → int[item] (live — fetches from clan_stash.php) ─────────
    regFn(scope, "get_stash", itemIntType, emptyList()) { _, _ ->
        val contents = runBlocking {
            clanStashRequest?.fetchContents() ?: emptyMap()
        }
        preferences?.let { CollectionCacheSync.saveStash(it, contents) }
        mapToAggregate(contents)
    }

    regFn(scope, "get_cached_stash", itemIntType, emptyList()) { _, _ ->
        cachedAggregate(Preferences.CACHED_STASH)
    }

    regFn(scope, "stash_amount", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
        ensureStashRetrieved()
        AshValue.of(cachedItemAmount(Preferences.CACHED_STASH, args[0].toString()))
    }
    regFn(scope, "stash_amount", AshType.INT, listOf("it" to AshType.INT)) { _, args ->
        ensureStashRetrieved()
        AshValue.of(cachedItemAmountById(Preferences.CACHED_STASH, args[0].toLong().toInt()))
    }

    // ── get_display() → int[item] (live — fetches from displaycollection.php) ─
    regFn(scope, "get_display", itemIntType, emptyList()) { _, _ ->
        val contents = runBlocking {
            displayCaseRequest?.fetchContents() ?: emptyMap()
        }
        preferences?.let { CollectionCacheSync.saveDisplay(it, contents) }
        mapToAggregate(contents)
    }

    regFn(scope, "get_cached_display", itemIntType, emptyList()) { _, _ ->
        cachedAggregate(Preferences.CACHED_DISPLAY)
    }

    regFn(scope, "display_amount", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
        // Desktop display_amount — no display case → 0; else refresh if never retrieved
        if (character?.state?.value?.hasDisplayCase == false) return@regFn AshValue.of(0L)
        ensureDisplayRetrieved()
        AshValue.of(cachedItemAmount(Preferences.CACHED_DISPLAY, args[0].toString()))
    }
    regFn(scope, "display_amount", AshType.INT, listOf("it" to AshType.INT)) { _, args ->
        if (character?.state?.value?.hasDisplayCase == false) return@regFn AshValue.of(0L)
        ensureDisplayRetrieved()
        AshValue.of(cachedItemAmountById(Preferences.CACHED_DISPLAY, args[0].toLong().toInt()))
    }
}
