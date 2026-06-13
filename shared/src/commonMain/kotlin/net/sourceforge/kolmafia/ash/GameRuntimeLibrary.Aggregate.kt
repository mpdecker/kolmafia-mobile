package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerAggregateExtensions(scope: AshScope) {
    val stringIntAgg = AggregateType(AshType.INT, AshType.STRING)

    regFn(scope, "join_string", AshType.STRING,
        listOf("agg" to stringIntAgg, "sep" to AshType.STRING)) { _, args ->
        val agg = args[0] as AggregateValue
        val sep = args[1].toString()
        val parts = agg.map.entries
            .sortedBy { it.key.toLong() }
            .map { it.value.toString() }
        AshValue.of(parts.joinToString(sep))
    }

    val stringStringAgg = AggregateType(AshType.STRING, AshType.STRING)
    regFn(scope, "join_string", AshType.STRING,
        listOf("agg" to stringStringAgg, "sep" to AshType.STRING)) { _, args ->
        val agg = args[0] as AggregateValue
        val sep = args[1].toString()
        val parts = agg.map.entries
            .sortedBy { it.key.toString() }
            .map { it.value.toString() }
        AshValue.of(parts.joinToString(sep))
    }

    regFn(scope, "contains_key", AshType.BOOLEAN,
        listOf("agg" to AshType.AGGREGATE, "key" to AshType.INT)) { _, args ->
        val agg = args[0] as? AggregateValue ?: return@regFn AshValue.FALSE
        AshValue.of(agg.map.containsKey(args[1]))
    }
    regFn(scope, "contains_key", AshType.BOOLEAN,
        listOf("agg" to AshType.AGGREGATE, "key" to AshType.STRING)) { _, args ->
        val agg = args[0] as? AggregateValue ?: return@regFn AshValue.FALSE
        AshValue.of(agg.map.containsKey(args[1]))
    }
    regFn(scope, "contains_key", AshType.BOOLEAN,
        listOf("agg" to AshType.AGGREGATE, "key" to AshType.ITEM)) { _, args ->
        val agg = args[0] as? AggregateValue ?: return@regFn AshValue.FALSE
        AshValue.of(agg.map.containsKey(args[1]))
    }
    regFn(scope, "contains_key", AshType.BOOLEAN,
        listOf("agg" to AshType.AGGREGATE, "key" to AshType.MONSTER)) { _, args ->
        val agg = args[0] as? AggregateValue ?: return@regFn AshValue.FALSE
        AshValue.of(agg.map.containsKey(args[1]))
    }
    regFn(scope, "contains_key", AshType.BOOLEAN,
        listOf("agg" to AshType.AGGREGATE, "key" to AshType.LOCATION)) { _, args ->
        val agg = args[0] as? AggregateValue ?: return@regFn AshValue.FALSE
        AshValue.of(agg.map.containsKey(args[1]))
    }
    regFn(scope, "contains_key", AshType.BOOLEAN,
        listOf("agg" to AshType.AGGREGATE, "key" to AshType.SKILL)) { _, args ->
        val agg = args[0] as? AggregateValue ?: return@regFn AshValue.FALSE
        AshValue.of(agg.map.containsKey(args[1]))
    }
    regFn(scope, "contains_key", AshType.BOOLEAN,
        listOf("agg" to AshType.AGGREGATE, "key" to AshType.EFFECT)) { _, args ->
        val agg = args[0] as? AggregateValue ?: return@regFn AshValue.FALSE
        AshValue.of(agg.map.containsKey(args[1]))
    }
    regFn(scope, "contains_key", AshType.BOOLEAN,
        listOf("agg" to AshType.AGGREGATE, "key" to AshType.FAMILIAR)) { _, args ->
        val agg = args[0] as? AggregateValue ?: return@regFn AshValue.FALSE
        AshValue.of(agg.map.containsKey(args[1]))
    }

    regFn(scope, "remove", AshType.VOID,
        listOf("agg" to AshType.AGGREGATE, "key" to AshType.MONSTER)) { _, args ->
        (args[0] as? AggregateValue)?.map?.remove(args[1])
        AshValue.VOID
    }
    regFn(scope, "remove", AshType.VOID,
        listOf("agg" to AshType.AGGREGATE, "key" to AshType.SKILL)) { _, args ->
        (args[0] as? AggregateValue)?.map?.remove(args[1])
        AshValue.VOID
    }
    regFn(scope, "remove", AshType.VOID,
        listOf("agg" to AshType.AGGREGATE, "key" to AshType.EFFECT)) { _, args ->
        (args[0] as? AggregateValue)?.map?.remove(args[1])
        AshValue.VOID
    }
    regFn(scope, "remove", AshType.VOID,
        listOf("agg" to AshType.AGGREGATE, "key" to AshType.FAMILIAR)) { _, args ->
        (args[0] as? AggregateValue)?.map?.remove(args[1])
        AshValue.VOID
    }
    regFn(scope, "remove", AshType.VOID,
        listOf("agg" to AshType.AGGREGATE, "key" to AshType.INT)) { _, args ->
        (args[0] as? AggregateValue)?.map?.remove(args[1])
        AshValue.VOID
    }
    regFn(scope, "remove", AshType.VOID,
        listOf("agg" to AshType.AGGREGATE, "key" to AshType.STRING)) { _, args ->
        (args[0] as? AggregateValue)?.map?.remove(args[1])
        AshValue.VOID
    }
    regFn(scope, "remove", AshType.VOID,
        listOf("agg" to AshType.AGGREGATE, "key" to AshType.ITEM)) { _, args ->
        (args[0] as? AggregateValue)?.map?.remove(args[1])
        AshValue.VOID
    }

    regFn(scope, "keys", AshType.AGGREGATE,
        listOf("agg" to AshType.AGGREGATE)) { _, args ->
        val agg = args[0] as? AggregateValue
            ?: return@regFn AggregateValue(AggregateType(AshType.INT, AshType.INT))
        agg.keys()
    }

    regFn(scope, "values", AshType.AGGREGATE,
        listOf("agg" to AshType.AGGREGATE)) { _, args ->
        val agg = args[0] as? AggregateValue
            ?: return@regFn AggregateValue(AggregateType(AshType.INT, AshType.INT))
        val result = AggregateValue(AggregateType(AshType.INT, agg.type.dataType))
        agg.map.values.forEachIndexed { i, v -> result[AshValue.of(i)] = v }
        result
    }
}
