package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.ModifierEntry
import net.sourceforge.kolmafia.modifiers.BooleanModifier
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ModifierParser
import net.sourceforge.kolmafia.modifiers.StringModifier

/**
 * ASH Semantic Closure — phases 3651–3710.
 *
 * These overloads complete the value-bearing portions of modifier introspection
 * without inventing data for entity classes that have no modifiers.txt rows.
 */
internal fun GameRuntimeLibrary.registerPhase3710(scope: AshScope) {
    regFn(scope, "numeric_modifier", AshType.FLOAT,
        listOf("typeName" to AshType.STRING, "modifier" to AshType.MODIFIER)) { _, args ->
        val (type, name) = parseTypeName(args[0].toString())
        numericStatic(ModifierDatabase.get(type, name), args[1].toString())
    }
    regFn(scope, "boolean_modifier", AshType.BOOLEAN,
        listOf("typeName" to AshType.STRING, "modifier" to AshType.MODIFIER)) { _, args ->
        val (type, name) = parseTypeName(args[0].toString())
        booleanStatic(ModifierDatabase.get(type, name), args[1].toString())
    }
    regFn(scope, "string_modifier", AshType.STRING,
        listOf("typeName" to AshType.STRING, "modifier" to AshType.MODIFIER)) { _, args ->
        val (type, name) = parseTypeName(args[0].toString())
        stringStatic(ModifierDatabase.get(type, name), args[1].toString())
    }
    regFn(scope, "numeric_modifier", AshType.FLOAT,
        listOf("modifier" to AshType.MODIFIER)) { _, args ->
        numericCurrent(args[0].toString())
    }
    regFn(scope, "boolean_modifier", AshType.BOOLEAN,
        listOf("modifier" to AshType.MODIFIER)) { _, args ->
        booleanCurrent(args[0].toString())
    }
    regFn(scope, "string_modifier", AshType.STRING,
        listOf("modifier" to AshType.MODIFIER)) { _, args ->
        stringCurrent(args[0].toString())
    }
    regFn(scope, "numerics_modifier", AggregateType(AshType.INT, AshType.FLOAT),
        listOf("modifier" to AshType.MODIFIER)) { _, args ->
        currentMultiModifier(args[0].toString())
    }
    regFn(scope, "strings_modifier", AggregateType(AshType.INT, AshType.STRING),
        listOf("modifier" to AshType.MODIFIER)) { _, args ->
        currentMultiStringModifier(args[0].toString())
    }

    regFn(scope, "numerics_modifier", AggregateType(AshType.INT, AshType.FLOAT),
        listOf("typeName" to AshType.STRING, "modifier" to AshType.STRING)) { _, args ->
        staticNumerics(ModifierDatabase.get(parseTypeName(args[0].toString()).first,
            parseTypeName(args[0].toString()).second), args[1].toString())
    }
    regFn(scope, "numerics_modifier", AggregateType(AshType.INT, AshType.FLOAT),
        listOf("typeName" to AshType.STRING, "modifier" to AshType.MODIFIER)) { _, args ->
        staticNumerics(ModifierDatabase.get(parseTypeName(args[0].toString()).first,
            parseTypeName(args[0].toString()).second), args[1].toString())
    }
    regFn(scope, "numerics_modifier", AggregateType(AshType.INT, AshType.FLOAT),
        listOf("it" to AshType.ITEM, "modifier" to AshType.STRING)) { _, args ->
        staticNumerics(staticModifierEntry(AshType.ITEM, args[0].toString()), args[1].toString())
    }
    regFn(scope, "numerics_modifier", AggregateType(AshType.INT, AshType.FLOAT),
        listOf("it" to AshType.ITEM, "modifier" to AshType.MODIFIER)) { _, args ->
        staticNumerics(staticModifierEntry(AshType.ITEM, args[0].toString()), args[1].toString())
    }

    val entityTypes = listOf(
        AshType.ITEM to "it",
        AshType.EFFECT to "ef",
        AshType.SKILL to "sk",
        AshType.FAMILIAR to "fa",
    )
    for ((type, parameter) in entityTypes) {
        val captured = type
        val entry = { name: String -> staticModifierEntry(captured, name) }
        regFn(scope, "numeric_modifier", AshType.FLOAT,
            listOf(parameter to captured, "modifier" to AshType.MODIFIER)) { _, args ->
            numericStatic(entry(args[0].toString()), args[1].toString())
        }
        regFn(scope, "boolean_modifier", AshType.BOOLEAN,
            listOf(parameter to captured, "modifier" to AshType.MODIFIER)) { _, args ->
            booleanStatic(entry(args[0].toString()), args[1].toString())
        }
        regFn(scope, "string_modifier", AshType.STRING,
            listOf(parameter to captured, "modifier" to AshType.MODIFIER)) { _, args ->
            stringStatic(entry(args[0].toString()), args[1].toString())
        }
        regFn(scope, "strings_modifier", AggregateType(AshType.INT, AshType.STRING),
            listOf(parameter to captured, "modifier" to AshType.MODIFIER)) { _, args ->
            staticStrings(entry(args[0].toString()), args[1].toString())
        }
    }

    val itemType = AshType.ITEM
    regFn(scope, "effects_modifier", AggregateType(AshType.INT, AshType.EFFECT),
        listOf("it" to itemType, "modifier" to AshType.STRING)) { _, args ->
        staticEntities(staticModifierEntry(itemType, args[0].toString()), args[1].toString(), AshType.EFFECT)
    }
    regFn(scope, "effects_modifier", AggregateType(AshType.INT, AshType.EFFECT),
        listOf("it" to itemType, "modifier" to AshType.MODIFIER)) { _, args ->
        staticEntities(staticModifierEntry(itemType, args[0].toString()), args[1].toString(), AshType.EFFECT)
    }
    regFn(scope, "skills_modifier", AggregateType(AshType.INT, AshType.SKILL),
        listOf("it" to itemType, "modifier" to AshType.STRING)) { _, args ->
        staticEntities(staticModifierEntry(itemType, args[0].toString()), args[1].toString(), AshType.SKILL)
    }
    regFn(scope, "skills_modifier", AggregateType(AshType.INT, AshType.SKILL),
        listOf("it" to itemType, "modifier" to AshType.MODIFIER)) { _, args ->
        staticEntities(staticModifierEntry(itemType, args[0].toString()), args[1].toString(), AshType.SKILL)
    }

    for ((name, resultType) in listOf(
        "class_modifier" to AshType.CLASS,
        "skill_modifier" to AshType.SKILL,
        "stat_modifier" to AshType.STAT,
    )) {
        regFn(scope, name, resultType,
            listOf("lookup" to AshType.STRING, "modifier" to AshType.MODIFIER)) { _, args ->
            modifierEntity(resultType, phaseResolveModifierEntry(args[0].toString()), args[1].toString())
        }
    }
    regFn(scope, "class_modifier", AshType.CLASS,
        listOf("it" to AshType.ITEM, "modifier" to AshType.MODIFIER)) { _, args ->
        modifierEntity(AshType.CLASS, staticModifierEntry(AshType.ITEM, args[0].toString()), args[1].toString())
    }
}

