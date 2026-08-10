package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.equipment.Modeable
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop MaximizerSpeculation.tryFamiliarItems speculation pre-pass (Phase 374).
 * Seeds DFS with each ranked FAMILIAR-bucket item before the main speculate call.
 */
object MaximizerFamiliarItemSpeculation {

    private val dedupSlots = listOf(
        EquipmentSlot.WEAPON,
        EquipmentSlot.OFFHAND,
        EquipmentSlot.HAT,
        EquipmentSlot.PANTS,
        EquipmentSlot.ACC1,
        EquipmentSlot.ACC2,
        EquipmentSlot.ACC3,
    )

    fun tryFamiliarItems(
        spec: MaximizeSpec,
        baseState: CharacterState,
        candidatesBySlot: Map<EquipmentSlot, List<Pair<String, Double>>>,
        rankedBuckets: SlotList<MaximizerRankedItem>,
        budget: ComboBudget,
        familiarBonus: Double = 0.0,
        thrallBonus: Double = 0.0,
        currentBest: Map<EquipmentSlot, Pair<String, Double>>,
        gameDatabase: GameDatabase,
        priceFor: ((String) -> Int)? = null,
        modeOverrides: Map<Modeable, String> = emptyMap(),
        preferences: Preferences? = null,
    ): Map<EquipmentSlot, Pair<String, Double>> {
        if (currentBest[EquipmentSlot.FAMILIAR]?.first?.isNotBlank() == true) return currentBest
        val familiarCandidates = candidatesBySlot[EquipmentSlot.FAMILIAR].orEmpty()
        if (familiarCandidates.isEmpty() || budget.exhausted()) return currentBest

        var best = currentBest
        var bestScore = MaximizerSpeculation.scoreLoadout(
            baseState, best, spec.evaluator, familiarBonus, thrallBonus,
            modeOverrides, preferences,
        )
        var bestFailed = spec.evaluator.failed
        var bestTie = if (best.isNotEmpty()) {
            MaximizerSpeculation.tiebreakerScore(baseState, best, modeOverrides, preferences)
        } else {
            Double.NEGATIVE_INFINITY
        }
        var bestPrice = priceFor?.let { MaximizerSpeculation.assignmentPrice(best, it) } ?: Int.MAX_VALUE
        val preferLowerPrice = spec.maxPrice != null && priceFor != null

        for ((name, score) in familiarCandidates) {
            if (budget.exhausted()) break
            val baseCount = accessibleCount(name, rankedBuckets)
            val seed = currentBest + (EquipmentSlot.FAMILIAR to (name to score))
            if (availableCount(name, seed, baseCount) <= 0) continue

            val result = MaximizerSpeculation.speculate(
                spec = spec,
                baseState = baseState,
                candidatesBySlot = candidatesBySlot,
                budget = budget,
                familiarBonus = familiarBonus,
                thrallBonus = thrallBonus,
                seed = seed,
                priceFor = priceFor,
                modeOverrides = modeOverrides,
                preferences = preferences,
            )
            if (result.isEmpty()) continue

            val resultScore = MaximizerSpeculation.scoreLoadout(
                baseState, result, spec.evaluator, familiarBonus, thrallBonus,
                modeOverrides, preferences,
            )
            val failed = spec.evaluator.failed
            if (failed) continue
            val tie = MaximizerSpeculation.tiebreakerScore(baseState, result, modeOverrides, preferences)
            val price = priceFor?.let { MaximizerSpeculation.assignmentPrice(result, it) } ?: Int.MAX_VALUE
            if (MaximizerSpeculation.isBetterLoadout(
                    resultScore, tie, price, failed,
                    bestScore, bestTie, bestPrice, bestFailed, preferLowerPrice,
                )
            ) {
                best = result
                bestScore = resultScore
                bestFailed = false
                bestTie = tie
                bestPrice = price
            }
        }

        return best
    }

    internal fun accessibleCount(name: String, rankedBuckets: SlotList<MaximizerRankedItem>): Int =
        MaximizerEquipmentEnumerator.allRankedItems(rankedBuckets)
            .firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?.accessibleCount ?: 1

    internal fun availableCount(
        itemName: String,
        assignment: Map<EquipmentSlot, Pair<String, Double>>,
        baseCount: Int,
    ): Int {
        var count = baseCount
        for (slot in dedupSlots) {
            if (assignment[slot]?.first.equals(itemName, ignoreCase = true)) {
                count--
            }
        }
        return count
    }
}
