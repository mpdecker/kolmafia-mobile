package net.sourceforge.kolmafia.ash

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
            val itemName = gameDatabase?.item(itemId)?.name ?: "Item #$itemId"
            result[AshValue.item(itemName)] = AshValue.of(qty.toLong())
        }
        return result
    }

    fun cachedAggregate(prefKey: String): AggregateValue {
        val prefs = preferences ?: return AggregateValue(itemIntType)
        return mapToAggregate(CollectionCache.load(prefs, prefKey))
    }

    fun cachedItemAmount(prefKey: String, itemName: String): Long {
        val prefs = preferences ?: return 0L
        val itemId = gameDatabase?.item(itemName)?.id
            ?: inventoryManager?.state?.value?.items?.values
                ?.find { it.name.equals(itemName, ignoreCase = true) }?.itemId
            ?: return 0L
        return CollectionCache.load(prefs, prefKey)[itemId]?.toLong() ?: 0L
    }

    // ── get_closet() → int[item] (live — fetches from api.php?what=closet) ───
    regFn(scope, "get_closet", itemIntType, emptyList()) { _, _ ->
        val contents = kotlinx.coroutines.runBlocking {
            closetRequest?.fetchContents() ?: emptyMap()
        }
        preferences?.let { CollectionCache.save(it, Preferences.CACHED_CLOSET, contents) }
        mapToAggregate(contents)
    }

    regFn(scope, "get_cached_closet", itemIntType, emptyList()) { _, _ ->
        cachedAggregate(Preferences.CACHED_CLOSET)
    }

    regFn(scope, "closet_amount", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
        AshValue.of(cachedItemAmount(Preferences.CACHED_CLOSET, args[0].toString()))
    }

    // ── get_storage() → int[item] (live — fetches from api.php?what=storage) ─
    regFn(scope, "get_storage", itemIntType, emptyList()) { _, _ ->
        val contents = kotlinx.coroutines.runBlocking {
            storageRequest?.fetchContents() ?: emptyMap()
        }
        preferences?.let { CollectionCache.save(it, Preferences.CACHED_STORAGE, contents) }
        mapToAggregate(contents)
    }

    regFn(scope, "get_cached_storage", itemIntType, emptyList()) { _, _ ->
        cachedAggregate(Preferences.CACHED_STORAGE)
    }

    regFn(scope, "storage_amount", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
        AshValue.of(cachedItemAmount(Preferences.CACHED_STORAGE, args[0].toString()))
    }

    // ── get_stash() → int[item] (live — fetches from clan_stash.php) ─────────
    regFn(scope, "get_stash", itemIntType, emptyList()) { _, _ ->
        val contents = kotlinx.coroutines.runBlocking {
            clanStashRequest?.fetchContents() ?: emptyMap()
        }
        preferences?.let { CollectionCache.save(it, Preferences.CACHED_STASH, contents) }
        mapToAggregate(contents)
    }

    regFn(scope, "get_cached_stash", itemIntType, emptyList()) { _, _ ->
        cachedAggregate(Preferences.CACHED_STASH)
    }

    regFn(scope, "stash_amount", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
        AshValue.of(cachedItemAmount(Preferences.CACHED_STASH, args[0].toString()))
    }

    // ── get_display() → int[item] (live — fetches from displaycollection.php) ─
    regFn(scope, "get_display", itemIntType, emptyList()) { _, _ ->
        val contents = kotlinx.coroutines.runBlocking {
            displayCaseRequest?.fetchContents() ?: emptyMap()
        }
        preferences?.let { CollectionCache.save(it, Preferences.CACHED_DISPLAY, contents) }
        mapToAggregate(contents)
    }

    regFn(scope, "get_cached_display", itemIntType, emptyList()) { _, _ ->
        cachedAggregate(Preferences.CACHED_DISPLAY)
    }

    regFn(scope, "display_amount", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
        AshValue.of(cachedItemAmount(Preferences.CACHED_DISPLAY, args[0].toString()))
    }
}
