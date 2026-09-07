package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.modifiers.ClassNames

/**
 * ASH-P29 behavioral batch — CLASS entity validation + ModifierDatabase lookups.
 * ELEMENT modifiers live in AshP23 (resistance numeric + Element rows for bool/string).
 */
internal fun GameRuntimeLibrary.registerAshP29Batch(scope: AshScope) {
    val classModifierParams = listOf("cls" to AshType.CLASS, "modifier" to AshType.STRING)
    regFn(scope, "numeric_modifier", AshType.FLOAT, classModifierParams) { _, args ->
        AshValue.of(numericFromEntry(resolveClassModifierEntry(args[0].toString()), args[1].toString()))
    }
    regFn(scope, "boolean_modifier", AshType.BOOLEAN, classModifierParams) { _, args ->
        AshValue.of(booleanFromEntry(resolveClassModifierEntry(args[0].toString()), args[1].toString()))
    }
    regFn(scope, "string_modifier", AshType.STRING, classModifierParams) { _, args ->
        AshValue.of(stringFromEntry(resolveClassModifierEntry(args[0].toString()), args[1].toString()))
    }
    regFn(scope, "type_of", AshType.STRING, listOf("cls" to AshType.CLASS)) { _, _ ->
        AshValue.of(AshType.CLASS.name)
    }
    regFn(scope, "is_valid", AshType.BOOLEAN, listOf("cls" to AshType.CLASS)) { _, args ->
        AshValue.of(ClassNames.isValid(args[0].toString()))
    }
}
