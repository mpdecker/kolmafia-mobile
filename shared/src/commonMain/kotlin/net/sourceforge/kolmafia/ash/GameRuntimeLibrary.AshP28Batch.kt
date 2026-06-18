package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.modifiers.ModifierNames
import net.sourceforge.kolmafia.shop.CoinmasterRegistry

/**
 * ASH-P28 behavioral batch — live COINMASTER/MODIFIER entity validation and modifier no-ops.
 */
internal fun GameRuntimeLibrary.registerAshP28Batch(scope: AshScope) {
    val coinmasterModifierParams = listOf("value" to AshType.COINMASTER, "modifier" to AshType.STRING)
    regFn(scope, "numeric_modifier", AshType.FLOAT, coinmasterModifierParams) { _, _ ->
        AshValue.of(0.0)
    }
    regFn(scope, "boolean_modifier", AshType.BOOLEAN, coinmasterModifierParams) { _, _ ->
        AshValue.FALSE
    }
    regFn(scope, "string_modifier", AshType.STRING, coinmasterModifierParams) { _, _ ->
        AshValue.EMPTY_STRING
    }
    regFn(scope, "type_of", AshType.STRING, listOf("value" to AshType.COINMASTER)) { _, _ ->
        AshValue.of(AshType.COINMASTER.name)
    }
    regFn(scope, "is_valid", AshType.BOOLEAN, listOf("value" to AshType.COINMASTER)) { _, args ->
        AshValue.of(CoinmasterRegistry.isValid(args[0].toString()))
    }

    val modifierModifierParams = listOf("value" to AshType.MODIFIER, "modifier" to AshType.STRING)
    regFn(scope, "numeric_modifier", AshType.FLOAT, modifierModifierParams) { _, _ ->
        AshValue.of(0.0)
    }
    regFn(scope, "boolean_modifier", AshType.BOOLEAN, modifierModifierParams) { _, _ ->
        AshValue.FALSE
    }
    regFn(scope, "string_modifier", AshType.STRING, modifierModifierParams) { _, _ ->
        AshValue.EMPTY_STRING
    }
    regFn(scope, "type_of", AshType.STRING, listOf("value" to AshType.MODIFIER)) { _, _ ->
        AshValue.of(AshType.MODIFIER.name)
    }
    regFn(scope, "is_valid", AshType.BOOLEAN, listOf("value" to AshType.MODIFIER)) { _, args ->
        AshValue.of(ModifierNames.isValid(args[0].toString()))
    }
}
