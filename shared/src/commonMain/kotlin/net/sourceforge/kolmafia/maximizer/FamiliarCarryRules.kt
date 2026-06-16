package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse

/**
 * Familiars that wear or wield player equipment in the familiar slot.
 * Mirrors desktop Evaluator hatrack/scarecrow/hand/left-hand scoring.
 */
object FamiliarCarryRules {
    const val HATRACK_RACE = "Mad Hatrack"
    const val SCARECROW_RACE = "Fancypants Scarecrow"
    const val HAND_RACE = "Disembodied Hand"
    const val LEFT_HAND_RACE = "Left-Hand Man"

    /** Items that do not work in the Left-Hand Man familiar slot (desktop ItemPool). */
    private val LEFT_HAND_BLOCKED_ITEM_IDS = setOf(3680, 9133)

    fun carriedEquipmentSlots(race: String): List<EquipmentSlot> = when (race) {
        HATRACK_RACE -> listOf(EquipmentSlot.HAT)
        SCARECROW_RACE -> listOf(EquipmentSlot.PANTS)
        HAND_RACE -> listOf(EquipmentSlot.WEAPON, EquipmentSlot.OFFHAND)
        LEFT_HAND_RACE -> listOf(EquipmentSlot.OFFHAND)
        else -> emptyList()
    }

    fun carryRaces(spec: MaximizeSpec, resolvedFamiliar: String? = null): List<String> = buildList {
        resolvedFamiliar?.let { add(it) }
        addAll(spec.switchFamiliars)
    }.filter { carriedEquipmentSlots(it).isNotEmpty() }.distinct()

    fun canCarryItem(race: String, item: ItemData): Boolean {
        if (race == LEFT_HAND_RACE && item.id in LEFT_HAND_BLOCKED_ITEM_IDS) return false
        val slot = equipmentSlotFor(item) ?: return false
        return slot in carriedEquipmentSlots(race)
    }

    private fun equipmentSlotFor(item: ItemData): EquipmentSlot? = when (item.primaryUse) {
        ItemPrimaryUse.HAT -> EquipmentSlot.HAT
        ItemPrimaryUse.WEAPON, ItemPrimaryUse.SIXGUN -> EquipmentSlot.WEAPON
        ItemPrimaryUse.OFFHAND -> EquipmentSlot.OFFHAND
        ItemPrimaryUse.SHIRT -> EquipmentSlot.SHIRT
        ItemPrimaryUse.PANTS -> EquipmentSlot.PANTS
        ItemPrimaryUse.ACCESSORY -> EquipmentSlot.ACC1
        ItemPrimaryUse.FAMILIAR -> EquipmentSlot.FAMILIAR
        else -> null
    }
}
