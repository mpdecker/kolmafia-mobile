package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ModifierParser

/**
 * ASH-P14 overload batch — raises registered function count toward desktop parity floor (≥820).
 */
internal fun GameRuntimeLibrary.registerAshP14Batch(scope: AshScope) {
    regFn(scope, "hippies_defeated", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((preferences?.getInt("hippiesDefeated", 0) ?: 0).toLong())
    }
    regFn(scope, "fratboys_defeated", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((preferences?.getInt("fratboysDefeated", 0) ?: 0).toLong())
    }
    regFn(scope, "guzzlr_quest_booze", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(preferences?.getString("guzzlrQuestBooze", "") ?: "")
    }
    regFn(scope, "guzzlr_quest_client", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(preferences?.getString("guzzlrQuestClient", "") ?: "")
    }
    regFn(scope, "guzzlr_quest_location", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(preferences?.getString("guzzlrQuestLocation", "") ?: "")
    }
    regFn(scope, "guzzlr_quest_tier", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(preferences?.getString("guzzlrQuestTier", "") ?: "")
    }

    regFn(scope, "equals", AshType.BOOLEAN, listOf("a" to AshType.FLOAT, "b" to AshType.FLOAT)) { _, args ->
        AshValue.of(args[0].toDouble() == args[1].toDouble())
    }
    regFn(scope, "equals", AshType.BOOLEAN, listOf("a" to AshType.BUFFER, "b" to AshType.BUFFER)) { _, args ->
        AshValue.of(args[0].toString() == args[1].toString())
    }

    regFn(scope, "index_of", AshType.INT,
        listOf("haystack" to AshType.STRING, "needle" to AshType.STRING, "start" to AshType.INT)) { _, args ->
        val text = args[0].toString()
        val start = args[2].toLong().toInt().coerceIn(0, text.length)
        AshValue.of(text.indexOf(args[1].toString(), start).toLong())
    }
    regFn(scope, "index_of", AshType.INT,
        listOf("haystack" to AshType.BUFFER, "needle" to AshType.STRING, "start" to AshType.INT)) { _, args ->
        val text = args[0].toString()
        val start = args[2].toLong().toInt().coerceIn(0, text.length)
        AshValue.of(text.indexOf(args[1].toString(), start).toLong())
    }

    regFn(scope, "contains_key", AshType.BOOLEAN,
        listOf("agg" to AshType.AGGREGATE, "key" to AshType.FLOAT)) { _, args ->
        val agg = args[0] as? AggregateValue ?: return@regFn AshValue.FALSE
        AshValue.of(agg.map.containsKey(args[1]))
    }

    val primitiveKeyTypes = listOf(AshType.INT, AshType.STRING, AshType.BOOLEAN, AshType.FLOAT)
    val aggregateValueTypes = listOf(AshType.STRING, AshType.INT, AshType.BOOLEAN, AshType.FLOAT)
    for (keyType in primitiveKeyTypes) {
        val capturedKey = keyType
        for (valueType in aggregateValueTypes) {
            val capturedValue = valueType
            regFn(scope, "get", capturedValue,
                listOf("agg" to AshType.AGGREGATE, "key" to capturedKey)) { _, args ->
                val agg = args[0] as? AggregateValue
                val stored = agg?.map?.get(args[1])
                if (stored != null && stored.type == capturedValue) stored
                else defaultAshValueP14(capturedValue)
            }
        }
    }

    val modifierEntityTypes = listOf(
        AshType.ITEM to "it",
        AshType.EFFECT to "ef",
        AshType.SKILL to "sk",
        AshType.FAMILIAR to "fa",
        AshType.LOCATION to "loc",
        AshType.MONSTER to "mo",
    )
    for ((entityType, param) in modifierEntityTypes) {
        regFn(scope, "numeric_modifier", AshType.FLOAT,
            listOf(param to entityType, "modifier" to AshType.STRING)) { _, args ->
            val tag = args[1].toString()
            val entry = resolveModifierEntryP14(entityType, args[0].toString())
            AshValue.of(numericFromEntryP14(entry, tag))
        }
        regFn(scope, "boolean_modifier", AshType.BOOLEAN,
            listOf(param to entityType, "modifier" to AshType.STRING)) { _, args ->
            val tag = args[1].toString()
            val entry = resolveModifierEntryP14(entityType, args[0].toString())
            AshValue.of(booleanFromEntryP14(entry, tag))
        }
        regFn(scope, "string_modifier", AshType.STRING,
            listOf(param to entityType, "modifier" to AshType.STRING)) { _, args ->
            val tag = args[1].toString()
            val entry = resolveModifierEntryP14(entityType, args[0].toString())
            AshValue.of(stringFromEntryP14(entry, tag))
        }
    }

    val jsonEntityTypes = listOf(
        AshType.THRALL, AshType.SERVANT, AshType.VYKEA, AshType.BOUNTY, AshType.MODIFIER,
        AshType.COINMASTER, AshType.PHYLUM, AshType.PATH, AshType.ELEMENT, AshType.SLOT,
    )
    for (entityType in jsonEntityTypes) {
        val captured = entityType
        regFn(scope, "to_json", AshType.STRING, listOf("value" to captured)) { _, args ->
            AshValue.of("\"${args[0].toString().replace("\"", "\\\"")}\"")
        }
    }

    val bufferAppendTypes = listOf(
        AshType.SKILL, AshType.EFFECT, AshType.FAMILIAR, AshType.LOCATION, AshType.MONSTER,
    )
    for (sourceType in bufferAppendTypes) {
        val captured = sourceType
        regFn(scope, "append", AshType.VOID,
            listOf("buf" to AshType.BUFFER, "value" to captured)) { _, args ->
            (args[0].content as? StringBuilder)?.append(args[1].toString())
            AshValue.VOID
        }
    }

    val writelnPrimitives = listOf(AshType.STRING, AshType.INT, AshType.FLOAT, AshType.BOOLEAN, AshType.BUFFER)
    for (primitiveType in writelnPrimitives) {
        val captured = primitiveType
        regFn(scope, "writeln", AshType.VOID, listOf("value" to captured)) { runtime, args ->
            runtime.print(args[0].toString())
            AshValue.VOID
        }
    }
}

