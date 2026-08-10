package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ItemDatabase

/** Preserves live sub-slot contents when scoring parent items (Phase 385). */
object MaximizerSubSlotPreservation {

    fun copySubSlotsFromBase(
        baseState: CharacterState,
        target: MutableMap<EquipmentSlot, Pair<String, Double>>,
    ) {
        for (slot in EquipmentSlot.SUB_SLOTS) {
            val name = baseState.equipment[slot]?.takeIf { it.isNotBlank() } ?: continue
            target[slot] = name to 0.0
        }
    }

    fun applyParentPreservation(
        itemId: Int,
        baseState: CharacterState,
        target: MutableMap<EquipmentSlot, Pair<String, Double>>,
    ) {
        when {
            MaximizerSubSlotItems.isFolderHolder(itemId) -> copyFolders(baseState, target)
            MaximizerSubSlotItems.isStickerWeapon(itemId) -> copyStickers(baseState, target)
            MaximizerSubSlotItems.isCowboyBoots(itemId) -> copyBootAttachments(baseState, target)
        }
    }

    private fun copyFolders(
        baseState: CharacterState,
        target: MutableMap<EquipmentSlot, Pair<String, Double>>,
    ) {
        for (slot in EquipmentSlot.folderSlotsFor(baseState.inKoLHS)) {
            val name = baseState.equipment[slot]?.takeIf { it.isNotBlank() } ?: continue
            target[slot] = name to 0.0
        }
    }

    private fun copyStickers(
        baseState: CharacterState,
        target: MutableMap<EquipmentSlot, Pair<String, Double>>,
    ) {
        for (slot in EquipmentSlot.STICKER_SLOTS) {
            val name = baseState.equipment[slot]?.takeIf { it.isNotBlank() } ?: continue
            target[slot] = name to 0.0
        }
    }

    private fun copyBootAttachments(
        baseState: CharacterState,
        target: MutableMap<EquipmentSlot, Pair<String, Double>>,
    ) {
        for (slot in EquipmentSlot.BOOT_SLOTS) {
            val name = baseState.equipment[slot]?.takeIf { it.isNotBlank() } ?: continue
            target[slot] = name to 0.0
        }
    }

    fun itemIdForName(name: String): Int? = ItemDatabase.getByName(name)?.id
}
