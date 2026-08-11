package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.Beeosity
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.equipment.Modeable
import net.sourceforge.kolmafia.modifiers.CurrentModifiers
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.preferences.Preferences

/** Shared scoring options for maximizer speculation passes (Phase 381). */
data class MaximizerScoringOptions(
    val bestModes: Map<Modeable, String> = emptyMap(),
    val carryFamiliars: List<String> = emptyList(),
    val gameDatabase: GameDatabase? = null,
    val cardInSleeve: String? = null,
    val countFor: ((String) -> Int)? = null,
    val foldablesEnabled: Boolean = true,
    val activeEffects: List<EffectData> = emptyList(),
    val passiveSkillNames: Set<String> = emptySet(),
)

/**
 * Outfit-aware loadout scoring and recursive equipment speculation.
 * Mirrors desktop MaximizerSpeculation search shape with a shared combination budget.
 */
object MaximizerSpeculation {

    internal val searchSlots = EquipmentSlot.SEARCH_SLOTS

    fun scoreLoadout(
        baseState: CharacterState,
        assignment: Map<EquipmentSlot, Pair<String, Double>>,
        evaluator: Evaluator,
        familiarBonus: Double = 0.0,
        thrallBonus: Double = 0.0,
        modeOverrides: Map<Modeable, String> = emptyMap(),
        bestModes: Map<Modeable, String>? = null,
        carryFamiliars: List<String> = emptyList(),
        gameDatabase: GameDatabase? = null,
        cardInSleeve: String? = null,
        preferences: Preferences? = null,
        maxBeeosity: Int = 2,
        validateEquipment: Boolean = true,
        activeEffects: List<EffectData> = emptyList(),
        passiveSkillNames: Set<String> = emptySet(),
        horseryOverride: String? = null,
        boomBoxOverride: String? = null,
        mindControlOverride: Int? = null,
        customModifierOverlay: String? = null,
    ): Double {
        val effectiveModes = bestModes?.let {
            MaximizerModeSelection.assignmentModeOverrides(
                assignment, it, carryFamiliars, gameDatabase,
            )
        } ?: modeOverrides
        val resolvedCard = cardInSleeve ?: MaximizerCardSelection.cardForOffhand(
            assignment[EquipmentSlot.OFFHAND]?.first
                ?: baseState.equipment[EquipmentSlot.OFFHAND],
            null,
            baseState,
        )
        val equipment = buildEquipmentMap(baseState, assignment, resolvedCard)
        val baselineMods = CurrentModifiers(
            baseState,
            activeEffects = activeEffects,
            passiveSkillNames = passiveSkillNames,
            modeOverrides = modeOverrides,
            preferences = preferences,
            horseryOverride = horseryOverride,
            boomBoxOverride = boomBoxOverride,
            mindControlOverride = mindControlOverride,
            customModifierOverlay = customModifierOverlay,
        )
        val mods = CurrentModifiers(
            baseState.copy(equipment = equipment),
            activeEffects = activeEffects,
            passiveSkillNames = passiveSkillNames,
            modeOverrides = effectiveModes,
            preferences = preferences,
            horseryOverride = horseryOverride,
            boomBoxOverride = boomBoxOverride,
            mindControlOverride = mindControlOverride,
            customModifierOverlay = customModifierOverlay,
        )
        val score = evaluator.getScore(mods) +
            evaluator.equipmentBonus(equipment.values) +
            familiarBonus + thrallBonus
        if (MaximizerMutexViolations.introducesNewViolations(baselineMods.values, mods.values)) {
            evaluator.markFailed()
        }
        if (validateEquipment) {
            evaluator.checkEquipment(
                equipment = equipment,
                beeosity = Beeosity.equipmentBeeosity(equipment),
                maxBeeosity = maxBeeosity,
                inBeecore = baseState.inBeecore,
            )
        }
        return score
    }

