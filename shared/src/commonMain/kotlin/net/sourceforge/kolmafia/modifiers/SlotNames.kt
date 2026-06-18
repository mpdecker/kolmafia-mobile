package net.sourceforge.kolmafia.modifiers

import net.sourceforge.kolmafia.character.EquipmentSlot

/**
 * ASH slot name catalog. Mirrors desktop [Slot] names for core equipment slots.
 */
object SlotNames {

    private val CANONICAL = listOf(
        "hat", "weapon", "holster", "off-hand", "container", "shirt", "pants",
        "acc1", "acc2", "acc3", "familiar",
    )

    private val ALIAS_TO_CANONICAL = mapOf(
        "offhand" to "off-hand",
        "back" to "container",
        "familiarequip" to "familiar",
        "accessory 1" to "acc1",
        "accessory 2" to "acc2",
        "accessory 3" to "acc3",
        "off hand" to "off-hand",
    )

    private val EQUIPMENT_SLOT_BY_CANONICAL = mapOf(
        "hat" to EquipmentSlot.HAT,
        "weapon" to EquipmentSlot.WEAPON,
        "off-hand" to EquipmentSlot.OFFHAND,
        "container" to EquipmentSlot.CONTAINER,
        "shirt" to EquipmentSlot.SHIRT,
        "pants" to EquipmentSlot.PANTS,
        "acc1" to EquipmentSlot.ACC1,
        "acc2" to EquipmentSlot.ACC2,
        "acc3" to EquipmentSlot.ACC3,
        "familiar" to EquipmentSlot.FAMILIAR,
    )

    fun resolve(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.equals("none", ignoreCase = true)) return null

        val lower = trimmed.lowercase()
        ALIAS_TO_CANONICAL[lower]?.let { return it }
        CANONICAL.firstOrNull { it.equals(trimmed, ignoreCase = true) }?.let { return it }
        return null
    }

    fun isValid(name: String): Boolean = resolve(name) != null

    fun toEquipmentSlot(canonicalName: String): EquipmentSlot? {
        val resolved = resolve(canonicalName) ?: return null
        return EQUIPMENT_SLOT_BY_CANONICAL[resolved.lowercase()]
    }
}
