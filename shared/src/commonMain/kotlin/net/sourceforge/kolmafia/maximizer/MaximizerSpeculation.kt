package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.modifiers.CurrentModifiers
import net.sourceforge.kolmafia.modifiers.DoubleModifier

/**
 * Outfit-aware loadout scoring and recursive equipment speculation.
 * Mirrors desktop MaximizerSpeculation search shape with a shared combination budget.
 */
object MaximizerSpeculation {

    internal val searchSlots = listOf(
        EquipmentSlot.HAT,
        EquipmentSlot.WEAPON,
        EquipmentSlot.OFFHAND,
        EquipmentSlot.SHIRT,
        EquipmentSlot.PANTS,
        EquipmentSlot.ACC1,
        EquipmentSlot.ACC2,
        EquipmentSlot.ACC3,
        EquipmentSlot.FAMILIAR,
    )

    fun scoreLoadout(
        baseState: CharacterState,
        assignment: Map<EquipmentSlot, Pair<String, Double>>,
        evaluator: Evaluator,
        familiarBonus: Double = 0.0,
        thrallBonus: Double = 0.0,
    ): Double {
        val equipment = buildMap {
            for (slot in searchSlots) {
                val name = assignment[slot]?.first?.takeIf { it.isNotBlank() }
                    ?: baseState.equipment[slot]?.takeIf { it.isNotBlank() }
                if (!name.isNullOrBlank()) put(slot, name)
            }
        }
        val mods = CurrentModifiers(baseState.copy(equipment = equipment))
        return evaluator.getScore(mods) + familiarBonus + thrallBonus
    }

    fun tiebreakerScore(
        baseState: CharacterState,
        assignment: Map<EquipmentSlot, Pair<String, Double>>,
    ): Double {
        val equipment = buildMap {
            for (slot in searchSlots) {
                val name = assignment[slot]?.first?.takeIf { it.isNotBlank() }
                    ?: baseState.equipment[slot]?.takeIf { it.isNotBlank() }
                if (!name.isNullOrBlank()) put(slot, name)
            }
        }
        val mods = CurrentModifiers(baseState.copy(equipment = equipment))
        return Evaluator.tiebreaker().getScore(mods)
    }

    private fun assignmentPrice(
        assignment: Map<EquipmentSlot, Pair<String, Double>>,
        priceFor: (String) -> Int,
    ): Int = assignment.values.sumOf { (name, _) ->
        if (name.isBlank()) 0 else priceFor(name)
    }

    private fun isBetterLoadout(
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
    ): Map<EquipmentSlot, Pair<String, Double>> {
        var best = seed
        var bestScore: Double
        var bestFailed: Boolean
        if (seed.isNotEmpty()) {
            bestScore = scoreLoadout(baseState, seed, spec.evaluator, familiarBonus, thrallBonus)
            bestFailed = spec.evaluator.failed
        } else {
            bestScore = Double.NEGATIVE_INFINITY
            bestFailed = true
        }
        var bestTie = if (seed.isNotEmpty()) tiebreakerScore(baseState, seed) else Double.NEGATIVE_INFINITY
        var bestPrice = if (seed.isNotEmpty() && priceFor != null) assignmentPrice(seed, priceFor) else Int.MAX_VALUE
        val preferLowerPrice = spec.maxPrice != null && priceFor != null
        var stopSearch = false

        fun search(
            slotIndex: Int,
            current: MutableMap<EquipmentSlot, Pair<String, Double>>,
            usedItems: MutableSet<String>,
        ) {
            if (stopSearch || budget.tick()) return
            if (slotIndex >= searchSlots.size) {
                val score = scoreLoadout(
                    baseState, current, spec.evaluator, familiarBonus, thrallBonus,
                )
                val failed = spec.evaluator.failed
                val exceeded = spec.evaluator.exceeded
                val tie = tiebreakerScore(baseState, current)
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
                }
                if (exceeded) stopSearch = true
                return
            }
            val slot = searchSlots[slotIndex]
            val candidates = candidatesBySlot[slot].orEmpty()
            if (candidates.isEmpty()) {
                search(slotIndex + 1, current, usedItems)
                return
            }
            for ((name, _) in candidates) {
                if (stopSearch) return
                if (name in usedItems) continue
                current[slot] = name to 0.0
                usedItems.add(name)
                search(slotIndex + 1, current, usedItems)
                usedItems.remove(name)
                current.remove(slot)
                if (budget.exhausted() || stopSearch) return
            }
        }

        search(0, seed.toMutableMap(), seed.values.map { it.first }.toMutableSet())
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

    fun tick(): Boolean {
        if (limit <= 0) return false
        checked++
        return checked > limit
    }

    fun exhausted(): Boolean = limit > 0 && checked > limit

    fun remaining(): Int = if (limit <= 0) Int.MAX_VALUE else (limit - checked).coerceAtLeast(0)
}