    /** Post-equipment baseline score for non-equipment boost deltas (Phase 386). */
    fun scorePostEquipmentPlan(
        plan: MaximizerEmitSlot.Plan,
        charState: CharacterState,
        activeEffects: List<EffectData> = emptyList(),
        passiveSkillNames: Set<String> = emptySet(),
        horseryOverride: String? = null,
        boomBoxOverride: String? = null,
        mindControlOverride: Int? = null,
        customModifierOverlay: String? = null,
        carryFamiliars: List<String> = emptyList(),
        gameDatabase: GameDatabase? = null,
        preferences: Preferences? = null,
        thrallBonus: Double = 0.0,
    ): Double = scoreLoadout(
        baseState = charState,
        assignment = plan.bestPerSlot,
        evaluator = plan.spec.evaluator,
        familiarBonus = 0.0,
        thrallBonus = thrallBonus,
        bestModes = plan.modeSelections,
        carryFamiliars = carryFamiliars,
        gameDatabase = gameDatabase,
        cardInSleeve = plan.cardInSleeve,
        preferences = preferences,
        maxBeeosity = plan.spec.maxBeeosity,
        activeEffects = activeEffects,
        passiveSkillNames = passiveSkillNames,
        horseryOverride = horseryOverride,
        boomBoxOverride = boomBoxOverride,
        mindControlOverride = mindControlOverride,
        customModifierOverlay = customModifierOverlay,
    )

    /** Modifier accumulation for a post-equipment plan loadout (mutex checks). */
    fun modifierValuesForPostEquipmentPlan(
        plan: MaximizerEmitSlot.Plan,
        charState: CharacterState,
        activeEffects: List<EffectData> = emptyList(),
        passiveSkillNames: Set<String> = emptySet(),
        horseryOverride: String? = null,
        boomBoxOverride: String? = null,
        mindControlOverride: Int? = null,
        customModifierOverlay: String? = null,
        carryFamiliars: List<String> = emptyList(),
        gameDatabase: GameDatabase? = null,
        preferences: Preferences? = null,
    ) = modifierValuesForLoadout(
        baseState = charState,
        assignment = plan.bestPerSlot,
        bestModes = plan.modeSelections,
        carryFamiliars = carryFamiliars,
        gameDatabase = gameDatabase,
        cardInSleeve = plan.cardInSleeve,
        preferences = preferences,
        activeEffects = activeEffects,
        passiveSkillNames = passiveSkillNames,
        horseryOverride = horseryOverride,
        boomBoxOverride = boomBoxOverride,
        mindControlOverride = mindControlOverride,
        customModifierOverlay = customModifierOverlay,
    )

    /** Live post-equipment baseline score after maximize equip (Phase 405). */
    fun scorePostEquipmentLive(
        charState: CharacterState,
        evaluator: Evaluator,
        activeEffects: List<EffectData> = emptyList(),
        passiveSkillNames: Set<String> = emptySet(),
        horseryOverride: String? = null,
        boomBoxOverride: String? = null,
        mindControlOverride: Int? = null,
        customModifierOverlay: String? = null,
        gameDatabase: GameDatabase? = null,
        preferences: Preferences? = null,
        thrallBonus: Double = 0.0,
        maxBeeosity: Int = 2,
    ): Double = scoreLoadout(
        baseState = charState,
        assignment = emptyMap(),
        evaluator = evaluator,
        thrallBonus = thrallBonus,
        gameDatabase = gameDatabase,
        preferences = preferences,
        maxBeeosity = maxBeeosity,
        activeEffects = activeEffects,
        passiveSkillNames = passiveSkillNames,
        horseryOverride = horseryOverride,
        boomBoxOverride = boomBoxOverride,
        mindControlOverride = mindControlOverride,
        customModifierOverlay = customModifierOverlay,
    )

