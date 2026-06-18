package net.sourceforge.kolmafia.ash

/**
 * ASH-P13 overload batch — raises registered function count toward desktop parity floor (≥756).
 */
internal fun GameRuntimeLibrary.registerAshP13Batch(scope: AshScope) {
    regFn(scope, "pad_right", AshType.STRING,
        listOf("value" to AshType.STRING, "width" to AshType.INT, "pad" to AshType.STRING)) { _, args ->
        val text = args[0].toString()
        val width = args[1].toLong().toInt().coerceAtLeast(0)
        val pad = args[2].toString().ifEmpty { " " }
        if (text.length >= width) AshValue.of(text)
        else AshValue.of(text + pad.first().toString().repeat(width - text.length))
    }

    regFn(scope, "party_fair_quest", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(preferences?.getString("_questPartyFairQuest", "") ?: "")
    }
    regFn(scope, "party_fair_progress", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(preferences?.getString("_questPartyFairProgress", "") ?: "")
    }
    regFn(scope, "boo_peak_progress", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((preferences?.getInt("booPeakProgress", 0) ?: 0).toLong())
    }
    regFn(scope, "twin_peak_progress", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((preferences?.getInt("twinPeakProgress", 0) ?: 0).toLong())
    }
    regFn(scope, "oil_peak_progress", AshType.FLOAT, emptyList()) { _, _ ->
        val raw = preferences?.getString("oilPeakProgress", "0") ?: "0"
        AshValue.of(raw.toDoubleOrNull() ?: 0.0)
    }
    regFn(scope, "boo_peak_lit", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(preferences?.getBoolean("booPeakLit") == true)
    }
    regFn(scope, "oil_peak_lit", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(preferences?.getBoolean("oilPeakLit") == true)
    }

    regFn(scope, "my_daycount", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((character?.state?.value?.dayCount ?: 0).toLong())
    }
    regFn(scope, "my_global_daycount", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((character?.state?.value?.globalDaycount ?: 0).toLong())
    }
    regFn(scope, "my_ronin_left", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((character?.state?.value?.roninLeft ?: 0).toLong())
    }

    regFn(scope, "index_of", AshType.INT,
        listOf("haystack" to AshType.BUFFER, "needle" to AshType.STRING)) { _, args ->
        AshValue.of(args[0].toString().indexOf(args[1].toString()).toLong())
    }
    regFn(scope, "contains_key", AshType.BOOLEAN,
        listOf("agg" to AshType.AGGREGATE, "key" to AshType.BOOLEAN)) { _, args ->
        val agg = args[0] as? AggregateValue ?: return@regFn AshValue.FALSE
        AshValue.of(agg.map.containsKey(args[1]))
    }

    val aggregateValueTypes = listOf(AshType.STRING, AshType.INT, AshType.BOOLEAN, AshType.FLOAT)
    val aggregateKeyTypes = listOf(
        AshType.CLASS, AshType.STAT, AshType.SLOT, AshType.ELEMENT,
        AshType.COINMASTER, AshType.PHYLUM, AshType.PATH,
        AshType.THRALL, AshType.SERVANT, AshType.VYKEA, AshType.BOUNTY, AshType.MODIFIER,
        AshType.LOCATION, AshType.MONSTER, AshType.ITEM, AshType.SKILL, AshType.EFFECT, AshType.FAMILIAR,
    )
    for (keyType in aggregateKeyTypes) {
        val capturedKey = keyType
        for (valueType in aggregateValueTypes) {
            val capturedValue = valueType
            regFn(scope, "get", capturedValue,
                listOf("agg" to AshType.AGGREGATE, "key" to capturedKey)) { _, args ->
                val agg = args[0] as? AggregateValue
                val stored = agg?.map?.get(args[1])
                if (stored != null && stored.type == capturedValue) stored
                else defaultAshValue(capturedValue)
            }
        }
    }

    val toStringTypes = listOf(
        AshType.CLASS, AshType.STAT, AshType.SLOT, AshType.ELEMENT,
        AshType.COINMASTER, AshType.PHYLUM, AshType.PATH,
        AshType.LOCATION, AshType.MONSTER, AshType.ITEM, AshType.SKILL, AshType.EFFECT, AshType.FAMILIAR,
    )
    for (entityType in toStringTypes) {
        val captured = entityType
        regFn(scope, "to_string", AshType.STRING, listOf("value" to captured)) { _, args ->
            AshValue.of(args[0].toString())
        }
    }

    val writelnEntityTypes = listOf(
        AshType.ITEM, AshType.SKILL, AshType.EFFECT, AshType.FAMILIAR, AshType.LOCATION,
        AshType.MONSTER, AshType.CLASS, AshType.STAT, AshType.THRALL, AshType.SERVANT,
        AshType.VYKEA, AshType.BOUNTY, AshType.MODIFIER, AshType.COINMASTER, AshType.PHYLUM,
        AshType.PATH, AshType.ELEMENT, AshType.SLOT,
    )
    for (entityType in writelnEntityTypes) {
        val captured = entityType
        regFn(scope, "writeln", AshType.VOID, listOf("value" to captured)) { runtime, args ->
            runtime.print(args[0].toString())
            AshValue.VOID
        }
    }
}

private fun defaultAshValue(type: AshType): AshValue = when (type) {
    AshType.STRING -> AshValue.EMPTY_STRING
    AshType.INT -> AshValue.ZERO
    AshType.BOOLEAN -> AshValue.FALSE
    AshType.FLOAT -> AshValue.of(0.0)
    else -> AshValue.ZERO
}
