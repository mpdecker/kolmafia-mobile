package net.sourceforge.kolmafia.skill

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionProxy
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DynamicItemModifierSync
import net.sourceforge.kolmafia.quest.SkillGrantingEquipmentSync
import net.sourceforge.kolmafia.request.EquipmentRequest
import net.sourceforge.kolmafia.session.EquipmentManager

/**
 * Desktop UseSkillRequest.optimizeEquipment / prepareTool subset (Phases 2391–2405).
 */
object UseSkillOptimize {
    @Volatile
    var lastPreparedToolId: Int = -1
        private set

    @Volatile
    var lastPreparedSlot: EquipmentSlot? = null
        private set

    fun resetForTest() {
        lastPreparedToolId = -1
        lastPreparedSlot = null
    }

    /**
     * Select and (optionally) equip the best available BuffTool for [skillId].
     * Returns the chosen tool item id, or -1 if none / not a buff-tool skill.
     */
    suspend fun optimizeEquipment(
        skillId: Int,
        preferences: Preferences?,
        character: KoLCharacter?,
        inventory: InventoryManager?,
        equipmentManager: EquipmentManager?,
        equipmentRequest: EquipmentRequest?,
        equip: Boolean = true,
    ): Int {
        lastPreparedToolId = -1
        lastPreparedSlot = null
        val codpieceId = prepareCodpieceForNoncombatSkill(
            skillId = skillId,
            character = character,
            inventory = inventory,
            equipmentManager = equipmentManager,
            equipmentRequest = equipmentRequest,
            equip = equip,
        )
        if (codpieceId == SkillGrantingEquipmentSync.ETERNITY_CODPIECE_ID) {
            return codpieceId
        }
        if (!SkillDefinitionProxy.isBuff(skillId)) return -1
        val tools = BuffTools.toolsForSkill(skillId) ?: return -1
        val switch = preferences?.getBoolean("switchEquipmentForBuffs", true) != false
        if (!switch) return -1

        val state = character?.state?.value
        val best = findBestTool(tools, state, inventory, equipmentManager) ?: return -1
        lastPreparedToolId = best.itemId
        val slot = slotForTool(best.itemId, equipmentManager) ?: EquipmentSlot.WEAPON
        lastPreparedSlot = slot

        if (!equip || equipmentRequest == null || equipmentManager == null) {
            return best.itemId
        }
        if (equipmentManager.hasEquipped(best.itemId)) {
            return best.itemId
        }
        // Local inventory equip only (no mall retrieve in this subset).
        val invCount = inventory?.state?.value?.items?.get(best.itemId)?.quantity ?: 0
        if (invCount <= 0 && !isEquippedByName(best.itemId, state)) {
            return best.itemId
        }
        equipmentRequest.equipItem(best.itemId, slot)
        return best.itemId
    }

    /**
     * Desktop UseSkillRequest.optimizeEquipment Codpiece-first branch: if a noncombat
     * skill is granted by a gem socketed in The Eternity Codpiece, prefer equipping
     * the Codpiece instead of unsocketing/equipping the gem.
     */
    suspend fun prepareCodpieceForNoncombatSkill(
        skillId: Int,
        character: KoLCharacter?,
        inventory: InventoryManager?,
        equipmentManager: EquipmentManager?,
        equipmentRequest: EquipmentRequest?,
        equip: Boolean = true,
        gameDatabase: GameDatabase = GameDatabase(),
    ): Int {
        if (SkillDefinitionDatabase.getById(skillId)?.isNonCombat != true) return -1
        val context = checkContextFrom(character, inventory)
        if (context.codpieceGemNames.isEmpty()) return -1
        if (!SkillGrantingEquipmentSync.grantsSkillViaSocketedGem(skillId, context, gameDatabase)) {
            return -1
        }
        val codpieceId = SkillGrantingEquipmentSync.ETERNITY_CODPIECE_ID
        val state = character?.state?.value
        if (!hasTool(codpieceId, state, inventory, equipmentManager)) return -1
        val alreadyEquipped = equipmentManager?.hasEquipped(codpieceId) == true ||
            isEquippedByName(codpieceId, state)
        if (!alreadyEquipped && equipmentManager != null && !equipmentManager.canEquip(codpieceId)) {
            return -1
        }
        lastPreparedToolId = codpieceId
        val slot = slotForCodpiece(equipmentManager)
        lastPreparedSlot = slot
        if (!equip || equipmentRequest == null) {
            return codpieceId
        }
        if (alreadyEquipped) {
            return codpieceId
        }
        equipmentRequest.equipItem(codpieceId, slot)
        return codpieceId
    }