    /** Live post-equipment modifier accumulation (Phase 405). */
    fun modifierValuesForPostEquipmentLive(
        charState: CharacterState,
        activeEffects: List<EffectData> = emptyList(),
        passiveSkillNames: Set<String> = emptySet(),
        horseryOverride: String? = null,
        boomBoxOverride: String? = null,
        mindControlOverride: Int? = null,
        customModifierOverlay: String? = null,
        gameDatabase: GameDatabase? = null,
        preferences: Preferences? = null,
    ) = modifierValuesForLoadout(
        baseState = charState,
        assignment = emptyMap(),
        gameDatabase = gameDatabase,
        preferences = preferences,
        activeEffects = activeEffects,
        passiveSkillNames = passiveSkillNames,
        horseryOverride = horseryOverride,
        boomBoxOverride = boomBoxOverride,
        mindControlOverride = mindControlOverride,
        customModifierOverlay = customModifierOverlay,
    )

    internal fun modifierValuesForLoadout(
        baseState: CharacterState,
        assignment: Map<EquipmentSlot, Pair<String, Double>>,
        modeOverrides: Map<Modeable, String> = emptyMap(),
        bestModes: Map<Modeable, String>? = null,
        carryFamiliars: List<String> = emptyList(),
        gameDatabase: GameDatabase? = null,
        cardInSleeve: String? = null,
        preferences: Preferences? = null,
        activeEffects: List<EffectData> = emptyList(),
        passiveSkillNames: Set<String> = emptySet(),
        horseryOverride: String? = null,
        boomBoxOverride: String? = null,
        mindControlOverride: Int? = null,
        customModifierOverlay: String? = null,
    ): net.sourceforge.kolmafia.modifiers.ModifierValues {
        val effectiveModes = bestModes?.let {
            MaximizerModeSelection.assignmentModeOverrides(
                assignment, it, carryFamiliars, gameDatabase,
            )
        } ?: modeOverrides
        val resolvedCard = cardInSleeve ?: MaximizerCardSelection.cardForOffhand(
            assignment[EquipmentSlot.OFFHAND]?.first
                ?: baseState.equipment[EquipmentSlot.OFFHAND],
            null,
            baseState,
        )
        val equipment = buildEquipmentMap(baseState, assignment, resolvedCard)
        return CurrentModifiers(
            baseState.copy(equipment = equipment),
            activeEffects = activeEffects,
            passiveSkillNames = passiveSkillNames,
            modeOverrides = effectiveModes,
            preferences = preferences,
            horseryOverride = horseryOverride,
            boomBoxOverride = boomBoxOverride,
            mindControlOverride = mindControlOverride,
            customModifierOverlay = customModifierOverlay,
        ).values
    }

    fun tiebreakerScore(
        baseState: CharacterState,
        assignment: Map<EquipmentSlot, Pair<String, Double>>,
        goalEvaluator: Evaluator? = null,
        modeOverrides: Map<Modeable, String> = emptyMap(),
        bestModes: Map<Modeable, String>? = null,
        carryFamiliars: List<String> = emptyList(),
        gameDatabase: GameDatabase? = null,
        cardInSleeve: String? = null,
        preferences: Preferences? = null,
        activeEffects: List<EffectData> = emptyList(),
        passiveSkillNames: Set<String> = emptySet(),
    ): Double {
        val effectiveModes = bestModes?.let {
            MaximizerModeSelection.assignmentModeOverrides(
                assignment, it, carryFamiliars, gameDatabase,
            )
        } ?: modeOverrides
        val resolvedCard = cardInSleeve ?: MaximizerCardSelection.cardForOffhand(
            assignment[EquipmentSlot.OFFHAND]?.first
                ?: baseState.equipment[EquipmentSlot.OFFHAND],
            null,
            baseState,
        )
        val equipment = buildEquipmentMap(baseState, assignment, resolvedCard)
        val mods = CurrentModifiers(
            baseState.copy(equipment = equipment),
            activeEffects = activeEffects,
            passiveSkillNames = passiveSkillNames,
            modeOverrides = effectiveModes,
            preferences = preferences,
        )
        return goalEvaluator?.tiebreakerScore(mods) ?: Evaluator.tiebreaker().getScore(mods)
    }

