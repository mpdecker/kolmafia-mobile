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

    // ── get_closet() → int[item] (stub — closet not yet fetched on mobile) ────
    regFn(scope, "get_closet", itemIntType, emptyList()) { _, _ ->
        AggregateValue(itemIntType)
    }

    // ── get_storage() → int[item] (stub — Hagnk's storage not yet fetched) ───
    regFn(scope, "get_storage", itemIntType, emptyList()) { _, _ ->
        AggregateValue(itemIntType)
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
