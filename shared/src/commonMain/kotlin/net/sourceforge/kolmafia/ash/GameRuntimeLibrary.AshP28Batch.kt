package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.modifiers.ModifierNames
import net.sourceforge.kolmafia.shop.CoinmasterRegistry

/**
 * ASH-P28 behavioral batch — COINMASTER/MODIFIER entity validation + ModifierDatabase lookups.
 */
internal fun GameRuntimeLibrary.registerAshP28Batch(scope: AshScope) {
    val coinmasterModifierParams = listOf("value" to AshType.COINMASTER, "modifier" to AshType.STRING)
    regFn(scope, "numeric_modifier", AshType.FLOAT, coinmasterModifierParams) { _, args ->
        val entry = ModifierDatabase.get("Coinmaster", args[0].toString())
        AshValue.of(numericFromEntry(entry, args[1].toString()))
    }
    regFn(scope, "boolean_modifier", AshType.BOOLEAN, coinmasterModifierParams) { _, args ->
        val entry = ModifierDatabase.get("Coinmaster", args[0].toString())
        AshValue.of(booleanFromEntry(entry, args[1].toString()))
    }
    regFn(scope, "string_modifier", AshType.STRING, coinmasterModifierParams) { _, args ->
        val entry = ModifierDatabase.get("Coinmaster", args[0].toString())
        AshValue.of(stringFromEntry(entry, args[1].toString()))
    }
    regFn(scope, "type_of", AshType.STRING, listOf("value" to AshType.COINMASTER)) { _, _ ->
        AshValue.of(AshType.COINMASTER.name)
    }
    regFn(scope, "is_valid", AshType.BOOLEAN, listOf("value" to AshType.COINMASTER)) { _, args ->
        AshValue.of(CoinmasterRegistry.isValid(args[0].toString()))
    }

    // $modifier entity as subject — look up Modifier-type rows by the modifier's own name
    val modifierModifierParams = listOf("value" to AshType.MODIFIER, "modifier" to AshType.STRING)
    regFn(scope, "numeric_modifier", AshType.FLOAT, modifierModifierParams) { _, args ->
        val entry = ModifierDatabase.get("Modifier", args[0].toString())
        AshValue.of(numericFromEntry(entry, args[1].toString()))
    }
    regFn(scope, "boolean_modifier", AshType.BOOLEAN, modifierModifierParams) { _, args ->
        val entry = ModifierDatabase.get("Modifier", args[0].toString())
        AshValue.of(booleanFromEntry(entry, args[1].toString()))
    }
    regFn(scope, "string_modifier", AshType.STRING, modifierModifierParams) { _, args ->
        val entry = ModifierDatabase.get("Modifier", args[0].toString())
        AshValue.of(stringFromEntry(entry, args[1].toString()))
    }
    regFn(scope, "type_of", AshType.STRING, listOf("value" to AshType.MODIFIER)) { _, _ ->
        AshValue.of(AshType.MODIFIER.name)
    }
    regFn(scope, "is_valid", AshType.BOOLEAN, listOf("value" to AshType.MODIFIER)) { _, args ->
        AshValue.of(ModifierNames.isValid(args[0].toString()))
    }
}