    internal fun buildEquipmentMap(
        baseState: CharacterState,
        assignment: Map<EquipmentSlot, Pair<String, Double>>,
        cardInSleeve: String? = null,
    ): Map<EquipmentSlot, String> = buildMap {
        for (slot in searchSlots) {
            val name = assignment[slot]?.first?.takeIf { it.isNotBlank() }
                ?: baseState.equipment[slot]?.takeIf { it.isNotBlank() }
            if (!name.isNullOrBlank()) put(slot, name)
        }
        for (slot in EquipmentSlot.SUB_SLOTS) {
            val name = assignment[slot]?.first?.takeIf { it.isNotBlank() }
                ?: baseState.equipment[slot]?.takeIf { it.isNotBlank() }
            if (!name.isNullOrBlank()) put(slot, name)
        }
        for (slot in EquipmentSlot.CODPIECE_SLOTS) {
            baseState.equipment[slot]?.takeIf { it.isNotBlank() }?.let { put(slot, it) }
        }
        if (!cardInSleeve.isNullOrBlank()) {
            put(EquipmentSlot.CARDSLEEVE, cardInSleeve)
        } else {
            baseState.equipment[EquipmentSlot.CARDSLEEVE]?.takeIf { it.isNotBlank() }?.let {
                put(EquipmentSlot.CARDSLEEVE, it)
            }
        }
    }

    internal fun crossSlotDedupSlots(slot: EquipmentSlot): Set<EquipmentSlot> = when (slot) {
        EquipmentSlot.FAMILIAR -> setOf(
            EquipmentSlot.WEAPON,
            EquipmentSlot.OFFHAND,
            EquipmentSlot.HAT,
            EquipmentSlot.PANTS,
        )
        EquipmentSlot.OFFHAND -> setOf(
            EquipmentSlot.WEAPON,
            EquipmentSlot.FAMILIAR,
        )
        EquipmentSlot.WEAPON -> setOf(
            EquipmentSlot.OFFHAND,
            EquipmentSlot.FAMILIAR,
        )
        EquipmentSlot.HAT -> setOf(EquipmentSlot.FAMILIAR)
        EquipmentSlot.PANTS -> setOf(EquipmentSlot.FAMILIAR)
        else -> emptySet()
    }

    internal fun assignmentPrice(
        assignment: Map<EquipmentSlot, Pair<String, Double>>,
        priceFor: (String) -> Int,
    ): Int = assignment.values.sumOf { (name, _) ->
        if (name.isBlank()) 0 else priceFor(name)
    }

    internal fun isBetterLoadout(
        score: Double,
        tie: Double,
        price: Int,
        failed: Boolean,
        bestScore: Double,
        bestTie: Double,
        bestPrice: Int,
        bestFailed: Boolean,
        preferLowerPrice: Boolean,
    ): Boolean {
        if (failed != bestFailed) return !failed
        if (score > bestScore + 1e-9) return true
        if (score < bestScore - 1e-9) return false
        if (tie > bestTie + 1e-9) return true
        if (tie < bestTie - 1e-9) return false
        return preferLowerPrice && price < bestPrice
    }

