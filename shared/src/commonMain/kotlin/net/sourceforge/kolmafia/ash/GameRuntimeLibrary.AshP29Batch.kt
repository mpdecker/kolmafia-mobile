package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.modifiers.ClassNames
import net.sourceforge.kolmafia.modifiers.ElementNames

/**
 * ASH-P29 behavioral batch — CLASS (+ restored ELEMENT) entity validation +
 * ModifierDatabase CLASS lookups. ELEMENT modifier rows live in AshP23.
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
    // Phase 5086–5095 corpus polish: ELEMENT is_valid/type_of restored after AshP23 dedupe.
    regFn(scope, "type_of", AshType.STRING, listOf("element" to AshType.ELEMENT)) { _, _ ->
        AshValue.of(AshType.ELEMENT.name)
    }
    regFn(scope, "is_valid", AshType.BOOLEAN, listOf("element" to AshType.ELEMENT)) { _, args ->
        AshValue.of(ElementNames.isValid(args[0].toString()))
    }
}
