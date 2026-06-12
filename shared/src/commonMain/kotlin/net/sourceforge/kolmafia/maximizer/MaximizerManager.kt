package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.equipment.OutfitCheckpoint
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ModifierParser
import net.sourceforge.kolmafia.request.ClanStashRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.DisplayCaseRequest
import net.sourceforge.kolmafia.request.EquipmentRequest
import net.sourceforge.kolmafia.request.StorageRequest

/**
 * Greedy per-slot maximizer. Scores inventory + closet + storage + display + stash,
 * pulls from collections before equipping, and restores an outfit checkpoint on failure.
 */
open class MaximizerManager(
    private val gameDatabase: GameDatabase,
    private val inventoryManager: InventoryManager,
    private val equipmentRequest: EquipmentRequest,
    private val character: KoLCharacter,
    private val closetRequest: ClosetRequest? = null,
    private val storageRequest: StorageRequest? = null,
    private val displayCaseRequest: DisplayCaseRequest? = null,
    private val clanStashRequest: ClanStashRequest? = null,
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
        val closetContents = closetRequest?.fetchContents().orEmpty()
        val storageContents = storageRequest?.fetchContents().orEmpty()
        val displayContents = displayCaseRequest?.fetchContents().orEmpty()
        val stashContents = clanStashRequest?.fetchContents().orEmpty()
        val scoreBefore = scoreEquipped(charState.equipment, modifier)

        val bestPerSlot = findBestPerSlot(
            modifier, charState.equipment, invState,
            closetContents, storageContents, displayContents, stashContents,
        )
        val scoreAfter = bestPerSlot.values.sumOf { it.second }
        if (scoreAfter <= scoreBefore) {
            return MaximizeResult(false, goal, scoreBefore, scoreBefore)
        }

        val checkpoint = OutfitCheckpoint.snapshot(character, equipmentRequest, gameDatabase)
        val equipped = mutableMapOf<EquipmentSlot, String>()
        var anyFailure = false

        for ((slot, pair) in bestPerSlot) {
            val (name, _) = pair
            val itemId = gameDatabase.item(name)?.id ?: continue
            if (!ensureInInventory(itemId)) {
                anyFailure = true
                continue
            }
            if (equipmentRequest.equipItem(itemId, slot).isFailure) {
                anyFailure = true
            } else {
                equipped[slot] = name
            }
        }
        inventoryManager.syncCharacterEquipment()

        if (anyFailure || equipped.isEmpty()) {
            checkpoint.restore()
            return MaximizeResult(false, goal, scoreBefore, scoreBefore)
        }

        return MaximizeResult(
            success = true,
            goal = goal,
            scoreBefore = scoreBefore,
            scoreAfter = scoreAfter,
            equipped = equipped,
        )
    }

    private fun findBestPerSlot(
        modifier: DoubleModifier,
        equipment: Map<EquipmentSlot, String>,
        invState: InventoryState,
        closetContents: Map<Int, Int>,
        storageContents: Map<Int, Int>,
        displayContents: Map<Int, Int>,
        stashContents: Map<Int, Int>,
    ): Map<EquipmentSlot, Pair<String, Double>> {
        val candidateIds = buildSet {
            addAll(invState.items.keys)
            addAll(closetContents.keys)
            addAll(storageContents.keys)
            addAll(displayContents.keys)
            addAll(stashContents.keys)
        }
        val bestPerSlot = mutableMapOf<EquipmentSlot, Pair<String, Double>>()
        val usedItems = mutableSetOf<String>()
        for (slot in equipSlots) {
            var bestName = ""
            var bestScore = scoreItem(equipment[slot], modifier)
            for (itemId in candidateIds) {
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
        return bestPerSlot
    }

    private suspend fun ensureInInventory(itemId: Int): Boolean {
        if (inventoryCount(itemId) >= 1) return true
        if (closetRequest != null) {
            val before = inventoryCount(itemId)
            if (closetRequest.takeOut(itemId, 1).isSuccess) {
                inventoryManager.fetchInventory()
                if (inventoryCount(itemId) > before) return true
            }
        }
        if (storageRequest != null) {
            val before = inventoryCount(itemId)
            if (storageRequest.withdraw(itemId, 1).isSuccess) {
                inventoryManager.fetchInventory()
                if (inventoryCount(itemId) > before) return true
            }
        }
        if (displayCaseRequest != null) {
            val before = inventoryCount(itemId)
            if (displayCaseRequest.takeOut(itemId, 1).isSuccess) {
                inventoryManager.fetchInventory()
                if (inventoryCount(itemId) > before) return true
            }
        }
        if (clanStashRequest != null) {
            val before = inventoryCount(itemId)
            if (clanStashRequest.takeOut(itemId, 1).isSuccess) {
                inventoryManager.fetchInventory()
                if (inventoryCount(itemId) > before) return true
            }
        }
        return inventoryCount(itemId) >= 1
    }

    private fun inventoryCount(itemId: Int): Int =
        inventoryManager.state.value.items[itemId]?.quantity ?: 0

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