    fun speculate(
        spec: MaximizeSpec,
        baseState: CharacterState,
        candidatesBySlot: Map<EquipmentSlot, List<Pair<String, Double>>>,
        budget: ComboBudget,
        familiarBonus: Double = 0.0,
        thrallBonus: Double = 0.0,
        seed: Map<EquipmentSlot, Pair<String, Double>> = emptyMap(),
        priceFor: ((String) -> Int)? = null,
        modeOverrides: Map<Modeable, String> = emptyMap(),
        bestModes: Map<Modeable, String>? = null,
        carryFamiliars: List<String> = emptyList(),
        gameDatabase: GameDatabase? = null,
        cardInSleeve: String? = null,
        foldablesEnabled: Boolean = true,
        countFor: ((String) -> Int)? = null,
        preferences: Preferences? = null,
        activeEffects: List<EffectData> = emptyList(),
        passiveSkillNames: Set<String> = emptySet(),
    ): Map<EquipmentSlot, Pair<String, Double>> {
        var best = seed
        val resolvedCard = cardInSleeve
        var bestScore: Double
        var bestFailed: Boolean
        if (seed.isNotEmpty()) {
            bestScore = scoreLoadout(
                baseState, seed, spec.evaluator, familiarBonus, thrallBonus,
                modeOverrides, bestModes, carryFamiliars, gameDatabase, resolvedCard, preferences,
                maxBeeosity = spec.maxBeeosity,
                activeEffects = activeEffects,
                passiveSkillNames = passiveSkillNames,
            )
            bestFailed = spec.evaluator.failed
        } else {
            bestScore = Double.NEGATIVE_INFINITY
            bestFailed = true
        }
        var bestTie = if (seed.isNotEmpty()) {
            tiebreakerScore(
                baseState, seed, spec.evaluator, modeOverrides, bestModes, carryFamiliars,
                gameDatabase, resolvedCard, preferences, activeEffects, passiveSkillNames,
            )
        } else {
            Double.NEGATIVE_INFINITY
        }
        var bestPrice = if (seed.isNotEmpty() && priceFor != null) assignmentPrice(seed, priceFor) else Int.MAX_VALUE
        val preferLowerPrice = spec.maxPrice != null && priceFor != null
        var stopSearch = false
        val db = gameDatabase
        val countLookup = countFor ?: { 1 }

        fun search(
            slotIndex: Int,
            current: MutableMap<EquipmentSlot, Pair<String, Double>>,
        ) {
            if (stopSearch || budget.tick()) return
            if (slotIndex >= searchSlots.size) {
                val card = MaximizerCardSelection.cardForOffhand(
                    current[EquipmentSlot.OFFHAND]?.first, resolvedCard, baseState,
                )
                val score = scoreLoadout(
                    baseState, current, spec.evaluator, familiarBonus, thrallBonus,
                    modeOverrides, bestModes, carryFamiliars, gameDatabase, card, preferences,
                    maxBeeosity = spec.maxBeeosity,
                    activeEffects = activeEffects,
                    passiveSkillNames = passiveSkillNames,
                )
                val failed = spec.evaluator.failed
                val exceeded = spec.evaluator.exceeded
                val tie = tiebreakerScore(
                    baseState, current, spec.evaluator, modeOverrides, bestModes, carryFamiliars,
                    gameDatabase, card, preferences, activeEffects, passiveSkillNames,
                )
                val price = priceFor?.let { assignmentPrice(current, it) } ?: Int.MAX_VALUE
                if (!failed &&
                    isBetterLoadout(
                        score, tie, price, failed,
                        bestScore, bestTie, bestPrice, bestFailed, preferLowerPrice,
                    )
                ) {
                    bestScore = score
                    bestTie = tie
                    bestPrice = price
                    bestFailed = false
                    best = current.toMap()
                    MaximizerProgress.maybeShow(
                        budget.combinationsChecked,
                        bestScore,
                        bestFailed,
                    )
                }
                if (exceeded) {
                    budget.markScoreCapReached()
                    stopSearch = true
                }
                return
            }
            val slot = searchSlots[slotIndex]
            val candidates = candidatesBySlot[slot].orEmpty()
            if (candidates.isEmpty()) {
                search(slotIndex + 1, current)
                return
            }
            for ((name, _) in candidates) {
                if (stopSearch) return
                if (db != null) {
                    val available = MaximizerFoldDedup.availableCount(
                        itemName = name,
                        assignment = current,
                        baseCount = countLookup(name),
                        foldablesEnabled = foldablesEnabled,
                        gameDatabase = db,
                        excludeSlot = slot,
                        excludeSlotsForSameItem = crossSlotDedupSlots(slot),
                    )
                    if (available <= 0) continue
                }
                current[slot] = name to 0.0
                search(slotIndex + 1, current)
                current.remove(slot)
                if (budget.exhausted() || stopSearch) return
            }
        }

        if (!MaximizerContinuation.permitsContinue()) {
            budget.markInterrupted()
        } else {
            search(0, seed.toMutableMap())
        }
        return best
    }

