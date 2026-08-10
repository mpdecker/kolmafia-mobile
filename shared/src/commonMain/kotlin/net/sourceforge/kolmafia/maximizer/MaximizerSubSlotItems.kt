package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.shop.FolderHolderAccessibility

/** Desktop folder/sticker/cowboy-boot sub-slot parent item helpers (Phase 385). */
object MaximizerSubSlotItems {

    const val STICKER_SWORD = 3508
    const val STICKER_CROSSBOW = 3526
    const val COWBOY_BOOTS = 8850

    /** Desktop ItemPool.FOLDER_01 base: folder index N → item id 6618 + (N − 1). */
    private const val FOLDER_BASE_ID = 6618

    fun isFolderHolder(itemId: Int): Boolean =
        itemId == FolderHolderAccessibility.FOLDER_HOLDER ||
            itemId == FolderHolderAccessibility.REPLICA_FOLDER_HOLDER

    fun isStickerWeapon(itemId: Int): Boolean =
        itemId == STICKER_SWORD || itemId == STICKER_CROSSBOW

    fun isCowboyBoots(itemId: Int): Boolean = itemId == COWBOY_BOOTS

    fun needsSubSlotPreservation(itemId: Int): Boolean =
        isFolderHolder(itemId) || isStickerWeapon(itemId) || isCowboyBoots(itemId)

    fun folderItemIdFromIndex(index: Int): Int? {
        if (index <= 0) return null
        return FOLDER_BASE_ID + (index - 1)
    }

    /** Desktop Evaluator: skip folder holder 6617 unless on KoLHS path. */
    fun skipFolderHolderEnumeration(charState: CharacterState, itemId: Int): Boolean =
        itemId == FolderHolderAccessibility.FOLDER_HOLDER && !charState.inKoLHS

    fun hasFolderHolderEquipped(state: CharacterState): Boolean =
        state.equipment.values.any { name ->
            val id = net.sourceforge.kolmafia.data.ItemDatabase.getByName(name)?.id ?: return@any false
            isFolderHolder(id)
        }

    fun hasStickerWeaponEquipped(state: CharacterState): Boolean {
        val weaponId = state.equipment[EquipmentSlot.WEAPON]
            ?.let { net.sourceforge.kolmafia.data.ItemDatabase.getByName(it)?.id }
            ?: return false
        return isStickerWeapon(weaponId)
    }

    fun hasCowboyBootsEquipped(state: CharacterState): Boolean =
        EquipmentSlot.SEARCH_SLOTS.any { slot ->
            state.equipment[slot]?.let { name ->
                val id = net.sourceforge.kolmafia.data.ItemDatabase.getByName(name)?.id
                id != null && isCowboyBoots(id)
            } == true
        }
}