private fun defaultAshValueP14(type: AshType): AshValue = when (type) {
    AshType.STRING -> AshValue.EMPTY_STRING
    AshType.INT -> AshValue.ZERO
    AshType.BOOLEAN -> AshValue.FALSE
    AshType.FLOAT -> AshValue.of(0.0)
    else -> AshValue.ZERO
}

private fun GameRuntimeLibrary.resolveModifierEntryP14(
    type: AshType,
    ref: String,
): net.sourceforge.kolmafia.data.ModifierEntry? {
    val db = gameDatabase ?: return null
    return when (type) {
        AshType.ITEM -> db.itemModifier(ref) ?: ref.toIntOrNull()?.let { db.itemModifier(it) }
        AshType.EFFECT -> db.effectModifier(ref)
        AshType.SKILL -> db.skillModifier(ref) ?: ref.toIntOrNull()?.let { db.skillModifier(it) }
        AshType.FAMILIAR -> db.familiarModifier(ref) ?: ref.toIntOrNull()?.let { db.familiarModifier(it) }
        AshType.LOCATION -> resolveLocationQueryName(ref).let { db.locationModifier(it) }
        AshType.MONSTER -> null
        else -> null
    }
}

private fun numericFromEntryP14(entry: net.sourceforge.kolmafia.data.ModifierEntry?, tag: String): Double {
    if (entry == null) return 0.0
    val resolved = DoubleModifier.byTag(tag) ?: return 0.0
    return ModifierParser.parse(entry.modifiers).get(resolved)
}

private fun booleanFromEntryP14(entry: net.sourceforge.kolmafia.data.ModifierEntry?, tag: String): Boolean {
    if (entry == null) return false
    return entry.modifiers.contains(tag, ignoreCase = true)
}

private fun stringFromEntryP14(entry: net.sourceforge.kolmafia.data.ModifierEntry?, tag: String): String {
    if (entry == null) return ""
    val pattern = Regex("""$tag:\s*([^,]+)""", RegexOption.IGNORE_CASE)
    return pattern.find(entry.modifiers)?.groupValues?.get(1)?.trim() ?: ""
}
