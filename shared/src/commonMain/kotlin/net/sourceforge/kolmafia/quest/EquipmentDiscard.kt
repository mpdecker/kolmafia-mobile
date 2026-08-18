package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ItemDatabase

/**
 * Desktop [EquipmentManager.discardEquipment]: clear the worn slot, then consume
 * the item only if it was equipped.
 */
object EquipmentDiscard {

    fun discardIfEquipped(
        itemId: Int,
        equipment: Map<EquipmentSlot, String>,
        itemName: (Int) -> String = { ItemDatabase.getItemName(it) },
        clearSlot: (EquipmentSlot) -> Unit,
        consumeItem: (Int, Int) -> Unit,
    ): Boolean {
        val name = itemName(itemId)
        if (name.isBlank()) return false
        val slot = equipment.entries.firstOrNull { it.value.equals(name, ignoreCase = true) }?.key
            ?: return false
        clearSlot(slot)
        consumeItem(itemId, 1)
        return true
    }
}
