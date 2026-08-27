package net.sourceforge.kolmafia.ash

/**
 * AshP997–1003 Track S — Character niche residuals.
 *
 * Phase 997: my_maxfury, my_ram, my_wildfire_water
 * Phase 998: minstrel_* (pref-backed)
 * Phase 999: heist(int), dart_parts_to_skills
 * Phase 1000: beret_bonus, mobius_bonus
 * Phase 1001: sausage_bonus, autumnaton_*
 * Phase 1002: locket_monster_map, florist_plants
 * Phase 1003: voting_booth_initiatives
 */
internal fun GameRuntimeLibrary.registerAshP997TrackSBatch(scope: AshScope) {
    // ── Phase 997: my_maxfury / my_ram / my_wildfire_water ───────────
    regFn(scope, "my_maxfury", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(3L)
    }

    regFn(scope, "my_ram", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((preferences?.getInt("_ramDrinks", 0) ?: 0).toLong())
    }

    regFn(scope, "my_wildfire_water", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((preferences?.getInt("wildfireWater", 0) ?: 0).toLong())
    }

    // ── Phase 998: minstrel pref-backed ─────────────────────────────
    regFn(scope, "minstrel_level", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((preferences?.getInt("clancyLevel", 1) ?: 1).toLong())
    }

    regFn(scope, "minstrel_instrument", AshType.ITEM, emptyList()) { _, _ ->
        val inst = preferences?.getString("clancyInstrument", "").orEmpty()
        AshValue.item(inst)
    }

    regFn(scope, "minstrel_quest", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(preferences?.getBoolean("clancyHasQuest", false) ?: false)
    }

    // ── Phase 999: heist / dart_parts_to_skills ─────────────────────
    val itemToItem = AggregateType(AshType.ITEM, AshType.ITEM)
    regFn(scope, "heist", itemToItem, listOf("count" to AshType.INT)) { _, _ ->
        AggregateValue(itemToItem)
    }

    regFn(scope, "heist", itemToItem, emptyList()) { _, _ ->
        AggregateValue(itemToItem)
    }

    val stringToBoolean = AggregateType(AshType.STRING, AshType.BOOLEAN)
    regFn(scope, "dart_parts_to_skills", stringToBoolean, emptyList()) { _, _ ->
        val result = AggregateValue(stringToBoolean)
        val parts = preferences?.getString("dartPerks", "")?.takeIf { it.isNotBlank() }
        if (parts != null) {
            for (part in parts.split("|").filter { it.isNotBlank() }) {
                result[AshValue.of(part)] = AshValue.TRUE
            }
        }
        result
    }

    // ── Phase 1000: beret_bonus / mobius_bonus ──────────────────────
    regFn(scope, "beret_bonus", AshType.FLOAT, emptyList()) { _, _ ->
        AshValue.of(0.0)
    }

    regFn(scope, "mobius_bonus", AshType.FLOAT, emptyList()) { _, _ ->
        AshValue.of(0.0)
    }

    // ── Phase 1001: sausage_bonus / autumnaton prefs ────────────────
    regFn(scope, "sausage_bonus", AshType.FLOAT, emptyList()) { _, _ ->
        AshValue.of(0.0)
    }

    val stringArray = AggregateType(AshType.INT, AshType.STRING)
    regFn(scope, "autumnaton_locations", stringArray, emptyList()) { _, _ ->
        val result = AggregateValue(stringArray)
        val locs = preferences?.getString("autumnatonLocations", "")?.takeIf { it.isNotBlank() }
        if (locs != null) {
            for ((i, loc) in locs.split("|").filter { it.isNotBlank() }.withIndex()) {
                result[AshValue.of(i)] = AshValue.of(loc)
            }
        }
        result
    }

    // ── Phase 1002: locket monsters / florist plants ────────────────
    val monsterArray = AggregateType(AshType.INT, AshType.MONSTER)
    regFn(scope, "get_locket_monsters", monsterArray, emptyList()) { _, _ ->
        val result = AggregateValue(monsterArray)
        val raw = preferences?.getString("_locketMonstersFought", "")?.takeIf { it.isNotBlank() }
        if (raw != null) {
            for ((i, name) in raw.split("|").filter { it.isNotBlank() }.withIndex()) {
                result[AshValue.of(i)] = AshValue(AshType.MONSTER, name)
            }
        }
        result
    }

    val stringToStringArray = AggregateType(AshType.LOCATION, AggregateType(AshType.INT, AshType.STRING))
    regFn(scope, "florist_available", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(preferences?.getBoolean("_floristPlantsAvailable", false) ?: false)
    }

    // ── Phase 1003: voting booth initiatives ────────────────────────
    val intToString = AggregateType(AshType.INT, AshType.STRING)
    regFn(scope, "voting_booth_initiatives", intToString,
        listOf("clss" to AshType.INT, "path" to AshType.INT,
            "daycount" to AshType.INT)) { _, _ ->
        AggregateValue(intToString)
    }
}