private fun GameRuntimeLibrary.numericCurrent(tag: String): AshValue {
    val modifier = DoubleModifier.byTag(tag) ?: return AshValue.of(0.0)
    return AshValue.of(buildCurrentModifiers().values.get(modifier))
}

private fun GameRuntimeLibrary.booleanCurrent(tag: String): AshValue {
    val modifier = BooleanModifier.byTag(tag) ?: return AshValue.FALSE
    return AshValue.of(buildCurrentModifiers().values.get(modifier))
}

private fun GameRuntimeLibrary.stringCurrent(tag: String): AshValue {
    val modifier = StringModifier.byTag(tag) ?: return AshValue.EMPTY_STRING
    return AshValue.of(buildCurrentModifiers().values.get(modifier).orEmpty())
}

private fun GameRuntimeLibrary.currentMultiModifier(tag: String): AggregateValue {
    val modifier = DoubleModifier.byTag(tag)
        ?.takeIf { it.multiple }
        ?: return buildEmptyFloatAggregate()
    return buildFloatAggregate(currentMultiDoubleValues(modifier))
}

private fun GameRuntimeLibrary.currentMultiStringModifier(tag: String): AggregateValue {
    val modifier = StringModifier.byTag(tag)
        ?.takeIf { it.multiple }
        ?: return AggregateValue(AggregateType(AshType.INT, AshType.STRING))
    val result = AggregateValue(AggregateType(AshType.INT, AshType.STRING))
    buildCurrentModifiers().values.getAll(modifier).forEachIndexed { index, value ->
        result[AshValue.of(index)] = AshValue.of(value)
    }
    return result
}

