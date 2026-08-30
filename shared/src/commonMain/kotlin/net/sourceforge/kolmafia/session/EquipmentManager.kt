package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.TorsoAwareness
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.modifiers.BooleanModifier
import net.sourceforge.kolmafia.quest.EquipmentDiscard
import net.sourceforge.kolmafia.session.YouRobotManager
import net.sourceforge.kolmafia.skill.SkillManager

/**
 * Desktop [EquipmentManager] hub (Phases 2091–2150): slot mutations, canEquip,
 * autoequip, discard/break/transform. Wraps [KoLCharacter] equipment map.
 */
class EquipmentManager(
    private val character: KoLCharacter,
    private val inventoryManager: InventoryManager? = null,
    private val skillManager: SkillManager? = null,
) {
    fun getEquipment(slot: EquipmentSlot): String =
        character.state.value.equipment[slot].orEmpty()

    fun getEquipmentId(slot: EquipmentSlot): Int {
        val name = getEquipment(slot)
        if (name.isBlank()) return -1
        return ItemDatabase.getByName(name)?.id ?: -1
    }

    fun hasEquipped(itemId: Int): Boolean {
        val name = ItemDatabase.getItemName(itemId)
        if (name.isBlank()) return false
        return character.state.value.equipment.values.any { it.equals(name, ignoreCase = true) }
    }

    fun findSlot(itemId: Int): EquipmentSlot? {
        val name = ItemDatabase.getItemName(itemId)
        if (name.isBlank()) return null
        return character.state.value.equipment.entries
            .firstOrNull { it.value.equals(name, ignoreCase = true) }
            ?.key
    }

    /** Desktop [EquipmentManager.itemIdToEquipmentType]. */
    fun itemIdToEquipmentType(itemId: Int): EquipmentSlot? =
        when (ItemDatabase.getById(itemId)?.primaryUse) {
            ItemPrimaryUse.HAT -> EquipmentSlot.HAT
            ItemPrimaryUse.WEAPON -> EquipmentSlot.WEAPON
            ItemPrimaryUse.SIXGUN -> EquipmentSlot.HOLSTER
            ItemPrimaryUse.OFFHAND -> EquipmentSlot.OFFHAND
            ItemPrimaryUse.SHIRT -> EquipmentSlot.SHIRT
            ItemPrimaryUse.PANTS -> EquipmentSlot.PANTS
            ItemPrimaryUse.ACCESSORY -> emptyAccessorySlot()
            ItemPrimaryUse.CONTAINER -> EquipmentSlot.CONTAINER
            ItemPrimaryUse.FAMILIAR -> EquipmentSlot.FAMILIAR
            ItemPrimaryUse.CARD -> EquipmentSlot.CARDSLEEVE
            ItemPrimaryUse.STICKER -> emptyStickerSlot()
            ItemPrimaryUse.FOLDER -> emptyFolderSlot()
            ItemPrimaryUse.BOOTSKIN -> EquipmentSlot.BOOTSKIN
            ItemPrimaryUse.BOOTSPUR -> EquipmentSlot.BOOTSPUR
            else -> null
        }

    private fun emptyAccessorySlot(): EquipmentSlot {
        val eq = character.state.value.equipment
        return listOf(EquipmentSlot.ACC1, EquipmentSlot.ACC2, EquipmentSlot.ACC3)
            .firstOrNull { eq[it].isNullOrBlank() }
            ?: EquipmentSlot.ACC1
    }

    private fun emptyStickerSlot(): EquipmentSlot {
        val eq = character.state.value.equipment
        return EquipmentSlot.STICKER_SLOTS.firstOrNull { eq[it].isNullOrBlank() }
            ?: EquipmentSlot.STICKER1
    }

    private fun emptyFolderSlot(): EquipmentSlot {
        val eq = character.state.value.equipment
        val slots = EquipmentSlot.folderSlotsFor(character.state.value.inKoLHS)
        return slots.firstOrNull { eq[it].isNullOrBlank() } ?: EquipmentSlot.FOLDER1
    }

    /**
     * Desktop [EquipmentManager.setEquipment] — update slot; swap inventory when
     * [swapInventory] and [inventoryManager] are available.
     */
    fun setEquipment(
        slot: EquipmentSlot,
        itemId: Int,
        swapInventory: Boolean = true,
    ) {
        val newName = when {
            itemId <= 0 -> ""
            else -> ItemDatabase.getItemName(itemId).ifBlank { "#$itemId" }
        }
        val oldName = getEquipment(slot)
        if (newName.equals(oldName, ignoreCase = true)) return

        if (swapInventory && inventoryManager != null) {
            if (itemId > 0) {
                inventoryManager.consumeItemLocally(itemId, 1)
            }
            if (oldName.isNotBlank()) {
                ItemDatabase.getByName(oldName)?.id?.let { oldId ->
                    inventoryManager.gainItemLocally(oldId, 1)
                }
            }
        }

        character.updateEquipment(slot, newName)
    }

    fun removeEquipment(slot: EquipmentSlot, swapInventory: Boolean = true) {
        setEquipment(slot, -1, swapInventory)
    }

    /** Desktop [EquipmentManager.autoequipItem]. */
    fun autoequipItem(itemId: Int, swapInventory: Boolean = true) {
        if (itemId <= 0) return
        val slot = itemIdToEquipmentType(itemId) ?: return
        if (getEquipmentId(slot) == itemId) return
        setEquipment(slot, itemId, swapInventory)
    }

    fun autoequipItemByName(itemName: String, swapInventory: Boolean = true) {
        val id = ItemDatabase.getByName(itemName)?.id ?: return
        autoequipItem(id, swapInventory)
    }

    /** Desktop [EquipmentManager.discardEquipment] via [EquipmentDiscard]. */
    fun discardEquipment(itemId: Int): EquipmentSlot? {
        var discarded: EquipmentSlot? = null
        EquipmentDiscard.discardIfEquipped(
            itemId = itemId,
            equipment = character.state.value.equipment,
            clearSlot = { slot ->
                discarded = slot
                character.updateEquipment(slot, "")
            },
            consumeItem = { id, qty -> inventoryManager?.consumeItemLocally(id, qty) },
        )
        return discarded
    }

    /** Soft break: unequip without returning to inventory. */
    fun breakEquipment(itemId: Int): EquipmentSlot? {
        val slot = findSlot(itemId) ?: return null
        character.updateEquipment(slot, "")
        inventoryManager?.consumeItemLocally(itemId, 1)
        return slot
    }

    /** Transform: replace equipped item id with [toItemId] in the same slot. */
    fun transformEquipment(fromItemId: Int, toItemId: Int): Boolean {
        val slot = findSlot(fromItemId) ?: return false
        character.updateEquipment(slot, ItemDatabase.getItemName(toItemId))
        inventoryManager?.consumeItemLocally(fromItemId, 1)
        return true
    }

    fun canEquip(itemId: Int): Boolean {
        if (itemId <= 0) return false
        val item = ItemDatabase.getById(itemId) ?: return false
        val state = character.state.value

        when (item.primaryUse) {
            ItemPrimaryUse.SIXGUN ->
                if (state.ascensionPath != AscensionPath.AVATAR_OF_WEST_OF_LOATHING) return false
            ItemPrimaryUse.SHIRT -> {
                val skills = skillManager?.state?.value?.skills.orEmpty()
                if (!TorsoAwareness.hasTorsoAwareness(skills) &&
                    !(state.inRobocore && YouRobotManager.hasEquipped(YouRobotManager.RobotUpgrade.TOPOLOGY_GRID))
                ) {
                    return false
                }
            }
            else -> Unit
        }

        if (state.inRobocore) {
            val skills = skillManager?.state?.value?.skills.orEmpty()
            val hasTorso = TorsoAwareness.hasTorsoAwareness(skills) ||
                YouRobotManager.hasEquipped(YouRobotManager.RobotUpgrade.TOPOLOGY_GRID)
            if (!YouRobotManager.canEquip(item.primaryUse, hasTorso)) return false
        }

        if (state.isFistcore &&
            (item.primaryUse == ItemPrimaryUse.WEAPON || item.primaryUse == ItemPrimaryUse.OFFHAND)
        ) {
            return false
        }
        if (state.isAxecore &&
            (item.primaryUse == ItemPrimaryUse.WEAPON || item.primaryUse == ItemPrimaryUse.OFFHAND)
        ) {
            return itemId == TRUSTY
        }
        if (state.isHardcore &&
            ModifierDatabase.hasBooleanModifier(item.name, BooleanModifier.SOFTCORE)
        ) {
            return false
        }
        return meetsStatRequirements(itemId, state)
    }

    fun canEquip(itemName: String): Boolean {
        val id = ItemDatabase.getByName(itemName)?.id ?: return false
        return canEquip(id)
    }

    fun meetsStatRequirements(itemId: Int, state: CharacterState = character.state.value): Boolean {
        val req = EquipmentDatabase.getByItemId(itemId)?.statRequirement ?: return true
        val amount = req.substringAfter(':').trim().toIntOrNull() ?: return true
        return when {
            req.startsWith("Mus", ignoreCase = true) -> state.baseMusc >= amount
            req.startsWith("Mys", ignoreCase = true) -> state.baseMyst >= amount
            req.startsWith("Mox", ignoreCase = true) -> state.baseMoxie >= amount
            else -> true
        }
    }

    fun usingTwoWeapons(): Boolean {
        val offId = getEquipmentId(EquipmentSlot.OFFHAND)
        return offId > 0 && EquipmentDatabase.getHands(offId) == 1
    }

    fun usingChefstaff(): Boolean =
        EquipmentDatabase.isChefStaff(getEquipmentId(EquipmentSlot.WEAPON))

    /** Currently worn outfit name if all pieces match a known outfit; else null. */
    fun currentOutfitName(outfits: Collection<Pair<String, List<String>>>): String? {
        val eq = character.state.value.equipment
        for ((name, pieces) in outfits) {
            if (pieces.isEmpty()) continue
            if (pieces.all { piece ->
                    eq.values.any { worn -> worn.equals(piece, ignoreCase = true) }
                }
            ) {
                return name
            }
        }
        return null
    }

    companion object {
        const val TRUSTY = 5756
    }
}