    private fun checkContextFrom(
        character: KoLCharacter?,
        inventory: InventoryManager?,
    ): DynamicItemModifierSync.CheckContext {
        val state = character?.state?.value
        val inventoryItemIds = inventory?.state?.value?.items
            ?.filterValues { it.quantity > 0 }
            ?.keys
            ?: emptySet()
        val equippedItemNames = state?.equipment?.values
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()
        val codpieceGemNames = state?.equipment
            ?.filterKeys { it in EquipmentSlot.CODPIECE_SLOTS }
            ?.values
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()
        return DynamicItemModifierSync.CheckContext(
            inventoryItemIds = inventoryItemIds,
            equippedItemNames = equippedItemNames,
            activeEffectNames = emptySet(),
            codpieceGemNames = codpieceGemNames,
        )
    }

    private fun slotForCodpiece(equipmentManager: EquipmentManager?): EquipmentSlot {
        equipmentManager?.itemIdToEquipmentType(SkillGrantingEquipmentSync.ETERNITY_CODPIECE_ID)?.let {
            return it
        }
        return when (ItemDatabase.getById(SkillGrantingEquipmentSync.ETERNITY_CODPIECE_ID)?.primaryUse) {
            ItemPrimaryUse.PANTS -> EquipmentSlot.PANTS
            ItemPrimaryUse.ACCESSORY -> EquipmentSlot.ACC1
            else -> EquipmentSlot.PANTS
        }
    }

    fun findBestTool(
        tools: Array<BuffTool>,
        state: CharacterState?,
        inventory: InventoryManager?,
        equipmentManager: EquipmentManager?,
    ): BuffTool? {
        for (tool in tools) {
            if (tool.isClassLimited &&
                state?.characterClassEnum != null &&
                state.characterClassEnum != tool.ascensionClass
            ) {
                continue
            }
            if (hasTool(tool.itemId, state, inventory, equipmentManager)) {
                return tool
            }
        }
        // Weakest (last) as retrieve hint — return it if we at least know the id
        return tools.lastOrNull()
    }

    fun hasTool(
        itemId: Int,
        state: CharacterState?,
        inventory: InventoryManager?,
        equipmentManager: EquipmentManager?,
    ): Boolean {
        if (equipmentManager?.hasEquipped(itemId) == true) return true
        if (isEquippedByName(itemId, state)) return true
        val qty = inventory?.state?.value?.items?.get(itemId)?.quantity ?: 0
        return qty > 0
    }

    fun slotForTool(itemId: Int, equipmentManager: EquipmentManager?): EquipmentSlot {
        equipmentManager?.itemIdToEquipmentType(itemId)?.let { return it }
        return when (ItemDatabase.getById(itemId)?.primaryUse) {
            net.sourceforge.kolmafia.data.ItemPrimaryUse.OFFHAND -> EquipmentSlot.OFFHAND
            net.sourceforge.kolmafia.data.ItemPrimaryUse.HAT -> EquipmentSlot.HAT
            else -> EquipmentSlot.WEAPON
        }
    }

    private fun isEquippedByName(itemId: Int, state: CharacterState?): Boolean {
        if (state == null) return false
        val name = ItemDatabase.getItemName(itemId)
        if (name.isBlank()) return false
        return state.equipment.values.any { it.equals(name, ignoreCase = true) }
    }
}
