package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ModifierEntry
import net.sourceforge.kolmafia.modifiers.ServantData
import net.sourceforge.kolmafia.modifiers.VykeaCompanionData

/**
 * ASH-P26 behavioral batch — live SERVANT/VYKEA entity validation and VYKEA couch/lamp modifiers.
 */
internal fun GameRuntimeLibrary.registerAshP26Batch(scope: AshScope) {
    val servantModifierParams = listOf("servant" to AshType.SERVANT, "modifier" to AshType.STRING)
    regFn(scope, "numeric_modifier", AshType.FLOAT, servantModifierParams) { _, _ ->
        AshValue.of(0.0)
    }
    regFn(scope, "boolean_modifier", AshType.BOOLEAN, servantModifierParams) { _, _ ->
        AshValue.FALSE
    }
    regFn(scope, "string_modifier", AshType.STRING, servantModifierParams) { _, _ ->
        AshValue.EMPTY_STRING
    }
    regFn(scope, "type_of", AshType.STRING, listOf("servant" to AshType.SERVANT)) { _, _ ->
        AshValue.of(AshType.SERVANT.name)
    }
    regFn(scope, "is_valid", AshType.BOOLEAN, listOf("servant" to AshType.SERVANT)) { _, args ->
        AshValue.of(ServantData.isValid(args[0].toString()))
    }

    val vykeaModifierParams = listOf("vykea" to AshType.VYKEA, "modifier" to AshType.STRING)
    regFn(scope, "numeric_modifier", AshType.FLOAT, vykeaModifierParams) { _, args ->
        val entry = resolveVykeaModifierEntry(args[0].toString())
        AshValue.of(numericFromEntry(entry, args[1].toString()))
    }
    regFn(scope, "boolean_modifier", AshType.BOOLEAN, vykeaModifierParams) { _, _ ->
        AshValue.FALSE
    }
    regFn(scope, "string_modifier", AshType.STRING, vykeaModifierParams) { _, args ->
        val entry = resolveVykeaModifierEntry(args[0].toString())
        AshValue.of(stringFromEntry(entry, args[1].toString()))
    }
    regFn(scope, "type_of", AshType.STRING, listOf("vykea" to AshType.VYKEA)) { _, _ ->
        AshValue.of(AshType.VYKEA.name)
    }
    regFn(scope, "is_valid", AshType.BOOLEAN, listOf("vykea" to AshType.VYKEA)) { _, args ->
        AshValue.of(VykeaCompanionData.isValid(args[0].toString()))
    }

    regFn(scope, "have_servant", AshType.BOOLEAN, listOf("servant" to AshType.SERVANT)) { _, args ->
        AshValue.of(ServantData.resolve(args[0].toString()) != null)
    }
}

internal fun GameRuntimeLibrary.resolveVykeaModifierEntry(vykeaName: String): ModifierEntry? {
    val companion = VykeaCompanionData.companionFor(vykeaName) ?: return null
    val mods = companion.modifiers
    if (mods.isEmpty()) return null
    return ModifierEntry("VYKEA", vykeaName, mods)
}
