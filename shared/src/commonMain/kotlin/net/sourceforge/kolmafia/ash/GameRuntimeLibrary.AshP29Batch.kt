package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.modifiers.ClassNames
import net.sourceforge.kolmafia.modifiers.ElementNames

/**
 * ASH-P29 behavioral batch — live CLASS/ELEMENT entity validation and modifier no-ops.
 */
internal fun GameRuntimeLibrary.registerAshP29Batch(scope: AshScope) {
    val classModifierParams = listOf("cls" to AshType.CLASS, "modifier" to AshType.STRING)
    regFn(scope, "numeric_modifier", AshType.FLOAT, classModifierParams) { _, _ ->
        AshValue.of(0.0)
    }
    regFn(scope, "boolean_modifier", AshType.BOOLEAN, classModifierParams) { _, _ ->
        AshValue.FALSE
    }
    regFn(scope, "string_modifier", AshType.STRING, classModifierParams) { _, _ ->
        AshValue.EMPTY_STRING
    }
    regFn(scope, "type_of", AshType.STRING, listOf("cls" to AshType.CLASS)) { _, _ ->
        AshValue.of(AshType.CLASS.name)
    }
    regFn(scope, "is_valid", AshType.BOOLEAN, listOf("cls" to AshType.CLASS)) { _, args ->
        AshValue.of(ClassNames.isValid(args[0].toString()))
    }

    val elementModifierParams = listOf("element" to AshType.ELEMENT, "modifier" to AshType.STRING)
    regFn(scope, "numeric_modifier", AshType.FLOAT, elementModifierParams) { _, _ ->
        AshValue.of(0.0)
    }
    regFn(scope, "boolean_modifier", AshType.BOOLEAN, elementModifierParams) { _, _ ->
        AshValue.FALSE
    }
    regFn(scope, "string_modifier", AshType.STRING, elementModifierParams) { _, _ ->
        AshValue.EMPTY_STRING
    }
    regFn(scope, "type_of", AshType.STRING, listOf("element" to AshType.ELEMENT)) { _, _ ->
        AshValue.of(AshType.ELEMENT.name)
    }
    regFn(scope, "is_valid", AshType.BOOLEAN, listOf("element" to AshType.ELEMENT)) { _, args ->
        AshValue.of(ElementNames.isValid(args[0].toString()))
    }
}
