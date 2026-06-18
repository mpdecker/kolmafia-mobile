package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.BountyDatabase
import net.sourceforge.kolmafia.modifiers.PhylumNames
import net.sourceforge.kolmafia.modifiers.SlotNames

/**
 * ASH-P27 behavioral batch — live BOUNTY/SLOT/PHYLUM entity validation and modifier no-ops.
 */
internal fun GameRuntimeLibrary.registerAshP27Batch(scope: AshScope) {
    val bountyModifierParams = listOf("bounty" to AshType.BOUNTY, "modifier" to AshType.STRING)
    regFn(scope, "numeric_modifier", AshType.FLOAT, bountyModifierParams) { _, _ ->
        AshValue.of(0.0)
    }
    regFn(scope, "boolean_modifier", AshType.BOOLEAN, bountyModifierParams) { _, _ ->
        AshValue.FALSE
    }
    regFn(scope, "string_modifier", AshType.STRING, bountyModifierParams) { _, _ ->
        AshValue.EMPTY_STRING
    }
    regFn(scope, "type_of", AshType.STRING, listOf("bounty" to AshType.BOUNTY)) { _, _ ->
        AshValue.of(AshType.BOUNTY.name)
    }
    regFn(scope, "is_valid", AshType.BOOLEAN, listOf("bounty" to AshType.BOUNTY)) { _, args ->
        AshValue.of(BountyDatabase.isValid(args[0].toString()))
    }

    val slotModifierParams = listOf("slot" to AshType.SLOT, "modifier" to AshType.STRING)
    regFn(scope, "numeric_modifier", AshType.FLOAT, slotModifierParams) { _, _ ->
        AshValue.of(0.0)
    }
    regFn(scope, "boolean_modifier", AshType.BOOLEAN, slotModifierParams) { _, _ ->
        AshValue.FALSE
    }
    regFn(scope, "string_modifier", AshType.STRING, slotModifierParams) { _, _ ->
        AshValue.EMPTY_STRING
    }
    regFn(scope, "type_of", AshType.STRING, listOf("slot" to AshType.SLOT)) { _, _ ->
        AshValue.of(AshType.SLOT.name)
    }
    regFn(scope, "is_valid", AshType.BOOLEAN, listOf("slot" to AshType.SLOT)) { _, args ->
        AshValue.of(SlotNames.isValid(args[0].toString()))
    }

    val phylumModifierParams = listOf("phylum" to AshType.PHYLUM, "modifier" to AshType.STRING)
    regFn(scope, "numeric_modifier", AshType.FLOAT, phylumModifierParams) { _, _ ->
        AshValue.of(0.0)
    }
    regFn(scope, "boolean_modifier", AshType.BOOLEAN, phylumModifierParams) { _, _ ->
        AshValue.FALSE
    }
    regFn(scope, "string_modifier", AshType.STRING, phylumModifierParams) { _, _ ->
        AshValue.EMPTY_STRING
    }
    regFn(scope, "type_of", AshType.STRING, listOf("phylum" to AshType.PHYLUM)) { _, _ ->
        AshValue.of(AshType.PHYLUM.name)
    }
    regFn(scope, "is_valid", AshType.BOOLEAN, listOf("phylum" to AshType.PHYLUM)) { _, args ->
        AshValue.of(PhylumNames.isValid(args[0].toString()))
    }
}
