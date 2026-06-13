package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ModifierEntry
import net.sourceforge.kolmafia.modifiers.BooleanModifier
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ModifierParser
import net.sourceforge.kolmafia.modifiers.StringModifier

internal fun GameRuntimeLibrary.registerModifierQueries(scope: AshScope) {

    // ── numeric_modifier(item, string) → float ────────────────────────────────
    regFn(scope, "numeric_modifier", AshType.FLOAT,
        listOf("it" to AshType.ITEM, "modifier" to AshType.STRING)) { _, args ->
        val itemRef = args[0].toString()
        val entry = gameDatabase?.itemModifier(itemRef)
            ?: itemRef.toIntOrNull()?.let { gameDatabase?.itemModifier(it) }
        AshValue.of(numericFromEntry(entry, args[1].toString()))
    }

    // ── numeric_modifier(effect, string) → float ──────────────────────────────
    regFn(scope, "numeric_modifier", AshType.FLOAT,
        listOf("ef" to AshType.EFFECT, "modifier" to AshType.STRING)) { _, args ->
        val entry = gameDatabase?.effectModifier(args[0].toString())
        AshValue.of(numericFromEntry(entry, args[1].toString()))
    }

    // ── numeric_modifier(skill, string) → float ───────────────────────────────
    regFn(scope, "numeric_modifier", AshType.FLOAT,
        listOf("sk" to AshType.SKILL, "modifier" to AshType.STRING)) { _, args ->
        val skillRef = args[0].toString()
        val entry = gameDatabase?.skillModifier(skillRef)
            ?: skillRef.toIntOrNull()?.let { gameDatabase?.skillModifier(it) }
        AshValue.of(numericFromEntry(entry, args[1].toString()))
    }

    // ── numeric_modifier(familiar, string) → float ────────────────────────────
    regFn(scope, "numeric_modifier", AshType.FLOAT,
        listOf("fa" to AshType.FAMILIAR, "modifier" to AshType.STRING)) { _, args ->
        val familiarRef = args[0].toString()
        val entry = gameDatabase?.familiarModifier(familiarRef)
            ?: familiarRef.toIntOrNull()?.let { gameDatabase?.familiarModifier(it) }
        AshValue.of(numericFromEntry(entry, args[1].toString()))
    }

    // ── boolean_modifier(item, string) → boolean ──────────────────────────────
    regFn(scope, "boolean_modifier", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "modifier" to AshType.STRING)) { _, args ->
        val entry = gameDatabase?.itemModifier(args[0].toString())
        AshValue.of(booleanFromEntry(entry, args[1].toString()))
    }

    // ── boolean_modifier(effect, string) → boolean ────────────────────────────
    regFn(scope, "boolean_modifier", AshType.BOOLEAN,
        listOf("ef" to AshType.EFFECT, "modifier" to AshType.STRING)) { _, args ->
        val entry = gameDatabase?.effectModifier(args[0].toString())
        AshValue.of(booleanFromEntry(entry, args[1].toString()))
    }

    // ── boolean_modifier(skill, string) → boolean ─────────────────────────────
    regFn(scope, "boolean_modifier", AshType.BOOLEAN,
        listOf("sk" to AshType.SKILL, "modifier" to AshType.STRING)) { _, args ->
        val skillRef = args[0].toString()
        val entry = gameDatabase?.skillModifier(skillRef)
            ?: skillRef.toIntOrNull()?.let { gameDatabase?.skillModifier(it) }
        AshValue.of(booleanFromEntry(entry, args[1].toString()))
    }

    // ── boolean_modifier(familiar, string) → boolean ──────────────────────────
    regFn(scope, "boolean_modifier", AshType.BOOLEAN,
        listOf("fa" to AshType.FAMILIAR, "modifier" to AshType.STRING)) { _, args ->
        val familiarRef = args[0].toString()
        val entry = gameDatabase?.familiarModifier(familiarRef)
            ?: familiarRef.toIntOrNull()?.let { gameDatabase?.familiarModifier(it) }
        AshValue.of(booleanFromEntry(entry, args[1].toString()))
    }

    // ── string_modifier(item, string) → string ────────────────────────────────
    regFn(scope, "string_modifier", AshType.STRING,
        listOf("it" to AshType.ITEM, "modifier" to AshType.STRING)) { _, args ->
        val entry = gameDatabase?.itemModifier(args[0].toString())
        AshValue.of(stringFromEntry(entry, args[1].toString()))
    }

    // ── string_modifier(effect, string) → string ──────────────────────────────
    regFn(scope, "string_modifier", AshType.STRING,
        listOf("ef" to AshType.EFFECT, "modifier" to AshType.STRING)) { _, args ->
        val entry = gameDatabase?.effectModifier(args[0].toString())
        AshValue.of(stringFromEntry(entry, args[1].toString()))
    }

    // ── string_modifier(skill, string) → string ───────────────────────────────
    regFn(scope, "string_modifier", AshType.STRING,
        listOf("sk" to AshType.SKILL, "modifier" to AshType.STRING)) { _, args ->
        val skillRef = args[0].toString()
        val entry = gameDatabase?.skillModifier(skillRef)
            ?: skillRef.toIntOrNull()?.let { gameDatabase?.skillModifier(it) }
        AshValue.of(stringFromEntry(entry, args[1].toString()))
    }

    // ── string_modifier(familiar, string) → string ────────────────────────────
    regFn(scope, "string_modifier", AshType.STRING,
        listOf("fa" to AshType.FAMILIAR, "modifier" to AshType.STRING)) { _, args ->
        val familiarRef = args[0].toString()
        val entry = gameDatabase?.familiarModifier(familiarRef)
            ?: familiarRef.toIntOrNull()?.let { gameDatabase?.familiarModifier(it) }
        AshValue.of(stringFromEntry(entry, args[1].toString()))
    }
}

private fun numericFromEntry(entry: ModifierEntry?, tag: String): Double {
    val dm = DoubleModifier.byTag(tag) ?: return 0.0
    return if (entry != null) ModifierParser.parse(entry.modifiers).get(dm) else 0.0
}

private fun booleanFromEntry(entry: ModifierEntry?, tag: String): Boolean {
    val bm = BooleanModifier.byTag(tag) ?: return false
    return if (entry != null) ModifierParser.parse(entry.modifiers).get(bm) else false
}

private fun stringFromEntry(entry: ModifierEntry?, tag: String): String {
    val sm = StringModifier.byTag(tag) ?: return ""
    return if (entry != null) ModifierParser.parse(entry.modifiers).get(sm) ?: "" else ""
}