private fun GameRuntimeLibrary.staticModifierEntry(type: AshType, name: String): ModifierEntry? =
    when (type) {
        AshType.ITEM -> gameDatabase?.itemModifier(name) ?: ModifierDatabase.getItem(name)
        AshType.EFFECT -> gameDatabase?.effectModifier(name) ?: ModifierDatabase.getEffect(name)
        AshType.SKILL -> gameDatabase?.skillModifier(name) ?: ModifierDatabase.getSkill(name)
        AshType.FAMILIAR -> gameDatabase?.familiarModifier(name) ?: ModifierDatabase.getFamiliar(name)
        else -> null
    }

private fun numericStatic(entry: ModifierEntry?, tag: String): AshValue =
    AshValue.of(numericFromEntry(entry, tag))

private fun booleanStatic(entry: ModifierEntry?, tag: String): AshValue =
    AshValue.of(booleanFromEntry(entry, tag))

private fun stringStatic(entry: ModifierEntry?, tag: String): AshValue =
    AshValue.of(stringFromEntry(entry, tag))

private fun staticStrings(entry: ModifierEntry?, tag: String): AggregateValue {
    val result = AggregateValue(AggregateType(AshType.INT, AshType.STRING))
    val modifier = StringModifier.byTag(tag) ?: return result
    ModifierParser.parse(entry?.modifiers.orEmpty()).getAll(modifier)
        .forEachIndexed { index, value -> result[AshValue.of(index)] = AshValue.of(value) }
    return result
}

private fun staticNumerics(entry: ModifierEntry?, tag: String): AggregateValue {
    val result = buildEmptyFloatAggregate()
    val modifier = DoubleModifier.byTag(tag)?.takeIf { it.multiple } ?: return result
    val value = ModifierParser.parse(entry?.modifiers.orEmpty()).get(modifier)
    return if (value == 0.0) result else buildFloatAggregate(listOf(value))
}

private fun staticEntities(entry: ModifierEntry?, tag: String, type: AshType): AggregateValue {
    val result = AggregateValue(AggregateType(AshType.INT, type))
    val modifier = StringModifier.byTag(tag) ?: return result
    ModifierParser.parse(entry?.modifiers.orEmpty()).getAll(modifier)
        .forEachIndexed { index, value ->
            result[AshValue.of(index)] = AshValue(type, value)
        }
    return result
}

private fun phaseResolveModifierEntry(lookup: String): ModifierEntry? {
    val colon = lookup.indexOf(':')
    return if (colon > 0) {
        ModifierDatabase.get(lookup.substring(0, colon).trim(), lookup.substring(colon + 1).trim())
    } else {
        ModifierDatabase.getItem(lookup)
            ?: ModifierDatabase.getEffect(lookup)
            ?: ModifierDatabase.getSkill(lookup)
            ?: ModifierDatabase.getFamiliar(lookup)
    }
}

private fun modifierEntity(type: AshType, entry: ModifierEntry?, tag: String): AshValue {
    val value = stringFromEntry(entry, tag)
    return AshValue(type, value)
}
