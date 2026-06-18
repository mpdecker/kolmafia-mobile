package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ModifierEntry
import net.sourceforge.kolmafia.modifiers.ClassModifiers

/**
 * ASH-P24 behavioral batch — live CLASS entity modifiers.
 */
internal fun GameRuntimeLibrary.registerAshP24Batch(scope: AshScope) {
    val classModifierParams = listOf("cls" to AshType.CLASS, "modifier" to AshType.STRING)
    regFn(scope, "numeric_modifier", AshType.FLOAT, classModifierParams) { _, args ->
        val entry = resolveClassModifierEntry(args[0].toString())
        AshValue.of(numericFromEntry(entry, args[1].toString()))
    }
    regFn(scope, "boolean_modifier", AshType.BOOLEAN, classModifierParams) { _, _ ->
        AshValue.FALSE
    }
    regFn(scope, "string_modifier", AshType.STRING, classModifierParams) { _, args ->
        val entry = resolveClassModifierEntry(args[0].toString())
        AshValue.of(stringFromEntry(entry, args[1].toString()))
    }
}

internal fun GameRuntimeLibrary.resolveClassModifierEntry(className: String): ModifierEntry? {
    val normalized = className.trim()
    if (normalized.isEmpty()) return null
    gameDatabase?.modifier("Class", normalized)?.let { return it }
    val mods = ClassModifiers.modifierString(normalized) ?: return null
    return ModifierEntry("Class", normalized, mods)
}
