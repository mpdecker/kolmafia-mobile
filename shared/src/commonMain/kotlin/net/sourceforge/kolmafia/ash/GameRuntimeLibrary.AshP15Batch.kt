package net.sourceforge.kolmafia.ash

/**
 * ASH-P15 overload batch — raises registered function count toward desktop parity floor (≥890).
 */
internal fun GameRuntimeLibrary.registerAshP15Batch(scope: AshScope) {
    regFn(scope, "ltt_quest_name", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(preferences?.getString("lttQuestName", "") ?: "")
    }
    regFn(scope, "ltt_quest_difficulty", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((preferences?.getInt("lttQuestDifficulty", 0) ?: 0).toLong())
    }
    regFn(scope, "source_oracle_target", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(preferences?.getString("sourceOracleTarget", "") ?: "")
    }
    regFn(scope, "ghost_location", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(preferences?.getString("ghostLocation", "") ?: "")
    }
    regFn(scope, "shen_quest_item", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(preferences?.getString("shenQuestItem", "") ?: "")
    }
    regFn(scope, "doctor_bag_quest_item", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(preferences?.getString("doctorBagQuestItem", "") ?: "")
    }
    regFn(scope, "doctor_bag_quest_location", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(preferences?.getString("doctorBagQuestLocation", "") ?: "")
    }
    regFn(scope, "new_you_quest_skill", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(preferences?.getString("_newYouQuestSkill", "") ?: "")
    }
    regFn(scope, "new_you_quest_monster", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(preferences?.getString("_newYouQuestMonster", "") ?: "")
    }
    regFn(scope, "ns_contestants_left", AshType.INT, listOf("contest" to AshType.INT)) { _, args ->
        val contest = args[0].toLong().toInt()
        AshValue.of((preferences?.getInt("nsContestants$contest", -1) ?: -1).toLong())
    }
    regFn(scope, "cyrus_adjectives", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(preferences?.getString("cyrusAdjectives", "") ?: "")
    }

    regFn(scope, "contains_key", AshType.BOOLEAN,
        listOf("agg" to AshType.AGGREGATE, "key" to AshType.BOOLEAN)) { _, args ->
        val agg = args[0] as? AggregateValue ?: return@regFn AshValue.FALSE
        AshValue.of(agg.map.containsKey(args[1]))
    }
    regFn(scope, "contains_key", AshType.BOOLEAN,
        listOf("agg" to AshType.AGGREGATE, "key" to AshType.FLOAT)) { _, args ->
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
            regFn(scope, "set", AshType.VOID,
                listOf("agg" to AshType.AGGREGATE, "key" to capturedKey, "value" to capturedValue)) { _, args ->
                (args[0] as? AggregateValue)?.map?.set(args[1], args[2])
                AshValue.VOID
            }
        }
    }

    val removeEntityKeyTypes = listOf(
        AshType.LOCATION, AshType.CLASS, AshType.STAT, AshType.SLOT, AshType.ELEMENT,
        AshType.COINMASTER, AshType.PHYLUM, AshType.PATH,
        AshType.THRALL, AshType.SERVANT, AshType.VYKEA, AshType.BOUNTY, AshType.MODIFIER,
        AshType.BOOLEAN, AshType.FLOAT,
    )
    for (keyType in removeEntityKeyTypes) {
        val captured = keyType
        regFn(scope, "remove", AshType.VOID,
            listOf("agg" to AshType.AGGREGATE, "key" to captured)) { _, args ->
            (args[0] as? AggregateValue)?.map?.remove(args[1])
            AshValue.VOID
        }
    }

    regFn(scope, "split", AshType.AGGREGATE,
        listOf("value" to AshType.STRING, "sep" to AshType.STRING, "limit" to AshType.INT)) { _, args ->
        splitToAggregateP15(args[0].toString(), args[1].toString(), args[2].toLong().toInt())
    }
    regFn(scope, "split", AshType.AGGREGATE,
        listOf("value" to AshType.BUFFER, "sep" to AshType.STRING, "limit" to AshType.INT)) { _, args ->
        splitToAggregateP15(args[0].toString(), args[1].toString(), args[2].toLong().toInt())
    }

    val writelnEntityTypes = listOf(
        AshType.ITEM, AshType.SKILL, AshType.EFFECT, AshType.FAMILIAR, AshType.LOCATION, AshType.MONSTER,
    )
    for (entityType in writelnEntityTypes) {
        val captured = entityType
        regFn(scope, "writeln", AshType.VOID, listOf("value" to captured)) { runtime, args ->
            runtime.print(args[0].toString())
            AshValue.VOID
        }
    }
}

private fun splitToAggregateP15(text: String, sep: String, limit: Int): AggregateValue {
    val result = AggregateValue(AggregateType(AshType.INT, AshType.STRING))
    val parts = if (limit > 0) text.split(sep, limit = limit) else text.split(sep)
    parts.forEachIndexed { index, part -> result[AshValue.of(index)] = AshValue.of(part) }
    return result
}
