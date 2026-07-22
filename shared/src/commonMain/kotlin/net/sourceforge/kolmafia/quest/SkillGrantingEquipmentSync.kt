package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.ash.stringsFromEntry
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.modifiers.StringModifier

/** Desktop [InventoryManager.checkSkillGrantingEquipment]. */
object SkillGrantingEquipmentSync {

    fun grantedSkillNames(
        context: DynamicItemModifierSync.CheckContext,
        gameDatabase: GameDatabase,
        filterItemId: Int? = null,
    ): Set<String> {
        val granted = linkedSetOf<String>()
        for (itemName in ModifierDatabase.inventorySkillProviderNames()) {
            val item = gameDatabase.item(itemName) ?: continue
            if (filterItemId != null && item.id != filterItemId) continue
            if (!DynamicItemModifierSync.isEquippedOrInInventory(item.id, item.name, context)) {
                continue
            }
            val entry = ModifierDatabase.getItem(itemName) ?: continue
            granted += inventoryConditionalSkills(stringsFromEntry(entry, StringModifier.CONDITIONAL_SKILL_INVENTORY))
            if (context.equippedItemNames.any { it.equals(itemName, ignoreCase = true) }) {
                granted += equippedConditionalSkills(stringsFromEntry(entry, StringModifier.CONDITIONAL_SKILL_EQUIPPED))
            }
        }
        // Eternity codpiece gem slots deferred to Phase 137 (no codpiece slots in CharacterState yet).
        return granted
    }

    private fun inventoryConditionalSkills(skillNames: List<String>): Set<String> =
        skillNames.filter { it.isNotBlank() }.toSet()

    private fun equippedConditionalSkills(skillNames: List<String>): Set<String> =
        skillNames.filter { skillName ->
            skillName.isNotBlank() &&
                SkillDefinitionDatabase.getByName(skillName)?.isNonCombat == true
        }.toSet()
}
