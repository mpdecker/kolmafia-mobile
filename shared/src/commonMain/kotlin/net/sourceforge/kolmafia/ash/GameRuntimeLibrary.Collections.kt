package net.sourceforge.kolmafia.ash

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

    // ── get_closet() → int[item] (live — fetches from api.php?what=closet) ───
    regFn(scope, "get_closet", itemIntType, emptyList()) { _, _ ->
        val contents = kotlinx.coroutines.runBlocking {
            closetRequest?.fetchContents() ?: emptyMap()
        }
        mapToAggregate(contents)
    }

    // ── get_storage() → int[item] (live — fetches from api.php?what=storage) ─
    regFn(scope, "get_storage", itemIntType, emptyList()) { _, _ ->
        val contents = kotlinx.coroutines.runBlocking {
            storageRequest?.fetchContents() ?: emptyMap()
        }
        mapToAggregate(contents)
    }

    // ── get_stash() → int[item] (stub — clan stash not yet fetched) ──────────
    regFn(scope, "get_stash", itemIntType, emptyList()) { _, _ ->
        AggregateValue(itemIntType)
    }

    // ── get_display() → int[item] (stub — display case not yet fetched) ──────
    regFn(scope, "get_display", itemIntType, emptyList()) { _, _ ->
        AggregateValue(itemIntType)
    }
}
