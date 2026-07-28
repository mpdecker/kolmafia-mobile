package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.ash.stringsFromEntry
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.modifiers.StringModifier

/** Desktop [InventoryManager.checkSkillGrantingEquipment]. */
object SkillGrantingEquipmentSync {

    const val ETERNITY_CODPIECE_ITEM = "The Eternity Codpiece"
    const val ETERNITY_CODPIECE_ID = 12067

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
        granted += codpieceGemSkills(context, gameDatabase, filterItemId)
        return granted
    }

    private fun codpieceGemSkills(
        context: DynamicItemModifierSync.CheckContext,
        gameDatabase: GameDatabase,
        filterItemId: Int?,
    ): Set<String> {
        val codpiece = gameDatabase.item(ETERNITY_CODPIECE_ITEM) ?: return emptySet()
        if (filterItemId != null &&
            filterItemId != codpiece.id &&
            context.codpieceGemNames.none { gemName ->
                gameDatabase.item(gemName)?.id == filterItemId
            }
        ) {
            return emptySet()
        }
        if (!DynamicItemModifierSync.isEquippedOrInInventory(codpiece.id, codpiece.name, context)) {
            return emptySet()
        }
        val granted = linkedSetOf<String>()
        for (gemName in context.codpieceGemNames) {
            if (filterItemId != null) {
                val gemItem = gameDatabase.item(gemName)
                if (gemItem?.id != filterItemId && filterItemId != codpiece.id) continue
            }
            val entry = ModifierDatabase.getEternityCodpiece(gemName) ?: continue
            granted += inventoryConditionalSkills(
                stringsFromEntry(entry, StringModifier.CONDITIONAL_SKILL_INVENTORY),
            )
            granted += equippedConditionalSkills(
                stringsFromEntry(entry, StringModifier.CONDITIONAL_SKILL_EQUIPPED),
            )
        }
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
