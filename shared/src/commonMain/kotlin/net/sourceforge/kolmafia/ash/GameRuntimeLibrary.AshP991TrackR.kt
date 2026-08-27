package net.sourceforge.kolmafia.ash

/**
 * AshP991–996 Track R — Combat / result residuals.
 *
 * Phase 991: extract_items, extract_meat
 * Phase 992: last_monster_message, last_encounter
 * Phase 993: attack, steal (0-arg combat), twiddle
 * Phase 994: curse (player,item,item)
 * Phase 995: pickpocket, runaway
 * Phase 996: expected_damage, monster_level_adjustment
 */
internal fun GameRuntimeLibrary.registerAshP991TrackRBatch(scope: AshScope) {
    // ── Phase 991: extract_items / extract_meat ─────────────────────
    val itemToInt = AggregateType(AshType.ITEM, AshType.INT)
    regFn(scope, "extract_items", itemToInt,
        listOf("html" to AshType.STRING)) { _, args ->
        val result = AggregateValue(itemToInt)
        val html = args[0].toString()
        val regex = Regex("""You acquire.*?<b>([^<]+)</b>(?:\s*\((\d+)\))?""")
        for (match in regex.findAll(html)) {
            val name = match.groupValues[1]
            val qty = match.groupValues[2].toIntOrNull() ?: 1
            result[AshValue.item(name)] = AshValue.of(qty.toLong())
        }
        result
    }

    regFn(scope, "extract_meat", AshType.INT,
        listOf("html" to AshType.STRING)) { _, args ->
        val html = args[0].toString()
        val regex = Regex("""You gain (\d[\d,]*) Meat""")
        val match = regex.find(html)
        val meat = match?.groupValues?.getOrNull(1)
            ?.replace(",", "")?.toLongOrNull() ?: 0L
        AshValue.of(meat)
    }

    // ── Phase 992: last_monster_message / last_encounter ────────────
    regFn(scope, "last_monster_message", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(preferences?.getString("lastEncounter", "").orEmpty())
    }

    regFn(scope, "last_encounter", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(preferences?.getString("lastEncounter", "").orEmpty())
    }

    // ── Phase 993: attack / steal / twiddle ─────────────────────────
    regFn(scope, "attack", AshType.BUFFER, emptyList()) { _, _ ->
        AshValue(AshType.BUFFER, StringBuilder("attack"))
    }

    regFn(scope, "steal", AshType.BUFFER, emptyList()) { _, _ ->
        AshValue(AshType.BUFFER, StringBuilder("pickpocket"))
    }

    regFn(scope, "twiddle", AshType.BUFFER, emptyList()) { _, _ ->
        AshValue(AshType.BUFFER, StringBuilder(""))
    }

    // ── Phase 994: curse ────────────────────────────────────────────
    regFn(scope, "curse", AshType.BOOLEAN,
        listOf("target" to AshType.STRING, "item1" to AshType.ITEM,
            "item2" to AshType.ITEM)) { _, _ ->
        AshValue.FALSE
    }

    // ── Phase 995: pickpocket ─────────────────────────────────────
    regFn(scope, "pickpocket", AshType.BUFFER, emptyList()) { _, _ ->
        AshValue(AshType.BUFFER, StringBuilder("pickpocket"))
    }
    // runaway() already registered in AshP894 (Track A) with live HTTP

    // ── Phase 996: expected_damage / monster_level_adjustment ───────
    regFn(scope, "expected_damage", AshType.INT, emptyList()) { _, _ ->
        AshValue.ZERO
    }

    regFn(scope, "expected_damage", AshType.INT,
        listOf("mon" to AshType.MONSTER)) { _, _ ->
        AshValue.ZERO
    }

    regFn(scope, "monster_level_adjustment", AshType.INT, emptyList()) { _, _ ->
        val mods = buildCurrentModifiers()
        val ml = mods.values.get(net.sourceforge.kolmafia.modifiers.DoubleModifier.MONSTER_LEVEL).toInt()
        AshValue.of(ml.toLong())
    }
}
