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
            ItemPrimaryUse.STICKER -> "sticker1"
            ItemPrimaryUse.CARD -> "card-sleeve"
            ItemPrimaryUse.FOLDER -> "folder1"
            ItemPrimaryUse.BOOTSKIN -> "bootskin"
            ItemPrimaryUse.BOOTSPUR -> "bootspur"
            else -> ""
        }
    }

    fun hasEquippedAnywhere(name: String): Boolean {
        val equip = character?.state?.value?.equipment ?: return false
        val lower = name.lowercase()
        if (equip.values.any { it.equals(lower, ignoreCase = true) }) return true
        // Also match id-form equipment names when possible
        val item = gameDatabase?.item(name) ?: return false
        return equip.values.any { worn ->
            worn.equals(item.name, ignoreCase = true) ||
                worn.equals(item.id.toString(), ignoreCase = true)
        }
    }

    regFn(scope, "equipped_item", AshType.ITEM,
        listOf("slot" to AshType.SLOT)) { _, args ->
        AshValue.item(resolveSlot(args[0].toString()) ?: "none")
    }

    // Desktop KoLCharacter.hasEquipped — ACC1–3, stickers, folders, weapon/offhand
    regFn(scope, "have_equipped", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM)) { _, args ->
        AshValue.of(hasEquippedAnywhere(args[0].toString()))
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
