package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.BountyDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.modifiers.PhylumNames
import net.sourceforge.kolmafia.modifiers.SlotNames

/**
 * ASH-P27 behavioral batch — BOUNTY/SLOT/PHYLUM entity validation + ModifierDatabase lookups.
 */
internal fun GameRuntimeLibrary.registerAshP27Batch(scope: AshScope) {
    val bountyModifierParams = listOf("bounty" to AshType.BOUNTY, "modifier" to AshType.STRING)
    regFn(scope, "numeric_modifier", AshType.FLOAT, bountyModifierParams) { _, args ->
        val entry = ModifierDatabase.get("Bounty", args[0].toString())
        AshValue.of(numericFromEntry(entry, args[1].toString()))
    }
    regFn(scope, "boolean_modifier", AshType.BOOLEAN, bountyModifierParams) { _, args ->
        val entry = ModifierDatabase.get("Bounty", args[0].toString())
        AshValue.of(booleanFromEntry(entry, args[1].toString()))
    }
    regFn(scope, "string_modifier", AshType.STRING, bountyModifierParams) { _, args ->
        val entry = ModifierDatabase.get("Bounty", args[0].toString())
        AshValue.of(stringFromEntry(entry, args[1].toString()))
    }
    regFn(scope, "type_of", AshType.STRING, listOf("bounty" to AshType.BOUNTY)) { _, _ ->
        AshValue.of(AshType.BOUNTY.name)
    }
    regFn(scope, "is_valid", AshType.BOOLEAN, listOf("bounty" to AshType.BOUNTY)) { _, args ->
        AshValue.of(BountyDatabase.isValid(args[0].toString()))
    }

    val slotModifierParams = listOf("slot" to AshType.SLOT, "modifier" to AshType.STRING)
    regFn(scope, "numeric_modifier", AshType.FLOAT, slotModifierParams) { _, args ->
        val entry = ModifierDatabase.get("Slot", args[0].toString())
        AshValue.of(numericFromEntry(entry, args[1].toString()))
    }
    regFn(scope, "boolean_modifier", AshType.BOOLEAN, slotModifierParams) { _, args ->
        val entry = ModifierDatabase.get("Slot", args[0].toString())
        AshValue.of(booleanFromEntry(entry, args[1].toString()))
    }
    regFn(scope, "string_modifier", AshType.STRING, slotModifierParams) { _, args ->
        val entry = ModifierDatabase.get("Slot", args[0].toString())
        AshValue.of(stringFromEntry(entry, args[1].toString()))
    }
    regFn(scope, "type_of", AshType.STRING, listOf("slot" to AshType.SLOT)) { _, _ ->
        AshValue.of(AshType.SLOT.name)
    }
    regFn(scope, "is_valid", AshType.BOOLEAN, listOf("slot" to AshType.SLOT)) { _, args ->
        AshValue.of(SlotNames.isValid(args[0].toString()))
    }

    val phylumModifierParams = listOf("phylum" to AshType.PHYLUM, "modifier" to AshType.STRING)
    regFn(scope, "numeric_modifier", AshType.FLOAT, phylumModifierParams) { _, args ->
        val entry = ModifierDatabase.get("Phylum", args[0].toString())
        AshValue.of(numericFromEntry(entry, args[1].toString()))
    }
    regFn(scope, "boolean_modifier", AshType.BOOLEAN, phylumModifierParams) { _, args ->
        val entry = ModifierDatabase.get("Phylum", args[0].toString())
        AshValue.of(booleanFromEntry(entry, args[1].toString()))
    }
    regFn(scope, "string_modifier", AshType.STRING, phylumModifierParams) { _, args ->
        val entry = ModifierDatabase.get("Phylum", args[0].toString())
        AshValue.of(stringFromEntry(entry, args[1].toString()))
    }
    regFn(scope, "type_of", AshType.STRING, listOf("phylum" to AshType.PHYLUM)) { _, _ ->
        AshValue.of(AshType.PHYLUM.name)
    }
    regFn(scope, "is_valid", AshType.BOOLEAN, listOf("phylum" to AshType.PHYLUM)) { _, args ->
        AshValue.of(PhylumNames.isValid(args[0].toString()))
    }
}
