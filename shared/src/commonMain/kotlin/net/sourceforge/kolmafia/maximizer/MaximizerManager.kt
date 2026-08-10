package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.Beeosity
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.equipment.OutfitCheckpoint
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.familiar.FamiliarUsability
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemAvailability
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.modifiers.ModifierParser
import net.sourceforge.kolmafia.modifiers.ModifierValues
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.PastaThrall
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.request.ClanStashRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.DisplayCaseRequest
import net.sourceforge.kolmafia.request.EquipmentRequest
import net.sourceforge.kolmafia.request.RestrictionListRefresh
import net.sourceforge.kolmafia.request.StandardRequest
import net.sourceforge.kolmafia.request.StorageRequest
import net.sourceforge.kolmafia.request.ThriftyRequest
import net.sourceforge.kolmafia.request.TrendyRequest
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.mall.MallPriceManager

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
    private val standardRequest: StandardRequest? = null,
    private val thriftyRequest: ThriftyRequest? = null,
    private val trendyRequest: TrendyRequest? = null,
    private val skillManager: SkillManager? = null,
    private val retrieveItemService: RetrieveItemService? = null,
    private val mallPriceManager: MallPriceManager? = null,
    private val mallManager: MallManager? = null,
) {
    companion object {
        const val CROWN_OF_THRONES = "Crown of Thrones"
        const val BUDDY_BJORN = "Buddy Bjorn"
        const val COMBINATION_LIMIT_PREF = "maximizerCombinationLimit"
        private const val DEFAULT_COMBO_LIMIT = 64
        private const val TOP_ACCESSORY_CANDIDATES = 4
        private const val TOP_WEAPON_OFFHAND_CANDIDATES = 4
        private const val TOP_ARMOR_CANDIDATES = 3
    }

    private val armorSlots = listOf(
        EquipmentSlot.HAT,
        EquipmentSlot.SHIRT,
        EquipmentSlot.PANTS,
    )

    private val weaponOffhandSlots = listOf(
        EquipmentSlot.WEAPON,
        EquipmentSlot.OFFHAND,
    )

    private val accessorySlots = listOf(
        EquipmentSlot.ACC1,
        EquipmentSlot.ACC2,
        EquipmentSlot.ACC3,
    )

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
        val plan = buildMaximizePlan(goalText)
            ?: return MaximizeResult(false, goalText.trim(), 0.0, 0.0)
        if (plan.scoreAfter <= plan.scoreBefore) {
            return MaximizeResult(false, plan.goal, plan.scoreBefore, plan.scoreBefore)
        }

        val effectiveSpec = plan.spec
        val charState = character.state.value
        val checkpoint = OutfitCheckpoint.snapshot(character, equipmentRequest, gameDatabase)
        val equipped = mutableMapOf<EquipmentSlot, String>()
        var anyFailure = false
        var familiarSwitched: String? = null
        var enthronedSwitched: String? = null
        var bjornifiedSwitched: String? = null
        var thrallSwitched: String? = null
        val (targetThrall, _) = resolveTargetThrall(effectiveSpec)
        val familiarRace = resolveFamiliarSwitch(effectiveSpec)
        val enthronedRace = resolveEnthronedFamiliar(effectiveSpec)
        val bjornifiedRace = resolveBjornifiedFamiliar(effectiveSpec)

        if (familiarRace != null) {
            familiarManager?.setFamiliar(familiarRace)?.onSuccess {
                familiarSwitched = familiarRace
            }?.onFailure { anyFailure = true }
        }
        enthronedRace?.let { race ->
            familiarManager?.setEnthroned(race)?.onSuccess {
                enthronedSwitched = race
            }?.onFailure { anyFailure = true }
        }
        bjornifiedRace?.let { race ->
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

        for ((slot, pair) in plan.bestPerSlot) {
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
            return MaximizeResult(false, plan.goal, plan.scoreBefore, plan.scoreBefore)
        }

        return MaximizeResult(
            success = true,
            goal = plan.goal,
            scoreBefore = plan.scoreBefore,
            scoreAfter = plan.scoreAfter,
            equipped = equipped,
            familiarSwitched = familiarSwitched,
            enthronedSwitched = enthronedSwitched,
            bjornifiedSwitched = bjornifiedSwitched,
            thrallSwitched = thrallSwitched,
        )
    }

    /** Speculate-only loadout search — no equip side effects. */
    open suspend fun speculate(goalText: String): List<String> {
        val plan = buildMaximizePlan(goalText)
            ?: return listOf("Invalid goal: ${goalText.trim()}")
        if (plan.scoreAfter <= plan.scoreBefore) {
            return listOf("No improvement for ${plan.goal}")
        }
        val lines = mutableListOf<String>()
        for ((slot, pair) in plan.bestPerSlot) {
            val (name, score) = pair
            if (name.isNotBlank()) {
                lines += "${slot.name}: $name ($score)"
            }
        }
        lines += "Score: ${plan.scoreBefore} -> ${plan.scoreAfter}"
        return lines
    }

    private suspend fun buildMaximizePlan(goalText: String): MaximizePlan? {
        val goal = goalText.trim()
        val spec = MaximizeGoal.parseSpec(goal) ?: return null
        val effectiveSpec = spec.withCarryEquipment()

        val charState = character.state.value
        val invState = inventoryManager.state.value
        val closetContents = closetRequest?.fetchContents().orEmpty()
        val storageContents = storageRequest?.fetchContents().orEmpty()
        val displayContents = displayCaseRequest?.fetchContents().orEmpty()
        val stashContents = clanStashRequest?.fetchContents().orEmpty()
        val scoreBefore = MaximizerSpeculation.scoreLoadout(
            charState, charState.equipment.mapValues { (_, name) -> name to 0.0 },
            effectiveSpec.evaluator,
            scoreFamiliarBonuses(charState, effectiveSpec.primary),
            scoreCurrentThrall(effectiveSpec.primary),
        )

        val candidateIds = buildCandidateIds(
            invState, closetContents, storageContents, displayContents, stashContents, effectiveSpec,
        )
        val priceLevel = maximizerPriceLevel()
        prefetchMallPrices(candidateIds, effectiveSpec, priceLevel)
        var bestPerSlot = findBestPerSlot(
            effectiveSpec, charState, invState,
            closetContents, storageContents, displayContents, stashContents,
        )
        val rankedBuckets = buildRankedBuckets(
            effectiveSpec, candidateIds, invState,
            closetContents, storageContents, displayContents, stashContents,
        )
        val comboBudget = ComboBudget(
            preferences?.getInt(COMBINATION_LIMIT_PREF, DEFAULT_COMBO_LIMIT) ?: DEFAULT_COMBO_LIMIT,
        )
        bestPerSlot = refineAccessoryCombinations(
            effectiveSpec, charState, rankedBuckets,
            bestPerSlot, comboBudget,
        )
        bestPerSlot = refineWeaponOffhandCombinations(
            effectiveSpec, charState, rankedBuckets,
            bestPerSlot, comboBudget,
        )
        bestPerSlot = refineArmorCombinations(
            effectiveSpec, charState, rankedBuckets,
            bestPerSlot, comboBudget,
        )
        val (targetThrall, thrallBonus) = resolveTargetThrall(effectiveSpec)
        val familiarRace = resolveFamiliarSwitch(effectiveSpec)
        val enthronedRace = resolveEnthronedFamiliar(effectiveSpec)
        val bjornifiedRace = resolveBjornifiedFamiliar(effectiveSpec)
        val familiarBonus = familiarRace?.let { scoreFamiliarList(listOf(it), effectiveSpec.primary) } ?: 0.0
        val carryRaces = FamiliarCarryRules.carryRaces(effectiveSpec, familiarRace)
        val familiarCarryScorer = if (carryRaces.isNotEmpty()) {
            { itemName: String?, mod: DoubleModifier ->
                scoreFamiliarCarriedItem(itemName, mod, carryRaces, familiarRace, charState.familiarWeight)
            }
        } else {
            null
        }
        val speculated = MaximizerSpeculation.speculate(
            effectiveSpec,
            charState,
            MaximizerSpeculation.topCandidatesPerSlot(
                effectiveSpec,
                rankedBuckets,
                bestPerSlot.values.map { it.first }.toSet(),
                TOP_ARMOR_CANDIDATES,
                gameDatabase,
                ::scoreItem,
                ::itemMeetsConstraints,
                ::effectivePrice,
                carryRaces,
                familiarCarryScorer = familiarCarryScorer,
            ),
            comboBudget,
            familiarBonus,
            thrallBonus,
            bestPerSlot,
            ::effectivePrice,
        )
        if (speculated.isNotEmpty()) {
            bestPerSlot = speculated
        }
        bestPerSlot = applyEquipRequired(effectiveSpec, bestPerSlot, charState.equipment)
        val enthronedBonus = enthronedRace?.let { scoreFamiliarList(listOf(it), effectiveSpec.primary) } ?: 0.0
        val bjornBonus = bjornifiedRace?.let { scoreFamiliarList(listOf(it), effectiveSpec.primary) } ?: 0.0
        val scoreAfter = MaximizerSpeculation.scoreLoadout(
            charState, bestPerSlot, effectiveSpec.evaluator,
            enthronedBonus + bjornBonus + familiarBonus, thrallBonus,
        )
        if (charState.inBeecore &&
            loadoutBeeosity(charState.equipment, bestPerSlot) > effectiveSpec.maxBeeosity
        ) {
            return MaximizePlan(goal, effectiveSpec, scoreBefore, scoreBefore, emptyMap())
        }
        if (effectiveSpec.evaluator.failed) {
            return MaximizePlan(goal, effectiveSpec, scoreBefore, scoreBefore, emptyMap())
        }
        return MaximizePlan(goal, effectiveSpec, scoreBefore, scoreAfter, bestPerSlot)
    }

    private fun loadoutBeeosity(
        base: Map<EquipmentSlot, String>,
        assignment: Map<EquipmentSlot, Pair<String, Double>>,
    ): Int {
        val merged = base.toMutableMap()
        assignment.forEach { (slot, pair) ->
            if (pair.first.isNotBlank()) merged[slot] = pair.first
        }
        return Beeosity.equipmentBeeosity(merged)
    }

    private data class MaximizePlan(
        val goal: String,
        val spec: MaximizeSpec,
        val scoreBefore: Double,
        val scoreAfter: Double,
        val bestPerSlot: Map<EquipmentSlot, Pair<String, Double>>,
    )

    private fun resolveTargetThrall(spec: MaximizeSpec): Pair<String?, Double> {
        val prefs = preferences ?: return null to 0.0
        if (spec.switchThralls.isNotEmpty()) {
            return bestThrallFromCandidates(spec.switchThralls, spec.primary, prefs)
        }
        return bestThrallScore(spec.primary, prefs)
    }

    private fun bestThrallFromCandidates(
        names: List<String>,
        modifier: DoubleModifier,
        prefs: Preferences,
    ): Pair<String?, Double> {
        var bestName: String? = null
        var bestScore = Double.NEGATIVE_INFINITY
        var bestTie = Double.NEGATIVE_INFINITY
        for (name in names) {
            val level = PastaThrall.thrallLevel(prefs, name)
            val score = scoreThrall(name, level, modifier)
            val tie = thrallSecondaryScore(name, level)
            if (isBetterCandidate(score, tie, bestScore, bestTie)) {
                bestScore = score
                bestTie = tie
                bestName = name
            }
        }
        return bestName to bestScore.coerceAtLeast(0.0)
    }

    private fun bestThrallScore(modifier: DoubleModifier, prefs: Preferences): Pair<String?, Double> {
        val currentName = prefs.getString("_currentThrall", "")
        var bestName: String? = currentName.takeIf { it.isNotBlank() }
        var bestScore = if (currentName.isBlank()) 0.0 else scoreThrall(
            currentName,
            PastaThrall.thrallLevel(prefs, currentName),
            modifier,
        )
        var bestTie = if (currentName.isBlank()) 0.0 else thrallSecondaryScore(
            currentName,
            PastaThrall.thrallLevel(prefs, currentName),
        )
        for (index in 1..8) {
            val parsed = PastaThrall.parsePref(prefs.getString(PastaThrall.prefKey(index), "")) ?: continue
            val score = scoreThrall(parsed.second, parsed.first, modifier)
            val tie = thrallSecondaryScore(parsed.second, parsed.first)
            if (isBetterCandidate(score, tie, bestScore, bestTie)) {
                bestScore = score
                bestTie = tie
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
            updated[slot] = name to scoreItem(name, spec.evaluator)
        }
        return updated
    }

    private suspend fun resolveFamiliarSwitch(spec: MaximizeSpec): String? {
        if (spec.switchFamiliars.isEmpty()) return null
        val familiarState = familiarManager?.state?.value ?: return null
        val charState = character.state.value
        RestrictionListRefresh.ensureInitialized(
            charState,
            standardRequest,
            thriftyRequest,
            trendyRequest,
        )
        var bestRace: String? = null
        var bestScore = Double.NEGATIVE_INFINITY
        var bestTie = Double.NEGATIVE_INFINITY
        for (race in spec.switchFamiliars) {
            if (FamiliarUsability.usableByRace(
                    familiarState,
                    race,
                    charState,
                    preferences,
                ) == null
            ) continue
            val score = scoreFamiliarList(listOf(race), spec.primary)
            val tie = familiarSecondaryScore(race)
            if (isBetterCandidate(score, tie, bestScore, bestTie)) {
                bestScore = score
                bestTie = tie
                bestRace = race
            }
        }
        return bestRace
    }

    private suspend fun resolveEnthronedFamiliar(spec: MaximizeSpec): String? {
        if (spec.enthronedFamiliars.isEmpty()) return null
        val familiarState = familiarManager?.state?.value ?: return null
        val charState = character.state.value
        RestrictionListRefresh.ensureInitialized(
            charState,
            standardRequest,
            thriftyRequest,
            trendyRequest,
        )
        return FamiliarUsability.firstUsableFromGoals(
            familiarState,
            spec.enthronedFamiliars,
            charState,
            preferences,
        )
    }

    private suspend fun resolveBjornifiedFamiliar(spec: MaximizeSpec): String? {
        if (spec.bjornifiedFamiliars.isEmpty()) return null
        val familiarState = familiarManager?.state?.value ?: return null
        val charState = character.state.value
        RestrictionListRefresh.ensureInitialized(
            charState,
            standardRequest,
            thriftyRequest,
            trendyRequest,
        )
        return FamiliarUsability.firstUsableFromGoals(
            familiarState,
            spec.bjornifiedFamiliars,
            charState,
            preferences,
        )
    }

    private fun isBetterCandidate(
        score: Double,
        tie: Double,
        bestScore: Double,
        bestTie: Double,
    ): Boolean {
        if (score > bestScore + 1e-9) return true
        if (score < bestScore - 1e-9) return false
        return tie > bestTie + 1e-9
    }

    private fun familiarSecondaryScore(race: String): Double {
        val entry = ModifierDatabase.getFamiliar(race) ?: return 0.0
        val mods = ModifierParser.parse(entry.modifiers)
        return secondaryModifierScore(mods)
    }

    private fun thrallSecondaryScore(name: String, level: Int): Double {
        val entry = ModifierDatabase.getThrall(name) ?: return 0.0
        val mods = ModifierParser.parse(entry.modifiers, ExpressionContext(thrallLevel = level))
        return secondaryModifierScore(mods)
    }

    private fun secondaryModifierScore(mods: ModifierValues): Double =
        mods.get(DoubleModifier.INITIATIVE) +
            mods.get(DoubleModifier.ITEMDROP) +
            mods.get(DoubleModifier.MUS) +
            mods.get(DoubleModifier.MYS) +
            mods.get(DoubleModifier.MOX) +
            mods.get(DoubleModifier.MEATDROP)

    private fun findBestPerSlot(
        spec: MaximizeSpec,
        charState: CharacterState,
        invState: InventoryState,
        closetContents: Map<Int, Int>,
        storageContents: Map<Int, Int>,
        displayContents: Map<Int, Int>,
        stashContents: Map<Int, Int>,
    ): Map<EquipmentSlot, Pair<String, Double>> {
        val equipment = charState.equipment
        val candidateIds = buildCandidateIds(
            invState, closetContents, storageContents, displayContents, stashContents, spec,
        )
        val rankedBuckets = buildRankedBuckets(
            spec, candidateIds, invState,
            closetContents, storageContents, displayContents, stashContents,
        )
        val bestPerSlot = mutableMapOf<EquipmentSlot, Pair<String, Double>>()
        val usedItems = mutableSetOf<String>()
        for (slot in equipSlots) {
            var bestName = ""
            var bestScore = scoreItem(equipment[slot], spec.evaluator)
            val bucketSlots = MaximizerSlot.slotsForEquipmentSlot(slot, spec)
            for (ranked in MaximizerEquipmentEnumerator.mergeBuckets(
                rankedBuckets, bucketSlots, usedItems,
            )) {
                val (name, score) = ranked
                if (name in usedItems) continue
                if (score > bestScore) {
                    bestScore = score
                    bestName = name
                }
            }
            if (bestName.isNotBlank()) {
                bestPerSlot[slot] = bestName to bestScore
                usedItems.add(bestName)
            }
        }
        return bestPerSlot
    }

    private fun refineAccessoryCombinations(
        spec: MaximizeSpec,
        charState: CharacterState,
        rankedBuckets: SlotList<MaximizerRankedItem>,
        greedy: Map<EquipmentSlot, Pair<String, Double>>,
        budget: ComboBudget,
    ): Map<EquipmentSlot, Pair<String, Double>> {
        val nonAccessory = greedy.filterKeys { it !in accessorySlots }
        val usedElsewhere = nonAccessory.values.map { it.first }.toSet()
        val top = MaximizerEquipmentEnumerator.mergeBuckets(
            rankedBuckets,
            listOf(MaximizerSlot.ACC1),
            usedElsewhere,
            TOP_ACCESSORY_CANDIDATES,
        )
        if (top.size < 2) return greedy

        var bestAssignment = greedy
        var bestScore = scoreAssignment(greedy, spec.evaluator, charState)
        for (a in top) {
            for (b in top) {
                for (c in top) {
                    if (a.first == b.first || a.first == c.first || b.first == c.first) continue
                    if (budget.tick()) return bestAssignment
                    val combo = nonAccessory + mapOf(
                        EquipmentSlot.ACC1 to (a.first to a.second),
                        EquipmentSlot.ACC2 to (b.first to b.second),
                        EquipmentSlot.ACC3 to (c.first to c.second),
                    )
                    val score = scoreAssignment(combo, spec.evaluator, charState)
                    if (score > bestScore) {
                        bestScore = score
                        bestAssignment = combo
                    }
                }
            }
        }
        return bestAssignment
    }

    private fun refineWeaponOffhandCombinations(
        spec: MaximizeSpec,
        charState: CharacterState,
        rankedBuckets: SlotList<MaximizerRankedItem>,
        greedy: Map<EquipmentSlot, Pair<String, Double>>,
        budget: ComboBudget,
    ): Map<EquipmentSlot, Pair<String, Double>> {
        val nonWeaponOffhand = greedy.filterKeys { it !in weaponOffhandSlots }
        val usedElsewhere = nonWeaponOffhand.values.map { it.first }.toSet()
        val topWeapons = MaximizerEquipmentEnumerator.mergeBuckets(
            rankedBuckets,
            MaximizerSlot.weaponBuckets(spec),
            usedElsewhere,
            TOP_WEAPON_OFFHAND_CANDIDATES,
        )
        val topOffhands = MaximizerEquipmentEnumerator.mergeBuckets(
            rankedBuckets,
            MaximizerSlot.offhandBuckets(spec),
            usedElsewhere,
            TOP_WEAPON_OFFHAND_CANDIDATES,
        )
        if (topWeapons.isEmpty() || topOffhands.isEmpty()) return greedy

        var bestAssignment = greedy
        var bestScore = scoreAssignment(greedy, spec.evaluator, charState)
        for (weapon in topWeapons) {
            for (offhand in topOffhands) {
                if (weapon.first == offhand.first) continue
                if (budget.tick()) return bestAssignment
                val combo = nonWeaponOffhand + mapOf(
                    EquipmentSlot.WEAPON to (weapon.first to weapon.second),
                    EquipmentSlot.OFFHAND to (offhand.first to offhand.second),
                )
                val score = scoreAssignment(combo, spec.evaluator, charState)
                if (score > bestScore) {
                    bestScore = score
                    bestAssignment = combo
                }
            }
        }
        return bestAssignment
    }

    private fun refineArmorCombinations(
        spec: MaximizeSpec,
        charState: CharacterState,
        rankedBuckets: SlotList<MaximizerRankedItem>,
        greedy: Map<EquipmentSlot, Pair<String, Double>>,
        budget: ComboBudget,
    ): Map<EquipmentSlot, Pair<String, Double>> {
        val nonArmor = greedy.filterKeys { it !in armorSlots }
        val usedElsewhere = nonArmor.values.map { it.first }.toSet()
        val topHats = MaximizerEquipmentEnumerator.mergeBuckets(
            rankedBuckets, listOf(MaximizerSlot.HAT), usedElsewhere, TOP_ARMOR_CANDIDATES,
        )
        val topShirts = MaximizerEquipmentEnumerator.mergeBuckets(
            rankedBuckets, listOf(MaximizerSlot.SHIRT), usedElsewhere, TOP_ARMOR_CANDIDATES,
        )
        val topPants = MaximizerEquipmentEnumerator.mergeBuckets(
            rankedBuckets, listOf(MaximizerSlot.PANTS), usedElsewhere, TOP_ARMOR_CANDIDATES,
        )
        if (topHats.isEmpty() || topShirts.isEmpty() || topPants.isEmpty()) return greedy

        var bestAssignment = greedy
        var bestScore = scoreAssignment(greedy, spec.evaluator, charState)
        for (hat in topHats) {
            for (shirt in topShirts) {
                for (pants in topPants) {
                    if (hat.first == shirt.first || hat.first == pants.first || shirt.first == pants.first) {
                        continue
                    }
                    if (budget.tick()) return bestAssignment
                    val combo = nonArmor + mapOf(
                        EquipmentSlot.HAT to (hat.first to hat.second),
                        EquipmentSlot.SHIRT to (shirt.first to shirt.second),
                        EquipmentSlot.PANTS to (pants.first to pants.second),
                    )
                    val score = scoreAssignment(combo, spec.evaluator, charState)
                    if (score > bestScore) {
                        bestScore = score
                        bestAssignment = combo
                    }
                }
            }
        }
        return bestAssignment
    }

    private fun buildRankedBuckets(
        spec: MaximizeSpec,
        candidateIds: Set<Int>,
        invState: InventoryState,
        closetContents: Map<Int, Int>,
        storageContents: Map<Int, Int>,
        displayContents: Map<Int, Int>,
        stashContents: Map<Int, Int>,
    ): SlotList<MaximizerRankedItem> {
        val charState = character.state.value
        val priceLevel = maximizerPriceLevel()
        val maxPrice = spec.maxPrice?.toLong()
        val checkedContext = MaximizerCheckedItemBuilder.Context(
            spec = spec,
            gameDatabase = gameDatabase,
            characterState = charState,
            preferences = preferences,
            mallPriceManager = mallPriceManager,
            inventoryCount = ::inventoryCount,
            closetContents = closetContents,
            storageContents = storageContents,
            displayContents = displayContents,
            stashContents = stashContents,
            priceLevel = priceLevel,
        )
        val buckets = MaximizerEquipmentEnumerator.enumerate(
            candidateIds = candidateIds,
            spec = spec,
            gameDatabase = gameDatabase,
            checkedItem = { itemId ->
                val name = gameDatabase.item(itemId)?.name ?: ""
                MaximizerCheckedItemBuilder.build(itemId, name, checkedContext)
                    .validate(
                        maxPrice = maxPrice,
                        priceLevel = priceLevel,
                        availableMeat = charState.meat.toLong(),
                        storageMeat = charState.storageMeat,
                        mallPrice = { id -> mallPriceManager?.getMallPrice(id) ?: 0L },
                    )
            },
            scoreItem = ::scoreItem,
            itemMeetsConstraints = ::itemMeetsConstraints,
            priceFor = ::effectivePrice,
            autoContext = MaximizerAutoContext.from(spec.evaluator),
        )
        MaximizerSynergyAdjustments.apply(buckets, spec, charState, gameDatabase)
        return buckets
    }

    private fun accessibleCount(
        itemId: Int,
        invState: InventoryState,
        closetContents: Map<Int, Int>,
        storageContents: Map<Int, Int>,
        displayContents: Map<Int, Int>,
        stashContents: Map<Int, Int>,
    ): Int = inventoryCount(itemId) +
        (closetContents[itemId] ?: 0) +
        (storageContents[itemId] ?: 0) +
        (displayContents[itemId] ?: 0) +
        (stashContents[itemId] ?: 0)

    internal fun buildCandidateIds(
        invState: InventoryState,
        closetContents: Map<Int, Int>,
        storageContents: Map<Int, Int>,
        displayContents: Map<Int, Int>,
        stashContents: Map<Int, Int>,
        spec: MaximizeSpec? = null,
    ): Set<Int> = buildSet {
        addAll(invState.items.keys)
        addAll(closetContents.keys)
        addAll(storageContents.keys)
        addAll(displayContents.keys)
        addAll(stashContents.keys)
        if (spec != null) {
            addAll(EquipmentDatabase.allEquipmentItemIds())
        }
        if (spec?.allowCreatable == true) {
            for (concoction in ConcoctionDatabase.all()) {
                val item = gameDatabase.item(concoction.result) ?: continue
                if (!item.isEquipment) continue
                if (itemMeetsConstraints(item.name, spec)) add(item.id)
            }
        }
        if (spec != null && foldablesEnabled()) {
            val foldContext = MaximizerCheckedItemBuilder.Context(
                spec = spec,
                gameDatabase = gameDatabase,
                characterState = character.state.value,
                preferences = preferences,
                mallPriceManager = mallPriceManager,
                inventoryCount = ::inventoryCount,
                closetContents = closetContents,
                storageContents = storageContents,
                displayContents = displayContents,
                stashContents = stashContents,
                priceLevel = maximizerPriceLevel(),
            )
            for (itemId in toList()) {
                val name = gameDatabase.item(itemId)?.name ?: continue
                for (peerId in MaximizerCheckedItemBuilder.foldPeerItemIds(itemId, name, foldContext)) {
                    add(peerId)
                }
            }
        }
    }

    private fun foldablesEnabled(): Boolean =
        preferences?.getBoolean("maximizerFoldables", true) ?: true

    private fun scoreAssignment(
        assignment: Map<EquipmentSlot, Pair<String, Double>>,
        evaluator: Evaluator,
        baseState: CharacterState,
    ): Double = MaximizerSpeculation.scoreLoadout(
        baseState, assignment, evaluator,
    )

    private fun itemMeetsConstraints(itemName: String, spec: MaximizeSpec): Boolean {
        if (character.state.value.inBeecore &&
            Beeosity.itemBeeosity(itemName) > spec.maxBeeosity
        ) {
            return false
        }
        val price = effectivePrice(itemName)
        if (spec.maxPrice != null && price > spec.maxPrice) return false
        if (spec.minPrice != null && price < spec.minPrice) return false
        val isCreatable = gameDatabase.recipe(itemName) != null
        if (spec.allowCreatable && !isCreatable) return false
        if (spec.forbidCreatable && isCreatable) return false
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

    internal fun effectivePrice(itemName: String): Int {
        val itemId = gameDatabase.item(itemName)?.id
        val mall = itemId?.let { mallPriceManager?.getHistoricalPrice(it)?.toInt() } ?: 0
        val npc = gameDatabase.npcPrice(itemName)
        return when {
            mall > 0 && npc > 0 -> minOf(mall, npc)
            mall > 0 -> mall
            else -> npc
        }
    }

    private fun maximizerPriceLevel(): MaximizerPriceLevel =
        MaximizerPriceLevel.byIndex(preferences?.getInt("maximizerPriceLevel", 0) ?: 0)

    private suspend fun prefetchMallPrices(
        candidateIds: Set<Int>,
        spec: MaximizeSpec,
        priceLevel: MaximizerPriceLevel,
    ) {
        if (mallManager == null || mallPriceManager == null) return
        if (spec.maxPrice == null && spec.minPrice == null) return
        val idsToPrefetch = if (priceLevel != MaximizerPriceLevel.DONT_CHECK) {
            buyableEquipmentIdsForPrefetch(spec).toSet() + candidateIds
        } else {
            candidateIds
        }
        for (itemId in idsToPrefetch) {
            if (mallPriceManager.getHistoricalPrice(itemId) > 0L) continue
            val name = gameDatabase.item(itemId)?.name ?: continue
            mallManager.cheapestPrice(name)
        }
    }

    private fun buyableEquipmentIdsForPrefetch(spec: MaximizeSpec): Sequence<Int> {
        val prefs = preferences ?: return emptySequence()
        val maxPrice = spec.maxPrice ?: return emptySequence()
        val limitMode = character.state.value.limitMode
        return EquipmentDatabase.allEquipmentItemIds().filter { itemId ->
            val name = gameDatabase.item(itemId)?.name ?: return@filter false
            if (!ItemAvailability.canUseMall(
                    itemId = itemId,
                    itemName = name,
                    db = gameDatabase,
                    prefs = prefs,
                    limitMode = limitMode,
                )
            ) {
                return@filter false
            }
            effectivePrice(name) <= maxPrice
        }
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
        if (retrieveItemService != null && gameDatabase.recipe(gameDatabase.item(itemId)?.name ?: "") != null) {
            kotlinx.coroutines.runBlocking {
                retrieveItemService.retrieve(itemId, 1)
            }
            inventoryManager.fetchInventory()
        }
        return inventoryCount(itemId) >= 1
    }

    private fun inventoryCount(itemId: Int): Int =
        inventoryManager.state.value.items[itemId]?.quantity ?: 0

    private fun scoreItem(itemName: String?, evaluator: Evaluator): Double {
        if (itemName.isNullOrBlank()) return 0.0
        val entry = gameDatabase.itemModifier(itemName) ?: return 0.0
        return evaluator.getItemContribution(ModifierParser.parse(entry.modifiers))
    }

    private fun scoreFamiliarCarriedItem(
        itemName: String?,
        modifier: DoubleModifier,
        carryRaces: List<String>,
        activeRace: String?,
        familiarWeight: Int,
    ): Double {
        if (itemName.isNullOrBlank()) return 0.0
        val item = gameDatabase.item(itemName) ?: return 0.0
        val race = carryRaces.firstOrNull { FamiliarCarryRules.canCarryItem(it, item) }
            ?: activeRace?.takeIf { FamiliarCarryRules.canCarryItem(it, item) }
            ?: return 0.0
        return FamiliarCarriedScoring.score(race, itemName, modifier, gameDatabase, familiarWeight)
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
        EquipmentSlot.CODPIECE1, EquipmentSlot.CODPIECE2, EquipmentSlot.CODPIECE3,
        EquipmentSlot.CODPIECE4, EquipmentSlot.CODPIECE5 ->
            ModifierDatabase.isCodpieceGem(item.id)
    }

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
