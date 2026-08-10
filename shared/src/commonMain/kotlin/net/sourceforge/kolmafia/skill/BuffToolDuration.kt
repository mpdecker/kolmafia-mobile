package net.sourceforge.kolmafia.skill

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.equipment.OutfitManager

/** Desktop [SkillDatabase.getEffectDuration] wizard-hat + BuffTool layer for OTHER buffs. */
object BuffToolDuration {

    const val JEWEL_EYED_WIZARD_HAT = 1653
    const val REPLICA_JEWEL_EYED_WIZARD_HAT = 11199

    fun resolveBuffDuration(
        baseDuration: Int,
        skillId: Int,
        charState: CharacterState,
        accessibleCount: (Int) -> Int,
        gameDatabase: GameDatabase?,
    ): Int {
        var actualDuration = baseDuration
        if (hasWizardHat(charState, accessibleCount, gameDatabase)) {
            actualDuration += 5
        }

        val tools = BuffTools.toolsForSkill(skillId) ?: return actualDuration

        var inventoryDuration = 0
        for (tool in tools) {
            val current = actualDuration + tool.bonusTurns
            if (current <= inventoryDuration) continue
            if (hasToolAvailable(tool, charState, accessibleCount, gameDatabase)) {
                inventoryDuration = current
            }
        }

        return if (inventoryDuration > 0) inventoryDuration else actualDuration
    }

    fun hasWizardHat(
        charState: CharacterState,
        accessibleCount: (Int) -> Int,
        gameDatabase: GameDatabase?,
    ): Boolean {
        if (accessibleCount(JEWEL_EYED_WIZARD_HAT) > 0) return true
        if (isHatEquipped(JEWEL_EYED_WIZARD_HAT, charState, gameDatabase)) return true
        if (!charState.inLegacyOfLoathing) return false
        if (accessibleCount(REPLICA_JEWEL_EYED_WIZARD_HAT) > 0) return true
        return isHatEquipped(REPLICA_JEWEL_EYED_WIZARD_HAT, charState, gameDatabase)
    }

    fun hasToolAvailable(
        tool: BuffTool,
        charState: CharacterState,
        accessibleCount: (Int) -> Int,
        gameDatabase: GameDatabase?,
    ): Boolean {
        if (tool.isClassLimited && charState.characterClassEnum != tool.ascensionClass) {
            return false
        }
        if (accessibleCount(tool.itemId) > 0) return true
        val itemName = gameDatabase?.item(tool.itemId)?.name ?: return false
        return OutfitManager.equippedCount(itemName, charState.equipment) > 0
    }

    private fun isHatEquipped(
        itemId: Int,
        charState: CharacterState,
        gameDatabase: GameDatabase?,
    ): Boolean {
        val hatName = charState.equipment[EquipmentSlot.HAT]?.takeIf { it.isNotBlank() } ?: return false
        val itemName = gameDatabase?.item(itemId)?.name
        if (itemName != null && hatName.equals(itemName, ignoreCase = true)) return true
        if (itemId == JEWEL_EYED_WIZARD_HAT &&
            hatName.equals("jewel-eyed wizard hat", ignoreCase = true)
        ) {
            return true
        }
        if (itemId == REPLICA_JEWEL_EYED_WIZARD_HAT &&
            hatName.equals("replica jewel-eyed wizard hat", ignoreCase = true)
        ) {
            return true
        }
        if (charState.inHatTrick) {
            return charState.hatTrickHatIds.any { hatId ->
                hatId == itemId || gameDatabase?.item(hatId)?.name?.equals(hatName, ignoreCase = true) == true
            }
        }
        return false
    }
}
