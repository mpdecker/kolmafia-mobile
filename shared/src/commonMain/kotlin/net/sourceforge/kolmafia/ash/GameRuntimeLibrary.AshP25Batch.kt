package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.modifiers.StatNames

/**
 * ASH-P25 behavioral batch — live STAT entity validation and modifier no-ops.
 */
internal fun GameRuntimeLibrary.registerAshP25Batch(scope: AshScope) {
    val statModifierParams = listOf("stat" to AshType.STAT, "modifier" to AshType.STRING)
    regFn(scope, "numeric_modifier", AshType.FLOAT, statModifierParams) { _, _ ->
        AshValue.of(0.0)
    }
    regFn(scope, "boolean_modifier", AshType.BOOLEAN, statModifierParams) { _, _ ->
        AshValue.FALSE
    }
    regFn(scope, "string_modifier", AshType.STRING, statModifierParams) { _, _ ->
        AshValue.EMPTY_STRING
    }
    regFn(scope, "type_of", AshType.STRING, listOf("value" to AshType.STAT)) { _, _ ->
        AshValue.of(AshType.STAT.name)
    }
    regFn(scope, "is_valid", AshType.BOOLEAN, listOf("stat" to AshType.STAT)) { _, args ->
        AshValue.of(StatNames.isValid(args[0].toString()))
    }
}
