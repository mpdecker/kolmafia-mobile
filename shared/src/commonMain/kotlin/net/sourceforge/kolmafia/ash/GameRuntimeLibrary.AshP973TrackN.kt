package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.modifiers.ModifierParser
import net.sourceforge.kolmafia.modifiers.StringModifier

/**
 * AshP973–974 Track N — Modifier leftover functions.
 *
 * Phase 973: effect_modifier(string,string), effect_modifier(string,modifier),
 *            class_modifier(string,string), skill_modifier(string,string),
 *            stat_modifier(string,string) — generic "type:name" overloads
 * Phase 974: effects_modifier / strings_modifier / skills_modifier / split_modifiers
 */
internal fun GameRuntimeLibrary.registerAshP973TrackNBatch(scope: AshScope) {
    // ── Phase 973: Generic typed modifier lookups via "Type:Name" ─────
    regFn(scope, "effect_modifier", AshType.EFFECT,
        listOf("lookup" to AshType.STRING, "modifier" to AshType.STRING)) { _, args ->
        val lookup = args[0].toString()
        val modTag = args[1].toString()
        val entry = resolveModifierEntry(lookup) ?: return@regFn AshValue.effect("")
        AshValue.effect(resolveStringMod(entry, modTag))
    }

    regFn(scope, "effect_modifier", AshType.EFFECT,
        listOf("lookup" to AshType.STRING, "modifier" to AshType.MODIFIER)) { _, args ->
        val lookup = args[0].toString()
        val modTag = args[1].toString()
        val entry = resolveModifierEntry(lookup) ?: return@regFn AshValue.effect("")
        AshValue.effect(resolveStringMod(entry, modTag))
    }

    regFn(scope, "class_modifier", AshType.CLASS,
        listOf("lookup" to AshType.STRING, "modifier" to AshType.STRING)) { _, args ->
        val lookup = args[0].toString()
        val modTag = args[1].toString()
        val entry = resolveModifierEntry(lookup) ?: return@regFn AshValue(AshType.CLASS, "")
        AshValue(AshType.CLASS, resolveStringMod(entry, modTag))
    }

    regFn(scope, "skill_modifier", AshType.SKILL,
        listOf("lookup" to AshType.STRING, "modifier" to AshType.STRING)) { _, args ->
        val lookup = args[0].toString()
        val modTag = args[1].toString()
        val entry = resolveModifierEntry(lookup) ?: return@regFn AshValue.skill("")
        AshValue.skill(resolveStringMod(entry, modTag))
    }

    regFn(scope, "stat_modifier", AshType.STAT,
        listOf("lookup" to AshType.STRING, "modifier" to AshType.STRING)) { _, args ->
        val lookup = args[0].toString()
        val modTag = args[1].toString()
        val entry = resolveModifierEntry(lookup) ?: return@regFn AshValue(AshType.STAT, "")
        AshValue(AshType.STAT, resolveStringMod(entry, modTag))
    }

    // ── Phase 974: modifier aggregate lookups ────────────────────────
    val stringToEffect = AggregateType(AshType.STRING, AshType.EFFECT)
    regFn(scope, "effects_modifier", stringToEffect,
        listOf("lookup" to AshType.STRING, "modifier" to AshType.STRING)) { _, args ->
        val lookup = args[0].toString()
        val modTag = args[1].toString()
        val result = AggregateValue(stringToEffect)
        val entry = resolveModifierEntry(lookup) ?: return@regFn result
        val sm = StringModifier.byTag(modTag)
        if (sm != null) {
            val parsed = ModifierParser.parse(entry.modifiers)
            val values = parsed.getAll(sm)
            for ((i, v) in values.withIndex()) {
                result[AshValue.of(i.toString())] = AshValue.effect(v)
            }
        }
        result
    }

    val stringToString = AggregateType(AshType.STRING, AshType.STRING)
    regFn(scope, "strings_modifier", stringToString,
        listOf("lookup" to AshType.STRING, "modifier" to AshType.STRING)) { _, args ->
        val lookup = args[0].toString()
        val modTag = args[1].toString()
        val result = AggregateValue(stringToString)
        val entry = resolveModifierEntry(lookup) ?: return@regFn result
        val sm = StringModifier.byTag(modTag)
        if (sm != null) {
            val parsed = ModifierParser.parse(entry.modifiers)
            val values = parsed.getAll(sm)
            for ((i, v) in values.withIndex()) {
                result[AshValue.of(i.toString())] = AshValue.of(v)
            }
        }
        result
    }

    val stringToSkill = AggregateType(AshType.STRING, AshType.SKILL)
    regFn(scope, "skills_modifier", stringToSkill,
        listOf("lookup" to AshType.STRING, "modifier" to AshType.STRING)) { _, args ->
        val lookup = args[0].toString()
        val modTag = args[1].toString()
        val result = AggregateValue(stringToSkill)
        val entry = resolveModifierEntry(lookup) ?: return@regFn result
        val sm = StringModifier.byTag(modTag)
        if (sm != null) {
            val parsed = ModifierParser.parse(entry.modifiers)
            val values = parsed.getAll(sm)
            for ((i, v) in values.withIndex()) {
                result[AshValue.of(i.toString())] = AshValue.skill(v)
            }
        }
        result
    }

    regFn(scope, "split_modifiers", stringToString,
        listOf("modifiers" to AshType.STRING)) { _, args ->
        val raw = args[0].toString()
        val result = AggregateValue(stringToString)
        val parsed = ModifierParser.parse(raw)
        for ((mod, value) in parsed.doubles) {
            result[AshValue.of(mod.tag)] = AshValue.of(value.toString())
        }
        for ((mod, values) in parsed.strings) {
            result[AshValue.of(mod.tag)] = AshValue.of(values.firstOrNull().orEmpty())
        }
        result
    }
}

private fun resolveModifierEntry(lookup: String): net.sourceforge.kolmafia.data.ModifierEntry? {
    val colon = lookup.indexOf(':')
    return if (colon > 0) {
        val type = lookup.substring(0, colon).trim()
        val name = lookup.substring(colon + 1).trim()
        ModifierDatabase.get(type, name)
    } else {
        ModifierDatabase.getItem(lookup)
            ?: ModifierDatabase.getEffect(lookup)
            ?: ModifierDatabase.getSkill(lookup)
    }
}

private fun resolveStringMod(
    entry: net.sourceforge.kolmafia.data.ModifierEntry,
    modTag: String,
): String {
    val sm = StringModifier.byTag(modTag) ?: return ""
    val parsed = ModifierParser.parse(entry.modifiers)
    return parsed.strings[sm]?.firstOrNull().orEmpty()
}
