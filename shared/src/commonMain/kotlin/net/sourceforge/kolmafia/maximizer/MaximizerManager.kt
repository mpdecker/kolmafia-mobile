package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ModifierParser
import net.sourceforge.kolmafia.request.EquipmentRequest

/**
 * Inventory-only greedy maximizer MVP. Equips the best per-slot item for a single-stat goal.
 */
open class MaximizerManager(
    private val gameDatabase: GameDatabase,
    private val inventoryManager: InventoryManager,
    private val equipmentRequest: EquipmentRequest,
    private val character: KoLCharacter,
) {
    private val equipSlots = listOf(
        EquipmentSlot.HAT,
        EquipmentSlot.WEAPON,
        EquipmentSlot.OFFHAND,
        EquipmentSlot.SHIRT,
        EquipmentSlot.PANTS,
        EquipmentSlot.ACC1,
        EquipmentSlot.ACC2,
        EquipmentSlot.ACC3,
        EquipmentSlot.FAMILIAR,
        EquipmentSlot.CONTAINER,
    )

    open suspend fun maximize(goalText: String): MaximizeResult {
        val goal = goalText.trim()
        val modifier = MaximizeGoal.parse(goal)
            ?: return MaximizeResult(false, goal, 0.0, 0.0)

        val charState = character.state.value
        val invState = inventoryManager.state.value
        val scoreBefore = scoreEquipped(charState.equipment, modifier)

        val bestPerSlot = mutableMapOf<EquipmentSlot, Pair<String, Double>>()
        val usedItems = mutableSetOf<String>()
        for (slot in equipSlots) {
            var bestName = ""
            var bestScore = scoreItem(charState.equipment[slot], modifier)
            for ((itemId, _) in invState.items) {
                val itemData = gameDatabase.item(itemId) ?: continue
                if (!itemData.isEquipment || itemData.name in usedItems) continue
                if (!fitsSlot(itemData, slot)) continue
                val score = scoreItem(itemData.name, modifier)
                if (score > bestScore) {
                    bestScore = score
                    bestName = itemData.name
                }
            }
            if (bestName.isNotBlank()) {
                bestPerSlot[slot] = bestName to bestScore
                usedItems.add(bestName)
            }
        }

        val scoreAfter = bestPerSlot.values.sumOf { it.second }
        if (scoreAfter <= scoreBefore) {
            return MaximizeResult(false, goal, scoreBefore, scoreBefore)
        }

        val equipped = mutableMapOf<EquipmentSlot, String>()
        for ((slot, pair) in bestPerSlot) {
            val (name, _) = pair
            val itemId = gameDatabase.item(name)?.id ?: continue
            equipmentRequest.equipItem(itemId, slot).onSuccess {
                equipped[slot] = name
            }
        }
        inventoryManager.syncCharacterEquipment()

        return MaximizeResult(
            success = equipped.isNotEmpty(),
            goal = goal,
            scoreBefore = scoreBefore,
            scoreAfter = scoreAfter,
            equipped = equipped,
        )
    }

    private fun scoreItem(itemName: String?, modifier: DoubleModifier): Double {
        if (itemName.isNullOrBlank()) return 0.0
        val entry = gameDatabase.itemModifier(itemName) ?: return 0.0
        return ModifierParser.parse(entry.modifiers).get(modifier)
    }

    private fun fitsSlot(item: ItemData, slot: EquipmentSlot): Boolean = when (slot) {
        EquipmentSlot.HAT -> item.primaryUse == ItemPrimaryUse.HAT
        EquipmentSlot.WEAPON -> item.primaryUse in setOf(ItemPrimaryUse.WEAPON, ItemPrimaryUse.SIXGUN)
        EquipmentSlot.OFFHAND -> item.primaryUse == ItemPrimaryUse.OFFHAND
        EquipmentSlot.SHIRT -> item.primaryUse == ItemPrimaryUse.SHIRT
        EquipmentSlot.PANTS -> item.primaryUse == ItemPrimaryUse.PANTS
        EquipmentSlot.ACC1, EquipmentSlot.ACC2, EquipmentSlot.ACC3 ->
            item.primaryUse == ItemPrimaryUse.ACCESSORY
        EquipmentSlot.FAMILIAR -> item.primaryUse == ItemPrimaryUse.FAMILIAR
        EquipmentSlot.CONTAINER -> item.primaryUse == ItemPrimaryUse.CONTAINER
    }

    private fun scoreEquipped(
        equipment: Map<EquipmentSlot, String>,
        modifier: DoubleModifier,
    ): Double = equipSlots.sumOf { scoreItem(equipment[it], modifier) }
}