    fun topCandidatesPerSlot(
        spec: MaximizeSpec,
        rankedBuckets: SlotList<MaximizerRankedItem>,
        usedElsewhere: Set<String>,
        perSlotLimit: Int,
        gameDatabase: GameDatabase,
        scoreItem: (String, Evaluator) -> Double,
        itemMeetsConstraints: (String, MaximizeSpec) -> Boolean,
        priceFor: (String) -> Int = { gameDatabase.npcPrice(it) },
        familiarCarryRaces: List<String> = emptyList(),
        familiarCarryScorer: ((String, DoubleModifier) -> Double)? = null,
        familiarBucketIndex: Int? = null,
    ): Map<EquipmentSlot, List<Pair<String, Double>>> =
        MaximizerEquipmentEnumerator.toCandidatesByEquipmentSlot(
            rankedBuckets,
            spec,
            usedElsewhere,
            perSlotLimit,
            gameDatabase,
            scoreItem,
            itemMeetsConstraints,
            priceFor,
            familiarCarryRaces,
            familiarCarryScorer,
            familiarBucketIndex,
        )

    fun topCandidatesPerSlot(
        spec: MaximizeSpec,
        gameDatabase: GameDatabase,
        candidateIds: Set<Int>,
        usedElsewhere: Set<String>,
        perSlotLimit: Int,
        scoreItem: (String, Evaluator) -> Double,
        itemMeetsConstraints: (String, MaximizeSpec) -> Boolean,
        priceFor: (String) -> Int = { gameDatabase.npcPrice(it) },
        familiarCarryRaces: List<String> = emptyList(),
        familiarCarryScorer: ((String, DoubleModifier) -> Double)? = null,
        checkedItem: (Int) -> MaximizerCheckedItem = { itemId ->
            if (itemId in candidateIds) {
                val name = gameDatabase.item(itemId)?.name ?: ""
                MaximizerCheckedItem(itemId, name, initial = 1)
            } else {
                MaximizerCheckedItem(itemId, gameDatabase.item(itemId)?.name ?: "", initial = 0)
            }
        },
    ): Map<EquipmentSlot, List<Pair<String, Double>>> {
        val buckets = MaximizerEquipmentEnumerator.enumerate(
            candidateIds = candidateIds,
            spec = spec,
            gameDatabase = gameDatabase,
            checkedItem = checkedItem,
            scoreItem = scoreItem,
            itemMeetsConstraints = itemMeetsConstraints,
            priceFor = priceFor,
        )
        return topCandidatesPerSlot(
            spec,
            buckets,
            usedElsewhere,
            perSlotLimit,
            gameDatabase,
            scoreItem,
            itemMeetsConstraints,
            priceFor,
            familiarCarryRaces,
            familiarCarryScorer,
        )
    }
}

/** Shared combination budget across maximizer refine passes. */
class ComboBudget(private val limit: Int) {
    private var checked = 0
    var limitHit: Boolean = false
        private set
    var scoreCapReached: Boolean = false
        private set
    var interrupted: Boolean = false
        private set

    val combinationsChecked: Int
        get() = checked

    fun markScoreCapReached() {
        scoreCapReached = true
    }

    fun markInterrupted() {
        interrupted = true
    }

    fun tick(): Boolean {
        if (!MaximizerContinuation.permitsContinue()) {
            interrupted = true
            return true
        }
        if (limit <= 0) return false
        checked++
        if (checked > limit) {
            limitHit = true
            return true
        }
        return false
    }

    fun exhausted(): Boolean = limit > 0 && checked > limit

    fun remaining(): Int = if (limit <= 0) Int.MAX_VALUE else (limit - checked).coerceAtLeast(0)
}
