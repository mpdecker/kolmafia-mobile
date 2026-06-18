package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.modifiers.SlotNames

internal fun GameRuntimeLibrary.registerEquipmentQueries(scope: AshScope) {

    fun resolveSlot(slotName: String): String? {
        val slot = SlotNames.toEquipmentSlot(slotName) ?: return null
        val itemName = character?.state?.value?.equipment?.get(slot)
        return if (itemName.isNullOrBlank()) null else itemName
    }

    fun slotForItem(itemRef: String): String {
        val db = gameDatabase ?: return ""
        val item = itemRef.toIntOrNull()?.let { db.item(it) } ?: db.item(itemRef)
            ?: return ""
        return when (item.primaryUse) {
            ItemPrimaryUse.HAT -> "hat"
            ItemPrimaryUse.WEAPON -> "weapon"
            ItemPrimaryUse.SIXGUN -> "holster"
            ItemPrimaryUse.OFFHAND -> "off-hand"
            ItemPrimaryUse.CONTAINER -> "container"
            ItemPrimaryUse.SHIRT -> "shirt"
            ItemPrimaryUse.PANTS -> "pants"
            ItemPrimaryUse.ACCESSORY -> "acc1"
            ItemPrimaryUse.FAMILIAR -> "familiar"
            else -> ""
        }
    }

    regFn(scope, "equipped_item", AshType.ITEM,
        listOf("slot" to AshType.SLOT)) { _, args ->
        AshValue.item(resolveSlot(args[0].toString()) ?: "none")
    }

    regFn(scope, "have_equipped", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM)) { _, args ->
        val name = args[0].toString()
        val has = character?.state?.value?.equipment?.values
            ?.any { it.equals(name, ignoreCase = true) } ?: false
        AshValue.of(has)
    }

    regFn(scope, "to_slot", AshType.SLOT,
        listOf("name" to AshType.STRING)) { _, args ->
        val resolved = SlotNames.resolve(args[0].toString())
        AshValue(AshType.SLOT, resolved ?: "")
    }

    regFn(scope, "to_slot", AshType.SLOT,
        listOf("it" to AshType.ITEM)) { _, args ->
        AshValue(AshType.SLOT, slotForItem(args[0].toString()))
    }

    regFn(scope, "slot_to_item", AshType.ITEM,
        listOf("slot" to AshType.SLOT)) { _, args ->
        AshValue.item(resolveSlot(args[0].toString()) ?: "none")
    }
}
