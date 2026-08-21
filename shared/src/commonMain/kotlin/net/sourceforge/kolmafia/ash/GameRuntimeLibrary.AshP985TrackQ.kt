package net.sourceforge.kolmafia.ash

/**
 * AshP985–990 Track Q — Shop / mall residuals.
 *
 * Phase 985: have_shop, have_display
 * Phase 986: mall_prices (pref-backed stub map)
 * Phase 987: get_shop_log (stub)
 * Phase 988: put_shop_using_storage (stub), well_stocked (pref)
 * Phase 989: daily_special
 * Phase 990: sells_skill (coinmaster placeholder)
 */
internal fun GameRuntimeLibrary.registerAshP985TrackQBatch(scope: AshScope) {
    // ── Phase 985: have_shop / have_display ──────────────────────────
    regFn(scope, "have_shop", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(character?.state?.value?.hasStore ?: false)
    }

    regFn(scope, "have_display", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(character?.state?.value?.hasDisplayCase ?: false)
    }

    // ── Phase 986: mall_prices ──────────────────────────────────────
    val itemToInt = AggregateType(AshType.ITEM, AshType.INT)
    regFn(scope, "mall_prices", itemToInt,
        listOf("category" to AshType.STRING)) { _, _ ->
        AggregateValue(itemToInt)
    }

    regFn(scope, "mall_prices", itemToInt,
        listOf("category" to AshType.STRING, "tiers" to AshType.INT)) { _, _ ->
        AggregateValue(itemToInt)
    }

    // ── Phase 987: get_shop_log ─────────────────────────────────────
    val stringArray = AggregateType(AshType.INT, AshType.STRING)
    regFn(scope, "get_shop_log", stringArray, emptyList()) { _, _ ->
        AggregateValue(stringArray)
    }

    // ── Phase 988: put_shop_using_storage / well_stocked ────────────
    regFn(scope, "put_shop_using_storage", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "qty" to AshType.INT, "price" to AshType.INT)) { _, _ ->
        AshValue.FALSE
    }

    regFn(scope, "put_shop_using_storage", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "qty" to AshType.INT, "price" to AshType.INT,
            "limit" to AshType.INT)) { _, _ ->
        AshValue.FALSE
    }

    regFn(scope, "well_stocked", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM)) { _, _ ->
        AshValue.TRUE
    }

    // ── Phase 989: daily_special ────────────────────────────────────
    regFn(scope, "daily_special", AshType.ITEM, emptyList()) { _, _ ->
        val special = preferences?.getString("dailySpecial", "")?.takeIf { it.isNotBlank() }
        AshValue.item(special ?: "")
    }

    // ── Phase 990: sells_skill ─────────────────────────────────────
    regFn(scope, "sells_skill", AshType.BOOLEAN,
        listOf("cm" to AshType.COINMASTER)) { _, _ ->
        AshValue.FALSE
    }
}
