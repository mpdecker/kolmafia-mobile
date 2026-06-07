package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.modifiers.BooleanModifier
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ModifierParser
import net.sourceforge.kolmafia.modifiers.StringModifier

internal fun GameRuntimeLibrary.registerModifierQueries(scope: AshScope) {

    // ── numeric_modifier(item, string) → float ────────────────────────────────
    regFn(scope, "numeric_modifier", AshType.FLOAT,
        listOf("it" to AshType.ITEM, "modifier" to AshType.STRING)) { _, args ->
        val entry = gameDatabase?.itemModifier(args[0].toString())
        val dm = DoubleModifier.byTag(args[1].toString())
        val value = if (entry != null && dm != null)
            ModifierParser.parse(entry.modifiers).get(dm)
        else 0.0
        AshValue.of(value)
    }

    // ── numeric_modifier(effect, string) → float ──────────────────────────────
    regFn(scope, "numeric_modifier", AshType.FLOAT,
        listOf("ef" to AshType.EFFECT, "modifier" to AshType.STRING)) { _, args ->
        val entry = gameDatabase?.effectModifier(args[0].toString())
        val dm = DoubleModifier.byTag(args[1].toString())
        val value = if (entry != null && dm != null)
            ModifierParser.parse(entry.modifiers).get(dm)
        else 0.0
        AshValue.of(value)
    }

    // ── boolean_modifier(item, string) → boolean ──────────────────────────────
    regFn(scope, "boolean_modifier", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "modifier" to AshType.STRING)) { _, args ->
        val entry = gameDatabase?.itemModifier(args[0].toString())
        val bm = BooleanModifier.byTag(args[1].toString())
        val value = if (entry != null && bm != null)
            ModifierParser.parse(entry.modifiers).get(bm)
        else false
        AshValue.of(value)
    }

    // ── boolean_modifier(effect, string) → boolean ────────────────────────────
    regFn(scope, "boolean_modifier", AshType.BOOLEAN,
        listOf("ef" to AshType.EFFECT, "modifier" to AshType.STRING)) { _, args ->
        val entry = gameDatabase?.effectModifier(args[0].toString())
        val bm = BooleanModifier.byTag(args[1].toString())
        val value = if (entry != null && bm != null)
            ModifierParser.parse(entry.modifiers).get(bm)
        else false
        AshValue.of(value)
    }

    // ── string_modifier(item, string) → string ────────────────────────────────
    regFn(scope, "string_modifier", AshType.STRING,
        listOf("it" to AshType.ITEM, "modifier" to AshType.STRING)) { _, args ->
        val entry = gameDatabase?.itemModifier(args[0].toString())
        val sm = StringModifier.byTag(args[1].toString())
        val value = if (entry != null && sm != null)
            ModifierParser.parse(entry.modifiers).get(sm) ?: ""
        else ""
        AshValue.of(value)
    }

    // ── string_modifier(effect, string) → string ──────────────────────────────
    regFn(scope, "string_modifier", AshType.STRING,
        listOf("ef" to AshType.EFFECT, "modifier" to AshType.STRING)) { _, args ->
        val entry = gameDatabase?.effectModifier(args[0].toString())
        val sm = StringModifier.byTag(args[1].toString())
        val value = if (entry != null && sm != null)
            ModifierParser.parse(entry.modifiers).get(sm) ?: ""
        else ""
        AshValue.of(value)
    }
}
