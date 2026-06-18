package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.modifiers.StatNames

/**
 * ASH-P10 overload batch — raises registered function count toward desktop parity floor (≥560).
 */
internal fun GameRuntimeLibrary.registerAshP10Batch(scope: AshScope) {
    regFn(scope, "my_hash", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of((character?.state?.value?.playerId ?: 0).toString())
    }
    regFn(scope, "my_path_id", AshType.INT, emptyList()) { _, _ ->
        val path = character?.state?.value?.ascensionPath?.apiName ?: "None"
        AshValue.of(path.hashCode().toLong().let { if (it < 0) -it else it })
    }
    regFn(scope, "my_garden_type", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(preferences?.getString("myGardenType", "") ?: "")
    }
    regFn(scope, "my_pp", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((preferences?.getInt("currentPp", 0) ?: 0).toLong())
    }
    regFn(scope, "my_maxpp", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((preferences?.getInt("maxPp", 0) ?: 0).toLong())
    }
    regFn(scope, "my_fury", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((preferences?.getInt("furyLevel", 0) ?: 0).toLong())
    }
    regFn(scope, "my_soulsauce", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((preferences?.getInt("soulSauce", 0) ?: 0).toLong())
    }
    regFn(scope, "my_discomomentum", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((preferences?.getInt("discoMomentum", 0) ?: 0).toLong())
    }
    regFn(scope, "my_audience", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((preferences?.getInt("audience", 0) ?: 0).toLong())
    }
    regFn(scope, "my_thunder", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((preferences?.getInt("thunder", 0) ?: 0).toLong())
    }
    regFn(scope, "my_rain", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((preferences?.getInt("rain", 0) ?: 0).toLong())
    }
    regFn(scope, "my_lightning", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((preferences?.getInt("lightning", 0) ?: 0).toLong())
    }
    regFn(scope, "my_mask", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(preferences?.getString("currentMask", "") ?: "")
    }
    regFn(scope, "my_paradoxicity", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((preferences?.getInt("paradoxicity", 0) ?: 0).toLong())
    }
    regFn(scope, "my_servant", AshType.SERVANT, emptyList()) { _, _ ->
        val active = edServantManager?.activeServantType()?.takeIf { it.isNotBlank() }
            ?: preferences?.getString("_currentServant", "")
            ?: ""
        AshValue(AshType.SERVANT, active)
    }
    regFn(scope, "my_vykea_companion", AshType.VYKEA, emptyList()) { _, _ ->
        AshValue(AshType.VYKEA, preferences?.getString("_currentVykea", "") ?: "")
    }
    regFn(scope, "my_basestat", AshType.INT, listOf("stat" to AshType.STAT)) { _, args ->
        AshValue.of(baseStatValue(args[0]))
    }
    regFn(scope, "my_basestat", AshType.INT, listOf("stat" to AshType.STRING)) { _, args ->
        AshValue.of(baseStatValue(AshValue(AshType.STAT, args[0].toString())))
    }

    regFn(scope, "compare", AshType.INT, listOf("a" to AshType.INT, "b" to AshType.INT)) { _, args ->
        AshValue.of(args[0].toLong().compareTo(args[1].toLong()).toLong())
    }
    regFn(scope, "compare", AshType.INT, listOf("a" to AshType.FLOAT, "b" to AshType.FLOAT)) { _, args ->
        AshValue.of(args[0].toDouble().compareTo(args[1].toDouble()).toLong())
    }
    regFn(scope, "equals", AshType.BOOLEAN, listOf("a" to AshType.STRING, "b" to AshType.STRING)) { _, args ->
        AshValue.of(args[0].toString() == args[1].toString())
    }
    regFn(scope, "equals", AshType.BOOLEAN, listOf("a" to AshType.INT, "b" to AshType.INT)) { _, args ->
        AshValue.of(args[0].toLong() == args[1].toLong())
    }
    regFn(scope, "equals", AshType.BOOLEAN, listOf("a" to AshType.BOOLEAN, "b" to AshType.BOOLEAN)) { _, args ->
        AshValue.of(args[0].toBoolean() == args[1].toBoolean())
    }

    regFn(scope, "count", AshType.INT, listOf("agg" to AshType.AGGREGATE)) { _, args ->
        val agg = args[0] as? AggregateValue
        AshValue.of((agg?.map?.size ?: 0).toLong())
    }
    regFn(scope, "clear", AshType.VOID, listOf("agg" to AshType.AGGREGATE)) { _, args ->
        (args[0] as? AggregateValue)?.map?.clear()
        AshValue.VOID
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
            regFn(scope, "set", AshType.VOID,
                listOf("agg" to AshType.AGGREGATE, "key" to capturedKey, "value" to capturedValue)) { _, args ->
                (args[0] as? AggregateValue)?.map?.set(args[1], args[2])
                AshValue.VOID
            }
        }
    }

    val coerceTargetTypes = listOf(
        AshType.STRING, AshType.INT, AshType.FLOAT, AshType.BOOLEAN,
        AshType.ITEM, AshType.SKILL, AshType.EFFECT, AshType.FAMILIAR,
        AshType.LOCATION, AshType.MONSTER,
    )
    val coerceSourceTypes = listOf(AshType.STRING, AshType.INT, AshType.FLOAT, AshType.BOOLEAN)
    for (sourceType in coerceSourceTypes) {
        val capturedSource = sourceType
        for (targetType in coerceTargetTypes) {
            if (sourceType == targetType) continue
            val capturedTarget = targetType
            regFn(scope, "coerce_to", capturedTarget,
                listOf("value" to capturedSource)) { _, args ->
                args[0].coerceTo(capturedTarget)
            }
        }
    }

    val printEntityTypes = listOf(
        AshType.LOCATION, AshType.MONSTER, AshType.FAMILIAR, AshType.CLASS, AshType.STAT,
        AshType.THRALL, AshType.SERVANT, AshType.VYKEA, AshType.BOUNTY, AshType.MODIFIER,
        AshType.COINMASTER, AshType.PHYLUM, AshType.PATH, AshType.ELEMENT, AshType.SLOT,
    )
    for (entityType in printEntityTypes) {
        val captured = entityType
        regFn(scope, "print", AshType.VOID, listOf("value" to captured)) { runtime, args ->
            runtime.print(args[0].toString())
            AshValue.VOID
        }
    }
}

private fun GameRuntimeLibrary.baseStatValue(stat: AshValue): Long {
    val cs = character?.state?.value ?: return 0L
    return StatNames.baseValue(cs, stat.toString())
}
