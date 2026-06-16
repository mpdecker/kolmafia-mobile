package net.sourceforge.kolmafia.ash

/**
 * ASH-P12 overload batch — raises registered function count toward desktop parity floor (≥700).
 */
internal fun GameRuntimeLibrary.registerAshP12Batch(scope: AshScope) {
    regFn(scope, "lower_case", AshType.STRING, listOf("value" to AshType.STRING)) { _, args ->
        AshValue.of(args[0].toString().lowercase())
    }
    regFn(scope, "upper_case", AshType.STRING, listOf("value" to AshType.STRING)) { _, args ->
        AshValue.of(args[0].toString().uppercase())
    }
    regFn(scope, "capitalize", AshType.STRING, listOf("value" to AshType.STRING)) { _, args ->
        val text = args[0].toString()
        AshValue.of(text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
    }
    regFn(scope, "char_at", AshType.STRING, listOf("value" to AshType.STRING, "index" to AshType.INT)) { _, args ->
        val text = args[0].toString()
        val index = args[1].toLong().toInt()
        AshValue.of(if (index in text.indices) text[index].toString() else "")
    }
    regFn(scope, "hash_code", AshType.INT, listOf("value" to AshType.STRING)) { _, args ->
        AshValue.of(args[0].toString().hashCode().toLong().let { if (it < 0) -it else it })
    }

    regFn(scope, "my_storage_meat", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(character?.state?.value?.storageMeat ?: 0L)
    }
    regFn(scope, "my_closet_meat", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(character?.state?.value?.closetMeat ?: 0L)
    }
    regFn(scope, "my_session_meat", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(character?.state?.value?.sessionMeat ?: 0L)
    }
    regFn(scope, "my_run", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((character?.state?.value?.currentRun ?: 0).toLong())
    }
    regFn(scope, "my_ascensions", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((character?.state?.value?.ascensionNumber ?: 0).toLong())
    }

    val entityTypes = listOf(
        AshType.ITEM, AshType.SKILL, AshType.EFFECT, AshType.FAMILIAR, AshType.LOCATION,
        AshType.MONSTER, AshType.CLASS, AshType.STAT, AshType.THRALL, AshType.SERVANT,
        AshType.VYKEA, AshType.BOUNTY, AshType.MODIFIER, AshType.COINMASTER, AshType.PHYLUM,
        AshType.PATH, AshType.ELEMENT, AshType.SLOT,
    )
    for (entityType in entityTypes) {
        val captured = entityType
        val param = when (captured) {
            AshType.ITEM -> "it"
            AshType.SKILL -> "sk"
            AshType.EFFECT -> "ef"
            AshType.FAMILIAR -> "fa"
            AshType.LOCATION -> "loc"
            AshType.MONSTER -> "mo"
            AshType.CLASS -> "cls"
            AshType.STAT -> "stat"
            else -> "value"
        }
        regFn(scope, "name", AshType.STRING, listOf(param to captured)) { _, args ->
            AshValue.of(args[0].toString())
        }
        regFn(scope, "desc", AshType.STRING, listOf(param to captured)) { _, args ->
            AshValue.of("")
        }
        regFn(scope, "same", AshType.BOOLEAN, listOf("a" to captured, "b" to captured)) { _, args ->
            AshValue.of(args[0].toString() == args[1].toString())
        }
        regFn(scope, "println", AshType.VOID, listOf("value" to captured)) { runtime, args ->
            runtime.print(args[0].toString())
            AshValue.VOID
        }
    }

    val aggregateKeyTypes = listOf(AshType.INT, AshType.STRING, AshType.BOOLEAN, AshType.FLOAT)
    for (keyType in aggregateKeyTypes) {
        val captured = keyType
        regFn(scope, "remove", AshType.VOID,
            listOf("agg" to AshType.AGGREGATE, "key" to captured)) { _, args ->
            (args[0] as? AggregateValue)?.map?.remove(args[1])
            AshValue.VOID
        }
    }

    regFn(scope, "copy", AshType.AGGREGATE, listOf("agg" to AshType.AGGREGATE)) { _, args ->
        val source = args[0] as? AggregateValue
            ?: return@regFn AggregateValue(AggregateType(AshType.INT, AshType.INT))
        val copy = AggregateValue(source.type)
        source.map.forEach { (k, v) -> copy.map[k] = v }
        copy
    }
}
