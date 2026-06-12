package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.equipment.OutfitCheckpoint
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.modifiers.ModifierParser
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.PastaThrall
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.request.ClanStashRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.DisplayCaseRequest
import net.sourceforge.kolmafia.request.EquipmentRequest
import net.sourceforge.kolmafia.request.StorageRequest

open class MaximizerManager(
    private val gameDatabase: GameDatabase,
    private val inventoryManager: InventoryManager,
    private val equipmentRequest: EquipmentRequest,
    private val character: KoLCharacter,
    private val closetRequest: ClosetRequest? = null,
    private val storageRequest: StorageRequest? = null,
    private val displayCaseRequest: DisplayCaseRequest? = null,
    private val clanStashRequest: ClanStashRequest? = null,
    private val familiarManager: FamiliarManager? = null,
    private val preferences: Preferences? = null,
    private val skillManager: SkillManager? = null,
) {
    companion object {
        const val CROWN_OF_THRONES = "Crown of Thrones"
        const val BUDDY_BJORN = "Buddy Bjorn"
    }

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
        val spec = MaximizeGoal.parseSpec(goal)
            ?: return MaximizeResult(false, goal, 0.0, 0.0)
        val effectiveSpec = spec.withCarryEquipment()

        val charState = character.state.value
        val invState = inventoryManager.state.value
        val closetContents = closetRequest?.fetchContents().orEmpty()
        val storageContents = storageRequest?.fetchContents().orEmpty()
        val displayContents = displayCaseRequest?.fetchContents().orEmpty()
        val stashContents = clanStashRequest?.fetchContents().orEmpty()
        val scoreBefore = scoreEquipped(charState.equipment, effectiveSpec.primary) +
            scoreFamiliarBonuses(charState, effectiveSpec.primary) +
            scoreCurrentThrall(effectiveSpec.primary)

        var bestPerSlot = findBestPerSlot(
            effectiveSpec, charState.equipment, invState,
            closetContents, storageContents, displayContents, stashContents,
        )
        bestPerSlot = applyEquipRequired(effectiveSpec, bestPerSlot, charState.equipment)
        val enthronedBonus = scoreFamiliarList(effectiveSpec.enthronedFamiliars, effectiveSpec.primary)
        val bjornBonus = scoreFamiliarList(effectiveSpec.bjornifiedFamiliars, effectiveSpec.primary)
        val (targetThrall, thrallBonus) = resolveTargetThrall(effectiveSpec)
        val scoreAfter = bestPerSlot.values.sumOf { it.second } + enthronedBonus + bjornBonus + thrallBonus
        if (scoreAfter <= scoreBefore) {
            return MaximizeResult(false, goal, scoreBefore, scoreBefore)
        }

        val checkpoint = OutfitCheckpoint.snapshot(character, equipmentRequest, gameDatabase)
        val equipped = mutableMapOf<EquipmentSlot, String>()
        var anyFailure = false
        var familiarSwitched: String? = null
        var enthronedSwitched: String? = null
        var bjornifiedSwitched: String? = null
        var thrallSwitched: String? = null

        val familiarRace = resolveFamiliarSwitch(effectiveSpec)
        if (familiarRace != null) {
            familiarManager?.setFamiliar(familiarRace)?.onSuccess {
                familiarSwitched = familiarRace
            }?.onFailure { anyFailure = true }
        }
        effectiveSpec.enthronedFamiliars.firstOrNull()?.let { race ->
            familiarManager?.setEnthroned(race)?.onSuccess {
                enthronedSwitched = race
            }?.onFailure { anyFailure = true }
        }
        effectiveSpec.bjornifiedFamiliars.firstOrNull()?.let { race ->
            familiarManager?.setBjornified(race)?.onSuccess {
                bjornifiedSwitched = race
            }?.onFailure { anyFailure = true }
        }
        if (targetThrall != null &&
            !targetThrall.equals(preferences?.getString("_currentThrall", ""), ignoreCase = true)
        ) {
            bindThrall(targetThrall)?.onSuccess {
                thrallSwitched = targetThrall
            }?.onFailure { anyFailure = true }
        }

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

        val madeChange = equipped.isNotEmpty() ||
            familiarSwitched != null ||
            enthronedSwitched != null ||
            bjornifiedSwitched != null ||
            thrallSwitched != null
        if (anyFailure || !madeChange) {
            checkpoint.restore()
            return MaximizeResult(false, goal, scoreBefore, scoreBefore)
        }

        return MaximizeResult(
            success = true,
            goal = goal,
            scoreBefore = scoreBefore,
            scoreAfter = scoreAfter,
            equipped = equipped,
            familiarSwitched = familiarSwitched,
            enthronedSwitched = enthronedSwitched,
            bjornifiedSwitched = bjornifiedSwitched,
            thrallSwitched = thrallSwitched,
        )
    }

    private fun resolveTargetThrall(spec: MaximizeSpec): Pair<String?, Double> {
        val prefs = preferences ?: return null to 0.0
        if (spec.switchThralls.isNotEmpty()) {
            val name = spec.switchThralls.first()
            val level = PastaThrall.thrallLevel(prefs, name)
            return name to scoreThrall(name, level, spec.primary)
        }
        return bestThrallScore(spec.primary, prefs)
    }

    private fun bestThrallScore(modifier: DoubleModifier, prefs: Preferences): Pair<String?, Double> {
        val currentName = prefs.getString("_currentThrall", "")
        var bestName: String? = currentName.takeIf { it.isNotBlank() }
        var bestScore = if (currentName.isBlank()) 0.0 else scoreThrall(
            currentName,
            PastaThrall.thrallLevel(prefs, currentName),
            modifier,
        )
        for (index in 1..8) {
            val parsed = PastaThrall.parsePref(prefs.getString(PastaThrall.prefKey(index), "")) ?: continue
            val score = scoreThrall(parsed.second, parsed.first, modifier)
            if (score > bestScore) {
                bestScore = score
                bestName = parsed.second
            }
        }
        return bestName to bestScore
    }

    private fun scoreCurrentThrall(modifier: DoubleModifier): Double {
        val prefs = preferences ?: return 0.0
        val name = prefs.getString("_currentThrall", "")
        if (name.isBlank()) return 0.0
        return scoreThrall(name, PastaThrall.thrallLevel(prefs, name), modifier)
    }

    private fun scoreThrall(name: String, level: Int, modifier: DoubleModifier): Double {
        val entry = ModifierDatabase.getThrall(name) ?: return 0.0
        val ctx = ExpressionContext(thrallLevel = level)
        return ModifierParser.parse(entry.modifiers, ctx).get(modifier)
    }

    private suspend fun bindThrall(thrallName: String): Result<Unit>? {
        val skillId = PastaThrall.bindSkillId(thrallName) ?: return null
        val skill = skillManager?.state?.value?.skills?.find { it.id == skillId } ?: return null
        return skillManager.cast(skill)
    }

    private fun applyEquipRequired(
        spec: MaximizeSpec,
        bestPerSlot: Map<EquipmentSlot, Pair<String, Double>>,
        equipment: Map<EquipmentSlot, String>,
    ): Map<EquipmentSlot, Pair<String, Double>> {
        if (spec.equipRequired.isEmpty()) return bestPerSlot
        val updated = bestPerSlot.toMutableMap()
        for (name in spec.equipRequired) {
            val item = gameDatabase.item(name) ?: continue
            if (!itemMeetsConstraints(name, spec)) continue
            val slot = slotForItem(item) ?: continue
            updated[slot] = name to scoreItem(name, spec.primary)
        }
        return updated
    }

    private fun resolveFamiliarSwitch(spec: MaximizeSpec): String? {
        if (spec.switchFamiliars.isEmpty()) return null
        val owned = familiarManager?.state?.value?.ownedFamiliars.orEmpty()
        for (race in spec.switchFamiliars) {
            if (owned.any { it.race.equals(race, ignoreCase = true) }) return race
        }
        return null
    }

    private fun findBestPerSlot(
        spec: MaximizeSpec,
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
            var bestScore = scoreItem(equipment[slot], spec.primary)
            for (itemId in candidateIds) {
                val itemData = gameDatabase.item(itemId) ?: continue
                if (!itemData.isEquipment || itemData.name in usedItems) continue
                if (!fitsSlot(itemData, slot)) continue
                if (spec.requireMelee && slot == EquipmentSlot.WEAPON &&
                    itemData.primaryUse == ItemPrimaryUse.SIXGUN
                ) continue
                if (spec.requireHands && slot == EquipmentSlot.OFFHAND &&
                    itemData.primaryUse != ItemPrimaryUse.OFFHAND
                ) continue
                if (!itemMeetsConstraints(itemData.name, spec)) continue
                val score = scoreItem(itemData.name, spec.primary)
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

    private fun itemMeetsConstraints(itemName: String, spec: MaximizeSpec): Boolean {
        val entry = gameDatabase.itemModifier(itemName) ?: return spec.requiredBooleans.isEmpty()
        val mods = ModifierParser.parse(entry.modifiers)
        for (req in spec.requiredBooleans) {
            if (!mods.get(req)) return false
        }
        for (forbid in spec.forbiddenBooleans) {
            if (mods.get(forbid)) return false
        }
        return true
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

    private fun slotForItem(item: ItemData): EquipmentSlot? = when (item.primaryUse) {
        ItemPrimaryUse.HAT -> EquipmentSlot.HAT
        ItemPrimaryUse.WEAPON, ItemPrimaryUse.SIXGUN -> EquipmentSlot.WEAPON
        ItemPrimaryUse.OFFHAND -> EquipmentSlot.OFFHAND
        ItemPrimaryUse.SHIRT -> EquipmentSlot.SHIRT
        ItemPrimaryUse.PANTS -> EquipmentSlot.PANTS
        ItemPrimaryUse.ACCESSORY -> EquipmentSlot.ACC1
        ItemPrimaryUse.FAMILIAR -> EquipmentSlot.FAMILIAR
        ItemPrimaryUse.CONTAINER -> EquipmentSlot.CONTAINER
        else -> null
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

    private fun scoreFamiliarBonuses(
        charState: net.sourceforge.kolmafia.character.CharacterState,
        modifier: DoubleModifier,
    ): Double {
        var total = 0.0
        if (charState.enthronedFamiliarName.isNotBlank()) {
            total += scoreFamiliarList(listOf(charState.enthronedFamiliarName), modifier)
        }
        if (charState.bjornedFamiliarName.isNotBlank()) {
            total += scoreFamiliarList(listOf(charState.bjornedFamiliarName), modifier)
        }
        return total
    }

    private fun scoreFamiliarList(races: List<String>, modifier: DoubleModifier): Double =
        races.sumOf { race ->
            ModifierDatabase.getFamiliar(race)?.let { entry ->
                ModifierParser.parse(entry.modifiers).get(modifier)
            } ?: 0.0
        }

    private fun MaximizeSpec.withCarryEquipment(): MaximizeSpec {
        val carry = buildList {
            if (enthronedFamiliars.any { !it.equals("none", ignoreCase = true) }) {
                add(CROWN_OF_THRONES)
            }
            if (bjornifiedFamiliars.any { !it.equals("none", ignoreCase = true) }) {
                add(BUDDY_BJORN)
            }
        }
        if (carry.isEmpty()) return this
        return copy(equipRequired = (equipRequired + carry).distinct())
    }
}
