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
import net.sourceforge.kolmafia.equipment.Modeable
import net.sourceforge.kolmafia.equipment.ModeableState
import net.sourceforge.kolmafia.equipment.OutfitCheckpoint
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.familiar.FamiliarState
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
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.DisplayCaseRequest
import net.sourceforge.kolmafia.request.EquipmentRequest
import net.sourceforge.kolmafia.request.ModeableRequest
import net.sourceforge.kolmafia.request.RestrictionListRefresh
import net.sourceforge.kolmafia.request.StandardRequest
import net.sourceforge.kolmafia.request.StorageRequest
import net.sourceforge.kolmafia.request.ThriftyRequest
import net.sourceforge.kolmafia.request.TrendyRequest
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.mall.MallPriceManager
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.quest.DynamicItemModifierSync

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
    private val modeableRequest: ModeableRequest? = null,
    private val effectManager: EffectManager? = null,
    private val characterRequest: CharacterRequest? = null,
    private val foldItemRequest: net.sourceforge.kolmafia.request.FoldItemRequest? = null,
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

    /** Best mode per modeable during [buildMaximizePlan]; feeds mode-aware [scoreItem]. */
    private var activeModeSelections: Map<Modeable, String> = emptyMap()

    /** Post-refresh active effects during maximize/speculate search (Phase 411). */
    private var searchActiveEffects: List<EffectData> = emptyList()

    /** Passive skills during maximize/speculate search (Phase 414). */
    private var searchPassiveSkillNames: Set<String> = emptySet()

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

    /** Optional CLI dispatch for non-equipment boost cmds (cast/eat/drink/use/exotic). */
    var cliExecutor: (suspend (String) -> Boolean)? = null

    /** Optional live progress sink (desktop KoLmafia.updateDisplay during speculate). */
    var progressDisplay: ((String) -> Unit)? = null

    @Volatile
    var lastSucceeded: Boolean = false
        private set

    @Volatile
    var lastMaximizeGoal: String? = null
        private set

    fun lastMaximizeSucceeded(): Boolean = lastSucceeded

    open suspend fun maximize(
        goalText: String,
        filters: Set<MaximizerFilterType> = MaximizerFilters.fromPreferences(preferences),
    ): MaximizeResult {
        lastSucceeded = false
        lastMaximizeGoal = goalText.trim().takeIf { it.isNotEmpty() }
        val plan = buildMaximizePlan(goalText, filters)
            ?: return MaximizeResult(false, goalText.trim(), 0.0, 0.0)
        writeSpecFromPlan(plan)
        val boosts = buildBoosts(plan, MaximizerEquipScope.EQUIP_NOW)
        if (!hasUsefulMaximizeResult(plan, boosts)) {
            return MaximizeResult(false, plan.goal, plan.scoreBefore, plan.scoreBefore)
        }

        val charState = character.state.value
        val checkpoint = OutfitCheckpoint.snapshot(character, equipmentRequest, gameDatabase)
        val executor = boostExecutor()
        var anyFailure = false
        var thrallSwitched: String? = null
        val (targetThrall, _) = resolveTargetThrall(plan.spec)
        if (targetThrall != null &&
            !targetThrall.equals(preferences?.getString("_currentThrall", ""), ignoreCase = true)
        ) {
            bindThrall(targetThrall)?.onSuccess {
                thrallSwitched = targetThrall
            }?.onFailure { anyFailure = true }
        }

        val equipped = mutableMapOf<EquipmentSlot, String>()
        var familiarSwitched: String? = null
        var enthronedSwitched: String? = null
        var bjornifiedSwitched: String? = null
        val modeSwitched = mutableMapOf<Modeable, String>()

        val equipmentBoosts = boosts.filter { it.isEquipment }
        var nonEquipmentBoosts = boosts.filter { !it.isEquipment }

        for (boost in equipmentBoosts) {
            if (boost.cmd.isBlank()) continue
            if (!executor.execute(boost.cmd)) {
                anyFailure = true
                break
            }
            boost.familiarRace?.let { familiarSwitched = it }
            boost.slot?.let { slot ->
                if (boost.itemName.isNotBlank()) {
                    equipped[slot] = boost.itemName
                }
            }
            for (segment in boost.cmd.split(';')) {
                val trimmed = segment.trim()
                when {
                    trimmed.startsWith("familiar ", ignoreCase = true) ->
                        familiarSwitched = trimmed.removePrefix("familiar ").trim()
                    trimmed.startsWith("enthrone ", ignoreCase = true) ->
                        enthronedSwitched = trimmed.removePrefix("enthrone ").trim()
                    trimmed.startsWith("bjornify ", ignoreCase = true) ->
                        bjornifiedSwitched = trimmed.removePrefix("bjornify ").trim()
                    else -> {
                        val space = trimmed.indexOf(' ')
                        if (space > 0) {
                            val command = trimmed.substring(0, space)
                            val mode = trimmed.substring(space + 1).trim()
                            val modeable = Modeable.entries.find { it.command.equals(command, ignoreCase = true) }
                            if (modeable != null) {
                                modeSwitched[modeable] = plan.modeSelections[modeable] ?: mode
                            }
                        }
                    }
                }
            }
        }

        var nonEquipmentExecuted = 0
        var nonEquipmentFailure = false
        var liveRescoreRan = false
        if (!anyFailure && shouldRebuildNonEquipmentLive(plan)) {
            liveRescoreRan = true
            MaximizerPostEquipRefresh.refresh(inventoryManager, effectManager)
            nonEquipmentBoosts = buildNonEquipmentBoosts(
                plan,
                MaximizerNonEquipmentBoosts.NonEquipmentBaseline.LIVE_EQUIPPED,
            )
        }
        if (!anyFailure) {
            for (boost in nonEquipmentBoosts) {
                if (boost.cmd.isBlank()) continue
                if (!executor.execute(boost.cmd)) {
                    nonEquipmentFailure = true
                    break
                }
                nonEquipmentExecuted++
            }
        }
        inventoryManager.syncCharacterEquipment()

        val resultBoosts = equipmentBoosts + nonEquipmentBoosts

        val madeChange = equipped.isNotEmpty() ||
            familiarSwitched != null ||
            enthronedSwitched != null ||
            bjornifiedSwitched != null ||
            thrallSwitched != null ||
            modeSwitched.isNotEmpty() ||
            nonEquipmentExecuted > 0
        if (anyFailure || !madeChange) {
            checkpoint.restore()
            return MaximizeResult(false, plan.goal, plan.scoreBefore, plan.scoreBefore)
        }
        if (nonEquipmentFailure) {
            return MaximizeResult(
                success = false,
                goal = plan.goal,
                scoreBefore = plan.scoreBefore,
                scoreAfter = plan.scoreAfter,
                equipped = equipped,
                familiarSwitched = familiarSwitched,
                enthronedSwitched = enthronedSwitched,
                bjornifiedSwitched = bjornifiedSwitched,
                thrallSwitched = thrallSwitched,
                modeSwitched = modeSwitched,
                boosts = boostsWithStatus(resultBoosts, plan.searchStatus),
            )
        }

        val scoreAfter = if (liveRescoreRan) {
            computeLiveScoreAfter(plan)
        } else {
            plan.scoreAfter
        }
        if (liveRescoreRan) {
            writeSpecFromLiveState()
        }
        val result = MaximizeResult(
            success = true,
            goal = plan.goal,
            scoreBefore = plan.scoreBefore,
            scoreAfter = scoreAfter,
            equipped = equipped,
            familiarSwitched = familiarSwitched,
            enthronedSwitched = enthronedSwitched,
            bjornifiedSwitched = bjornifiedSwitched,
            thrallSwitched = thrallSwitched,
            modeSwitched = modeSwitched,
            boosts = boostsWithStatus(resultBoosts, plan.searchStatus),
        )
        lastSucceeded = result.success
        return result
    }

    /** Speculate-only loadout search — no equip side effects. */
    open suspend fun speculate(
        goalText: String,
        filters: Set<MaximizerFilterType> = MaximizerFilters.fromPreferences(preferences),
    ): List<String> {
        lastSucceeded = false
        val plan = buildMaximizePlan(goalText, filters)
            ?: return listOf("Invalid goal: ${goalText.trim()}")
        if (MaximizerFilterType.EQUIP in plan.filters) {
            lastSucceeded = !plan.spec.evaluator.failed
        }
        writeSpecFromPlan(plan)
        val boosts = buildBoosts(plan, MaximizerEquipScope.SPECULATE)
        if (!hasUsefulMaximizeResult(plan, boosts)) {
            return listOf("No improvement for ${plan.goal}")
        }
        val lines = mutableListOf<String>()
        for (boost in boosts) {
            lines += boost.text
        }
        val card = MaximizerCardSelection.cardForOffhand(
            plan.bestPerSlot[EquipmentSlot.OFFHAND]?.first,
            plan.cardInSleeve,
            character.state.value,
        )
        if (card != null) {
            lines += "# card $card"
        }
        plan.searchStatus?.statusLines()?.let { lines.addAll(it) }
        lines += "Score: ${plan.scoreBefore} -> ${plan.scoreAfter}"
        return lines
    }

    private suspend fun buildBoosts(plan: MaximizePlan, equipScope: MaximizerEquipScope): List<MaximizerBoost> {
        val equipmentBoosts = buildEquipmentBoosts(plan, equipScope)
        if (MaximizerFilters.isEquipOnly(plan.filters)) {
            return equipmentBoosts
        }
        return equipmentBoosts + buildNonEquipmentBoosts(
            plan,
            MaximizerNonEquipmentBoosts.NonEquipmentBaseline.PLAN_OVERLAY,
        )
    }

    private suspend fun buildEquipmentBoosts(
        plan: MaximizePlan,
        equipScope: MaximizerEquipScope,
    ): List<MaximizerBoost> {
        if (MaximizerFilterType.EQUIP !in plan.filters) return emptyList()
        val charState = character.state.value
        val carryFamiliars = FamiliarCarryRules.carryRaces(
            plan.spec,
            charState.familiarName.takeIf { it.isNotBlank() },
        )
        val (_, thrallBonus) = resolveTargetThrall(plan.spec)
        val familiarSwitch = plan.familiarSwitch ?: resolveFamiliarSwitch(plan.spec)
        return MaximizerEmitSlot.buildBoosts(
            MaximizerEmitSlot.Context(
                plan = plan.toEmitPlan(familiarSwitch),
                charState = charState,
                inventory = plan.inventorySnapshot,
                inventoryCount = ::inventoryCount,
                gameDatabase = gameDatabase,
                preferences = preferences,
                mallPriceManager = mallPriceManager,
                priceLevel = maximizerPriceLevel(),
                equipScope = equipScope,
                carryFamiliars = carryFamiliars,
                thrallBonus = thrallBonus,
            ),
        )
    }

    private suspend fun buildNonEquipmentBoosts(
        plan: MaximizePlan,
        baseline: MaximizerNonEquipmentBoosts.NonEquipmentBaseline,
    ): List<MaximizerBoost> =
        MaximizerNonEquipmentBoosts.build(nonEquipmentContext(plan, baseline)).sorted()

    private suspend fun nonEquipmentContext(
        plan: MaximizePlan,
        baseline: MaximizerNonEquipmentBoosts.NonEquipmentBaseline,
    ): MaximizerNonEquipmentBoosts.Context {
        val charState = character.state.value
        val carryFamiliars = FamiliarCarryRules.carryRaces(
            plan.spec,
            charState.familiarName.takeIf { it.isNotBlank() },
        )
        val (_, thrallBonus) = resolveTargetThrall(plan.spec)
        val familiarSwitch = plan.familiarSwitch ?: resolveFamiliarSwitch(plan.spec)
        return MaximizerNonEquipmentBoosts.Context(
            plan = plan.toEmitPlan(familiarSwitch),
            charState = charState,
            activeEffects = effectManager?.state?.value?.effects.orEmpty(),
            passiveSkillNames = MaximizerPassiveSkills.namesFrom(
                skillManager?.state?.value?.skills.orEmpty(),
            ),
            inventory = plan.inventorySnapshot,
            inventoryCount = ::inventoryCount,
            gameDatabase = gameDatabase,
            preferences = preferences,
            mallPriceManager = mallPriceManager,
            priceLevel = maximizerPriceLevel(),
            carryFamiliars = carryFamiliars,
            thrallBonus = thrallBonus,
            skillManager = skillManager,
            familiarManager = familiarManager,
            standardRequest = standardRequest,
            filters = plan.filters,
            includeAll = preferences?.getBoolean("maximizerIncludeAll", false) == true,
            baseline = baseline,
        )
    }

    private fun shouldRebuildNonEquipmentLive(plan: MaximizePlan): Boolean {
        if (MaximizerFilterType.EQUIP !in plan.filters) return false
        if (MaximizerFilters.isEquipOnly(plan.filters)) return false
        return true
    }

    private suspend fun writeSpecFromPlan(plan: MaximizePlan) {
        val charState = character.state.value
        val carryFamiliars = FamiliarCarryRules.carryRaces(
            plan.spec,
            charState.familiarName.takeIf { it.isNotBlank() },
        )
        val familiarSwitch = plan.familiarSwitch ?: resolveFamiliarSwitch(plan.spec)
        MaximizerSpecWriteBack.writeFromPlan(
            plan = plan.toEmitPlan(familiarSwitch),
            charState = charState,
            activeEffects = effectManager?.state?.value?.effects.orEmpty(),
            passiveSkillNames = MaximizerPassiveSkills.namesFrom(
                skillManager?.state?.value?.skills.orEmpty(),
            ),
            carryFamiliars = carryFamiliars,
            gameDatabase = gameDatabase,
            preferences = preferences,
        )
    }

    private fun writeSpecFromLiveState() {
        MaximizerSpecWriteBack.writeFromLiveState(
            charState = character.state.value,
            activeEffects = effectManager?.state?.value?.effects.orEmpty(),
            passiveSkillNames = MaximizerPassiveSkills.namesFrom(
                skillManager?.state?.value?.skills.orEmpty(),
            ),
            gameDatabase = gameDatabase,
            preferences = preferences,
        )
    }

    private fun computeLiveScoreAfter(plan: MaximizePlan): Double {
        val (_, thrallBonus) = resolveTargetThrall(plan.spec)
        return MaximizerSpeculation.scorePostEquipmentLive(
            charState = character.state.value,
            evaluator = plan.spec.evaluator,
            activeEffects = effectManager?.state?.value?.effects.orEmpty(),
            passiveSkillNames = MaximizerPassiveSkills.namesFrom(
                skillManager?.state?.value?.skills.orEmpty(),
            ),
            preferences = preferences,
            gameDatabase = gameDatabase,
            thrallBonus = thrallBonus,
        )
    }

    private fun hasUsefulMaximizeResult(plan: MaximizePlan, boosts: List<MaximizerBoost>): Boolean {
        if (hasPositiveNonEquipmentBoosts(boosts)) return true
        if (MaximizerFilterType.EQUIP !in plan.filters) return false
        return plan.scoreAfter > plan.scoreBefore
    }

    private fun hasPositiveNonEquipmentBoosts(boosts: List<MaximizerBoost>): Boolean =
        boosts.any { !it.isEquipment && it.delta > 0.0 }

    private fun statusBoosts(searchStatus: MaximizerSearchStatus?): List<MaximizerBoost> =
        searchStatus?.statusLines().orEmpty().map { line ->
            MaximizerBoost(cmd = "", text = line, delta = 0.0, isEquipment = false)
        }

    private fun boostsWithStatus(
        boosts: List<MaximizerBoost>,
        searchStatus: MaximizerSearchStatus?,
    ): List<MaximizerBoost> = statusBoosts(searchStatus) + boosts

    private fun boostExecutor(): MaximizerBoostExecutor =
        MaximizerBoostExecutor(
            gameDatabase = gameDatabase,
            inventoryManager = inventoryManager,
            equipmentRequest = equipmentRequest,
            closetRequest = closetRequest,
            storageRequest = storageRequest,
            displayCaseRequest = displayCaseRequest,
            clanStashRequest = clanStashRequest,
            familiarManager = familiarManager,
            modeableRequest = modeableRequest,
            retrieveItemService = retrieveItemService,
            mallManager = mallManager,
            preferences = preferences,
            character = character,
            cliExecutor = cliExecutor,
            foldItemRequest = foldItemRequest,
        )

    private suspend fun buildMaximizePlan(
        goalText: String,
        filters: Set<MaximizerFilterType>,
    ): MaximizePlan? {
        MaximizerContinuation.forceContinue()
        MaximizerProgress.reset()
        MaximizerProgress.sink = progressDisplay ?: {}
        activeModeSelections = emptyMap()
        searchActiveEffects = emptyList()
        searchPassiveSkillNames = emptySet()
        val goal = goalText.trim()
        val spec = MaximizeGoal.parseSpec(goal) ?: return null
        MaximizerPreSearchRefresh.refresh(
            inventoryManager = inventoryManager,
            effectManager = effectManager,
            character = character,
            characterRequest = characterRequest,
            preferences = preferences,
            skillManager = skillManager,
            familiarManager = familiarManager,
        )
        val activeEffects = effectManager?.state?.value?.effects.orEmpty()
        searchActiveEffects = activeEffects
        val charState = character.state.value
        val invState = inventoryManager.state.value
        val closetContents = closetRequest?.fetchContents().orEmpty()
        val storageContents = storageRequest?.fetchContents().orEmpty()
        val displayContents = displayCaseRequest?.fetchContents().orEmpty()
        val stashContents = clanStashRequest?.fetchContents().orEmpty()
        val passiveSkillNames = MaximizerPassiveSkills.resolve(
            skillManager?.state?.value?.skills.orEmpty(),
            buildCheckContext(
                charState = charState,
                invState = invState,
                activeEffects = activeEffects,
                closetContents = closetContents,
                storageContents = storageContents,
                stashContents = stashContents,
            ),
            gameDatabase,
        )
        searchPassiveSkillNames = passiveSkillNames
        val inventorySnap = inventorySnapshot(
            closetContents, storageContents, displayContents, stashContents,
        )
        val accessibleCount: (Int) -> Int = { itemId ->
            inventoryCount(itemId) +
                (closetContents[itemId] ?: 0) +
                (storageContents[itemId] ?: 0) +
                (displayContents[itemId] ?: 0) +
                (stashContents[itemId] ?: 0)
        }
        if (!spec.evaluator.resolvePlumberTools(charState, accessibleCount, gameDatabase)) {
            return null
        }
        val effectiveSpec = spec.withCarryEquipment()

        val scoreBefore = MaximizerSpeculation.scoreLoadout(
            charState, charState.equipment.mapValues { (_, name) -> name to 0.0 },
            effectiveSpec.evaluator,
            scoreFamiliarBonuses(charState, effectiveSpec.primary),
            scoreCurrentThrall(effectiveSpec.primary),
            preferences = preferences,
            maxBeeosity = effectiveSpec.maxBeeosity,
            activeEffects = activeEffects,
            passiveSkillNames = passiveSkillNames,
        )

        if (MaximizerFilterType.EQUIP !in filters) {
            val bestPerSlot = currentEquipmentBestPerSlot(effectiveSpec, charState)
            return MaximizePlan(
                goal = goal,
                spec = effectiveSpec,
                scoreBefore = scoreBefore,
                scoreAfter = scoreBefore,
                bestPerSlot = bestPerSlot,
                filters = filters,
                inventorySnapshot = inventorySnap,
                searchStatus = null,
            )
        }

        val candidateIds = buildCandidateIds(
            invState, closetContents, storageContents, displayContents, stashContents, effectiveSpec,
        )
        val priceLevel = maximizerPriceLevel()
        prefetchMallPrices(candidateIds, effectiveSpec, priceLevel)
        val (rankedBuckets, autoContext) = buildRankedBuckets(
            effectiveSpec, candidateIds, invState,
            closetContents, storageContents, displayContents, stashContents,
            activeEffects = activeEffects,
            passiveSkillNames = passiveSkillNames,
        )
        var bestPerSlot = findBestPerSlot(
            effectiveSpec, charState, rankedBuckets,
        )
        val comboBudget = ComboBudget(
            preferences?.getInt(COMBINATION_LIMIT_PREF, DEFAULT_COMBO_LIMIT) ?: DEFAULT_COMBO_LIMIT,
        )
        val carryFamiliars = FamiliarCarryRules.carryRaces(
            effectiveSpec,
            charState.familiarName.takeIf { it.isNotBlank() },
        )
        val modeSelections = MaximizerModeSelection.selectBestModes(
            spec = effectiveSpec,
            charState = charState,
            rankedBuckets = rankedBuckets,
            bestPerSlot = bestPerSlot,
            preferences = preferences,
            familiarBonus = 0.0,
            thrallBonus = 0.0,
            carryFamiliars = carryFamiliars,
            gameDatabase = gameDatabase,
            activeEffects = activeEffects,
            passiveSkillNames = passiveSkillNames,
        )
        activeModeSelections = modeSelections
        bestPerSlot = seedModeableBestPerSlot(bestPerSlot, effectiveSpec, modeSelections)
        bestPerSlot = refineAccessoryCombinations(
            effectiveSpec, charState, rankedBuckets,
            bestPerSlot, comboBudget, modeSelections, carryFamiliars, activeEffects, passiveSkillNames,
        )
        bestPerSlot = refineWeaponOffhandCombinations(
            effectiveSpec, charState, rankedBuckets,
            bestPerSlot, comboBudget, modeSelections, carryFamiliars, activeEffects, passiveSkillNames,
        )
        bestPerSlot = refineArmorCombinations(
            effectiveSpec, charState, rankedBuckets,
            bestPerSlot, comboBudget, modeSelections, carryFamiliars, activeEffects, passiveSkillNames,
        )
        val (targetThrall, thrallBonus) = resolveTargetThrall(effectiveSpec)
        val usableSwitch = usableSwitchFamiliars(effectiveSpec)
        val scoreFamiliarFn: (String?) -> Double = { familiarRace ->
            familiarRace?.let { scoreFamiliarList(listOf(it), effectiveSpec.primary) } ?: 0.0
        }
        val discoveryContext = MaximizerCarriedFamiliars.DiscoveryContext(
            familiarState = familiarManager?.state?.value ?: FamiliarState(),
            charState = charState,
            preferences = preferences,
            excludeRaces = MaximizerCarriedFamiliars.defaultExcludeRaces(charState),
            scoreFamiliar = { race -> scoreFamiliarFn(race) },
        )
        val usableEnthrone = usableEnthroneFamiliars(effectiveSpec).ifEmpty {
            if (MaximizerCarriedFamiliars.needsEnthroneDiscovery(
                    effectiveSpec, charState, rankedBuckets, bestPerSlot,
                )
            ) {
                MaximizerCarriedFamiliars.discoverCarryFamiliars(discoveryContext)
            } else {
                emptyList()
            }
        }
        val usableBjorn = usableBjornFamiliars(effectiveSpec).ifEmpty {
            if (MaximizerCarriedFamiliars.needsBjornDiscovery(
                    effectiveSpec, charState, rankedBuckets, bestPerSlot,
                )
            ) {
                MaximizerCarriedFamiliars.discoverCarryFamiliars(discoveryContext)
            } else {
                emptyList()
            }
        }
        val survivingOutfits = MaximizerOutfitSpeculation.survivingUsefulOutfits(
            rankedBuckets,
            autoContext.usefulOutfits,
        )
        val countForByName = buildCountLookup(rankedBuckets)
        val countForById = buildCountLookupById(rankedBuckets)
        val bestCard = MaximizerCardSelection.selectBestCard(
            spec = effectiveSpec,
            charState = charState,
            rankedBuckets = rankedBuckets,
            countFor = countForById,
            modeOverrides = modeSelections,
            preferences = preferences,
            carryFamiliars = carryFamiliars,
            gameDatabase = gameDatabase,
            activeEffects = activeEffects,
            passiveSkillNames = passiveSkillNames,
        )
        val scoringOptions = MaximizerScoringOptions(
            bestModes = modeSelections,
            carryFamiliars = carryFamiliars,
            gameDatabase = gameDatabase,
            cardInSleeve = bestCard,
            countFor = countForByName,
            foldablesEnabled = foldablesEnabled(),
            activeEffects = activeEffects,
            passiveSkillNames = passiveSkillNames,
        )
        val tryAllResult = MaximizerFamiliarSpeculation.tryAll(
            spec = effectiveSpec,
            charState = charState,
            survivingOutfits = survivingOutfits,
            rankedBuckets = rankedBuckets,
            refineBestPerSlot = bestPerSlot,
            comboBudget = comboBudget,
            thrallBonus = thrallBonus,
            gameDatabase = gameDatabase,
            usableSwitchFamiliars = usableSwitch,
            usableEnthroneFamiliars = usableEnthrone,
            usableBjornFamiliars = usableBjorn,
            buildCandidates = { familiarRace, isSwitchPass ->
                val familiarBucketIndex = if (isSwitchPass && familiarRace != null) {
                    effectiveSpec.switchFamiliars.indexOfFirst { it.equals(familiarRace, ignoreCase = true) }
                        .takeIf { it >= 0 }
                } else {
                    null
                }
                val carryRaces = if (isSwitchPass) {
                    emptyList()
                } else {
                    FamiliarCarryRules.carryRaces(effectiveSpec, familiarRace)
                }
                val familiarCarryScorer = if (carryRaces.isNotEmpty()) {
                    { itemName: String?, mod: DoubleModifier ->
                        scoreFamiliarCarriedItem(
                            itemName, mod, carryRaces, familiarRace, charState.familiarWeight,
                        )
                    }
                } else {
                    null
                }
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
                    familiarBucketIndex = familiarBucketIndex,
                )
            },
            scoreFamiliar = scoreFamiliarFn,
            priceFor = ::effectivePrice,
            scoring = scoringOptions,
            preferences = preferences,
        )
        bestPerSlot = tryAllResult.bestPerSlot
        val familiarSwitch = tryAllResult.familiarSwitch
        bestPerSlot = applyEquipRequired(effectiveSpec, bestPerSlot, charState.equipment)
        var enthronedRace = tryAllResult.enthronedRace
        var bjornifiedRace = tryAllResult.bjornifiedRace
        if (enthronedRace == null &&
            bestPerSlot[EquipmentSlot.HAT]?.first.equals(CROWN_OF_THRONES, ignoreCase = true)
        ) {
            enthronedRace = resolveEnthronedFamiliar(effectiveSpec)
                ?: usableEnthrone.firstOrNull()
        }
        if (bjornifiedRace == null &&
            bestPerSlot[EquipmentSlot.CONTAINER]?.first.equals(BUDDY_BJORN, ignoreCase = true)
        ) {
            bjornifiedRace = resolveBjornifiedFamiliar(effectiveSpec)
                ?: usableBjorn.firstOrNull()
        }
        val scoreAfter = MaximizerSpeculation.scoreLoadout(
            charState, bestPerSlot, effectiveSpec.evaluator,
            MaximizerFamiliarSpeculation.totalFamiliarBonus(
                tryAllResult.copy(enthronedRace = enthronedRace, bjornifiedRace = bjornifiedRace),
                scoreFamiliarFn,
            ),
            thrallBonus,
            bestModes = modeSelections,
            carryFamiliars = carryFamiliars,
            gameDatabase = gameDatabase,
            cardInSleeve = MaximizerCardSelection.cardForOffhand(
                bestPerSlot[EquipmentSlot.OFFHAND]?.first, bestCard, charState,
            ),
            preferences = preferences,
            maxBeeosity = effectiveSpec.maxBeeosity,
            activeEffects = activeEffects,
            passiveSkillNames = passiveSkillNames,
        )
        if (charState.inBeecore &&
            loadoutBeeosity(charState.equipment, bestPerSlot) > effectiveSpec.maxBeeosity
        ) {
            return MaximizePlan(
                goal, effectiveSpec, scoreBefore, scoreBefore, emptyMap(),
                filters = filters,
                inventorySnapshot = inventorySnap,
                searchStatus = finalizeSearchStatus(comboBudget, scoreBefore, effectiveSpec.evaluator.failed),
            )
        }
        if (effectiveSpec.evaluator.failed) {
            return MaximizePlan(
                goal, effectiveSpec, scoreBefore, scoreBefore, emptyMap(),
                filters = filters,
                inventorySnapshot = inventorySnap,
                searchStatus = finalizeSearchStatus(comboBudget, scoreBefore, true),
            )
        }
        return MaximizePlan(
            goal = goal,
            spec = effectiveSpec,
            scoreBefore = scoreBefore,
            scoreAfter = scoreAfter,
            bestPerSlot = withEmitSubSlots(bestPerSlot, charState, bestCard),
            familiarSwitch = familiarSwitch,
            enthronedRace = enthronedRace,
            bjornifiedRace = bjornifiedRace,
            modeSelections = modeSelections,
            cardInSleeve = bestCard,
            filters = filters,
            inventorySnapshot = inventorySnap,
            searchStatus = finalizeSearchStatus(comboBudget, scoreAfter, effectiveSpec.evaluator.failed),
        )
    }

    private fun finalizeSearchStatus(
        budget: ComboBudget,
        score: Double,
        failed: Boolean,
    ): MaximizerSearchStatus {
        MaximizerProgress.showFinal(budget.combinationsChecked, score, failed)
        return MaximizerSearchStatus.from(budget, MaximizerProgress.lastMessage)
    }

    private fun reportProgressImprovement(
        budget: ComboBudget,
        score: Double,
        spec: MaximizeSpec,
    ) {
        MaximizerProgress.maybeShow(
            budget.combinationsChecked,
            score,
            spec.evaluator.failed,
        )
    }

    private fun currentEquipmentBestPerSlot(
        spec: MaximizeSpec,
        charState: CharacterState,
    ): Map<EquipmentSlot, Pair<String, Double>> {
        val bestPerSlot = mutableMapOf<EquipmentSlot, Pair<String, Double>>()
        for (slot in equipSlots) {
            val name = charState.equipment[slot].orEmpty()
            if (name.isBlank()) continue
            bestPerSlot[slot] = name to scoreItem(name, spec.evaluator)
        }
        return bestPerSlot
    }

    private fun withEmitSubSlots(
        bestPerSlot: Map<EquipmentSlot, Pair<String, Double>>,
        charState: CharacterState,
        cardInSleeve: String?,
    ): Map<EquipmentSlot, Pair<String, Double>> {
        val updated = bestPerSlot.toMutableMap()
        for (slot in EquipmentSlot.SUB_SLOTS) {
            charState.equipment[slot]?.takeIf { it.isNotBlank() }?.let { updated[slot] = it to 0.0 }
        }
        cardInSleeve?.takeIf { it.isNotBlank() }?.let {
            updated[EquipmentSlot.CARDSLEEVE] = it to 0.0
        }
        return updated
    }

    private fun inventorySnapshot(
        closetContents: Map<Int, Int>,
        storageContents: Map<Int, Int>,
        displayContents: Map<Int, Int>,
        stashContents: Map<Int, Int>,
    ): MaximizerEmitSlot.InventorySnapshot =
        MaximizerEmitSlot.InventorySnapshot(
            closetContents = closetContents,
            storageContents = storageContents,
            displayContents = displayContents,
            stashContents = stashContents,
        )

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
        val filters: Set<MaximizerFilterType> = MaximizerFilters.allEnabled(),
        val familiarSwitch: String? = null,
        val enthronedRace: String? = null,
        val bjornifiedRace: String? = null,
        val modeSelections: Map<Modeable, String> = emptyMap(),
        val cardInSleeve: String? = null,
        val inventorySnapshot: MaximizerEmitSlot.InventorySnapshot = MaximizerEmitSlot.InventorySnapshot(),
        val searchStatus: MaximizerSearchStatus? = null,
    ) {
        fun toEmitPlan(familiarSwitchOverride: String? = familiarSwitch): MaximizerEmitSlot.Plan =
            MaximizerEmitSlot.Plan(
            goal = goal,
            spec = spec,
            scoreBefore = scoreBefore,
            scoreAfter = scoreAfter,
            bestPerSlot = bestPerSlot,
            familiarSwitch = familiarSwitchOverride,
            enthronedRace = enthronedRace,
            bjornifiedRace = bjornifiedRace,
            modeSelections = modeSelections,
            cardInSleeve = cardInSleeve,
        )
    }

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

    private suspend fun usableSwitchFamiliars(spec: MaximizeSpec): List<String> {
        if (spec.switchFamiliars.isEmpty()) return emptyList()
        val familiarState = familiarManager?.state?.value ?: return emptyList()
        val charState = character.state.value
        RestrictionListRefresh.ensureInitialized(
            charState,
            standardRequest,
            thriftyRequest,
            trendyRequest,
        )
        return spec.switchFamiliars.filter { race ->
            FamiliarUsability.usableByRace(
                familiarState,
                race,
                charState,
                preferences,
            ) != null
        }
    }

    private suspend fun usableEnthroneFamiliars(spec: MaximizeSpec): List<String> =
        usableGoalFamiliars(spec.enthronedFamiliars)

    private suspend fun usableBjornFamiliars(spec: MaximizeSpec): List<String> =
        usableGoalFamiliars(spec.bjornifiedFamiliars)

    private suspend fun usableGoalFamiliars(races: List<String>): List<String> {
        if (races.isEmpty()) return emptyList()
        val familiarState = familiarManager?.state?.value ?: return emptyList()
        val charState = character.state.value
        RestrictionListRefresh.ensureInitialized(
            charState,
            standardRequest,
            thriftyRequest,
            trendyRequest,
        )
        return races.filter { race ->
            !race.equals("none", ignoreCase = true) &&
                FamiliarUsability.usableByRace(
                    familiarState,
                    race,
                    charState,
                    preferences,
                ) != null
        }
    }

    private suspend fun resolveFamiliarSwitch(spec: MaximizeSpec): String? {
        val usable = usableSwitchFamiliars(spec)
        if (usable.isEmpty()) return null
        var bestRace: String? = null
        var bestScore = Double.NEGATIVE_INFINITY
        var bestTie = Double.NEGATIVE_INFINITY
        for (race in usable) {
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
        rankedBuckets: SlotList<MaximizerRankedItem>,
    ): Map<EquipmentSlot, Pair<String, Double>> {
        val equipment = charState.equipment
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
                val itemScore = scoreItem(name, spec.evaluator)
                if (itemScore > bestScore) {
                    bestScore = itemScore
                    bestName = name
                } else if (bestName.isBlank() && itemScore == bestScore && score > 0.0) {
                    bestName = name
                    bestScore = itemScore
                }
            }
            if (bestName.isNotBlank()) {
                bestPerSlot[slot] = bestName to bestScore
                usedItems.add(bestName)
            }
        }
        return bestPerSlot
    }

    /** Pin pre-selected modeables into greedy slots using mode-aware scores. */
    private fun seedModeableBestPerSlot(
        bestPerSlot: Map<EquipmentSlot, Pair<String, Double>>,
        spec: MaximizeSpec,
        modeSelections: Map<Modeable, String>,
    ): Map<EquipmentSlot, Pair<String, Double>> {
        if (modeSelections.isEmpty()) return bestPerSlot
        val updated = bestPerSlot.toMutableMap()
        for ((modeable, _) in modeSelections) {
            val score = scoreItem(modeable.itemName, spec.evaluator, modeSelections)
            val current = updated[modeable.slot]?.second ?: Double.NEGATIVE_INFINITY
            if (score >= current) {
                updated[modeable.slot] = modeable.itemName to score
            }
        }
        return updated
    }

    private fun refineAccessoryCombinations(
        spec: MaximizeSpec,
        charState: CharacterState,
        rankedBuckets: SlotList<MaximizerRankedItem>,
        greedy: Map<EquipmentSlot, Pair<String, Double>>,
        budget: ComboBudget,
        bestModes: Map<Modeable, String> = emptyMap(),
        carryFamiliars: List<String> = emptyList(),
        activeEffects: List<EffectData> = emptyList(),
        passiveSkillNames: Set<String> = searchPassiveSkillNames,
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
        var bestScore = scoreAssignment(
            greedy, spec.evaluator, charState, bestModes, carryFamiliars, spec.maxBeeosity, activeEffects, passiveSkillNames,
        )
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
                    val score = scoreAssignment(
                        combo, spec.evaluator, charState, bestModes, carryFamiliars, spec.maxBeeosity, activeEffects, passiveSkillNames,
                    )
                    if (score > bestScore) {
                        bestScore = score
                        bestAssignment = combo
                        reportProgressImprovement(budget, bestScore, spec)
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
        bestModes: Map<Modeable, String> = emptyMap(),
        carryFamiliars: List<String> = emptyList(),
        activeEffects: List<EffectData> = emptyList(),
        passiveSkillNames: Set<String> = searchPassiveSkillNames,
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
        if (topOffhands.isEmpty()) return greedy

        var bestAssignment = greedy
        var bestScore = scoreAssignment(
            greedy, spec.evaluator, charState, bestModes, carryFamiliars, spec.maxBeeosity, activeEffects, passiveSkillNames,
        )
        if (topWeapons.isEmpty()) {
            for (offhand in topOffhands) {
                if (budget.tick()) return bestAssignment
                val combo = nonWeaponOffhand + mapOf(
                    EquipmentSlot.OFFHAND to (offhand.first to offhand.second),
                )
                val score = scoreAssignment(
                    combo, spec.evaluator, charState, bestModes, carryFamiliars, spec.maxBeeosity, activeEffects, passiveSkillNames,
                )
                if (score > bestScore) {
                    bestScore = score
                    bestAssignment = combo
                    reportProgressImprovement(budget, bestScore, spec)
                }
            }
            return bestAssignment
        }

        for (weapon in topWeapons) {
            for (offhand in topOffhands) {
                if (weapon.first == offhand.first) continue
                if (budget.tick()) return bestAssignment
                val combo = nonWeaponOffhand + mapOf(
                    EquipmentSlot.WEAPON to (weapon.first to weapon.second),
                    EquipmentSlot.OFFHAND to (offhand.first to offhand.second),
                )
                val score = scoreAssignment(
                    combo, spec.evaluator, charState, bestModes, carryFamiliars, spec.maxBeeosity, activeEffects, passiveSkillNames,
                )
                if (score > bestScore) {
                    bestScore = score
                    bestAssignment = combo
                    reportProgressImprovement(budget, bestScore, spec)
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
        bestModes: Map<Modeable, String> = emptyMap(),
        carryFamiliars: List<String> = emptyList(),
        activeEffects: List<EffectData> = emptyList(),
        passiveSkillNames: Set<String> = searchPassiveSkillNames,
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
        var bestScore = scoreAssignment(
            greedy, spec.evaluator, charState, bestModes, carryFamiliars, spec.maxBeeosity, activeEffects, passiveSkillNames,
        )
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
                    val score = scoreAssignment(
                        combo, spec.evaluator, charState, bestModes, carryFamiliars, spec.maxBeeosity, activeEffects,
                    )
                    if (score > bestScore) {
                        bestScore = score
                        bestAssignment = combo
                        reportProgressImprovement(budget, bestScore, spec)
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
        activeEffects: List<EffectData> = searchActiveEffects,
        passiveSkillNames: Set<String> = searchPassiveSkillNames,
    ): Pair<SlotList<MaximizerRankedItem>, MaximizerAutoContext> {
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
        val autoContext = MaximizerAutoContext.from(spec.evaluator)
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
            scoreItem = { name, ev -> scoreItem(name, ev, activeEffects = activeEffects, passiveSkillNames = passiveSkillNames) },
            itemMeetsConstraints = ::itemMeetsConstraints,
            priceFor = ::effectivePrice,
            autoContext = autoContext,
            switchFamiliars = spec.switchFamiliars,
            familiarWeight = charState.familiarWeight,
            charState = charState,
            preferences = preferences,
            hasSkill = { skillId ->
                skillManager?.state?.value?.skills?.any { it.id == skillId } == true
            },
        )
        MaximizerSynergyAdjustments.apply(buckets, spec, charState, gameDatabase)
        MaximizerOutfitAdjustments.apply(buckets, autoContext.usefulOutfits, spec, charState, gameDatabase)
        return buckets to autoContext
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
        bestModes: Map<Modeable, String> = emptyMap(),
        carryFamiliars: List<String> = emptyList(),
        maxBeeosity: Int = 2,
        activeEffects: List<EffectData> = emptyList(),
        passiveSkillNames: Set<String> = searchPassiveSkillNames,
    ): Double = MaximizerSpeculation.scoreLoadout(
        baseState, assignment, evaluator,
        bestModes = bestModes,
        carryFamiliars = carryFamiliars,
        gameDatabase = gameDatabase,
        preferences = preferences,
        maxBeeosity = maxBeeosity,
        activeEffects = activeEffects,
        passiveSkillNames = passiveSkillNames,
    )

    private fun buildCountLookup(rankedBuckets: SlotList<MaximizerRankedItem>): (String) -> Int {
        val byName = MaximizerEquipmentEnumerator.allRankedItems(rankedBuckets)
            .associate { it.name.lowercase() to it.accessibleCount }
        return { name -> byName[name.lowercase()] ?: 1 }
    }

    private fun buildCountLookupById(rankedBuckets: SlotList<MaximizerRankedItem>): (Int) -> Int {
        val byId = MaximizerEquipmentEnumerator.allRankedItems(rankedBuckets)
            .associate { it.itemId to it.accessibleCount }
        return { id -> byId[id] ?: 0 }
    }

    private fun itemMeetsConstraints(itemName: String, spec: MaximizeSpec): Boolean {
        if (spec.evaluator.isNegEquip(itemName)) return false
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
        // Per-item: only reject VIOLATES (forbidden booleans). Required booleans are
        // loadout-level (desktop getScore / checkConstraints MEETS ranking).
        val entry = gameDatabase.itemModifier(itemName) ?: return true
        val mods = ModifierParser.parse(entry.modifiers)
        return spec.evaluator.checkConstraints(mods) != Evaluator.Constraint.VIOLATES
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

    private fun passesEmitMallCheck(itemId: Int, itemName: String, spec: MaximizeSpec): Boolean {
        val priceLevel = maximizerPriceLevel()
        val maxPrice = spec.maxPrice?.toLong() ?: return true
        val charState = character.state.value
        val checkedContext = MaximizerCheckedItemBuilder.Context(
            spec = spec,
            gameDatabase = gameDatabase,
            characterState = charState,
            preferences = preferences,
            mallPriceManager = mallPriceManager,
            inventoryCount = ::inventoryCount,
            closetContents = emptyMap(),
            storageContents = emptyMap(),
            displayContents = emptyMap(),
            stashContents = emptyMap(),
            priceLevel = priceLevel,
        )
        val checked = MaximizerCheckedItemBuilder.build(itemId, itemName, checkedContext)
        val mallPrice = mallPriceManager?.getMallPrice(itemId) ?: 0L
        val historicalPrice = mallPriceManager?.getHistoricalPrice(itemId) ?: 0L
        return checked.passesEmitMallCheck(
            priceLevel = priceLevel,
            maxPrice = maxPrice,
            mallPrice = mallPrice,
            historicalPrice = historicalPrice,
            tradeable = net.sourceforge.kolmafia.data.ItemDatabase.isTradeable(itemId),
        )
    }

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

    private fun scoreItem(
        itemName: String?,
        evaluator: Evaluator,
        bestModes: Map<Modeable, String> = emptyMap(),
        activeEffects: List<EffectData> = searchActiveEffects,
        passiveSkillNames: Set<String> = searchPassiveSkillNames,
    ): Double {
        if (itemName.isNullOrBlank()) return 0.0
        val itemData = gameDatabase.item(itemName) ?: return 0.0
        val modes = bestModes.ifEmpty { activeModeSelections }
        val charState = character.state.value
        if (MaximizerSubSlotItems.needsSubSlotPreservation(itemData.id)) {
            val slot = slotForItem(itemData) ?: return 0.0
            val assignment = mutableMapOf<EquipmentSlot, Pair<String, Double>>()
            assignment[slot] = itemName to 0.0
            MaximizerSubSlotPreservation.applyParentPreservation(itemData.id, charState, assignment)
            return MaximizerSpeculation.scoreLoadout(
                baseState = charState,
                assignment = assignment,
                evaluator = evaluator,
                bestModes = modes,
                carryFamiliars = emptyList(),
                gameDatabase = gameDatabase,
                preferences = preferences,
                validateEquipment = false,
                activeEffects = activeEffects,
                passiveSkillNames = passiveSkillNames,
            )
        }
        if (activeEffects.isNotEmpty() || passiveSkillNames.isNotEmpty()) {
            val slot = slotForItem(itemData) ?: return 0.0
            val baseline = charState.equipment.mapValues { (_, name) -> name to 0.0 }
            val withItem = baseline.toMutableMap()
            withItem[slot] = itemName to 0.0
            val baseScore = MaximizerSpeculation.scoreLoadout(
                baseState = charState,
                assignment = baseline,
                evaluator = evaluator,
                bestModes = modes,
                gameDatabase = gameDatabase,
                preferences = preferences,
                validateEquipment = false,
                activeEffects = activeEffects,
                passiveSkillNames = passiveSkillNames,
            )
            val withScore = MaximizerSpeculation.scoreLoadout(
                baseState = charState,
                assignment = withItem,
                evaluator = evaluator,
                bestModes = modes,
                gameDatabase = gameDatabase,
                preferences = preferences,
                validateEquipment = false,
                activeEffects = activeEffects,
                passiveSkillNames = passiveSkillNames,
            )
            return withScore - baseScore + evaluator.itemBonus(itemName)
        }
        val modeable = Modeable.find(itemName)
        val raw = if (modeable != null) {
            val mode = modes[modeable]
                ?: ModeableState.currentMode(preferences, modeable)
            modeable.modifiersForMode(mode)?.modifiers
        } else {
            gameDatabase.itemModifier(itemName)?.modifiers
        } ?: return 0.0
        return evaluator.getItemContribution(ModifierParser.parse(raw)) +
            evaluator.itemBonus(itemName)
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
        ItemPrimaryUse.WEAPON -> EquipmentSlot.WEAPON
        ItemPrimaryUse.SIXGUN -> EquipmentSlot.HOLSTER
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
        EquipmentSlot.WEAPON -> item.primaryUse == ItemPrimaryUse.WEAPON
        EquipmentSlot.HOLSTER -> item.primaryUse == ItemPrimaryUse.SIXGUN
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
        EquipmentSlot.CARDSLEEVE -> item.primaryUse == ItemPrimaryUse.CARD
        EquipmentSlot.STICKER1, EquipmentSlot.STICKER2, EquipmentSlot.STICKER3,
        EquipmentSlot.FOLDER1, EquipmentSlot.FOLDER2, EquipmentSlot.FOLDER3,
        EquipmentSlot.FOLDER4, EquipmentSlot.FOLDER5,
        EquipmentSlot.BOOTSKIN, EquipmentSlot.BOOTSPUR,
        -> false
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

    private fun buildCheckContext(
        charState: CharacterState,
        invState: InventoryState,
        activeEffects: List<EffectData>,
        closetContents: Map<Int, Int>,
        storageContents: Map<Int, Int>,
        stashContents: Map<Int, Int>,
    ): DynamicItemModifierSync.CheckContext {
        val codpieceGemNames = charState.equipment
            .filterKeys { it in EquipmentSlot.CODPIECE_SLOTS }
            .values
            .filter { it.isNotBlank() }
            .toSet()
        return DynamicItemModifierSync.CheckContext(
            inventoryItemIds = invState.items.filterValues { it.quantity > 0 }.keys,
            equippedItemNames = charState.equipment.values.filter { it.isNotBlank() }.toSet(),
            activeEffectNames = activeEffects.map { it.name }.toSet(),
            closetItemIds = closetContents.filterValues { it > 0 }.keys,
            storageItemIds = storageContents.filterValues { it > 0 }.keys,
            stashItemIds = stashContents.filterValues { it > 0 }.keys,
            limitMode = charState.limitMode,
            canInteract = !charState.isHardcore && !charState.isInRonin,
            hasClan = charState.hasClan,
            ascensionPath = charState.ascensionPath,
            codpieceGemNames = codpieceGemNames,
        )
    }
}
