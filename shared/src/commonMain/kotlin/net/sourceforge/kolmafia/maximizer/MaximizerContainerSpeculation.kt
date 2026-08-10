package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.equipment.Modeable
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop MaximizerSpeculation.tryContainers speculation pre-pass (Phase 376–377).
 * Seeds DFS with each ranked CONTAINER-bucket item before the main speculate call.
 */
object MaximizerContainerSpeculation {

    data class TryContainersResult(
        val bestPerSlot: Map<EquipmentSlot, Pair<String, Double>>,
        val bjornifiedRace: String? = null,
    )

    private val sameItemDedupSlots = setOf(
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

    fun tryContainers(
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
        foldablesEnabled: Boolean = true,
        usableBjornFamiliars: List<String> = emptyList(),
        scoreFamiliar: (String?) -> Double = { 0.0 },
        modeOverrides: Map<Modeable, String> = emptyMap(),
        preferences: Preferences? = null,
    ): TryContainersResult {
        if (currentBest[EquipmentSlot.CONTAINER]?.first?.isNotBlank() == true) {
            return TryContainersResult(currentBest)
        }
        val containerCandidates = candidatesBySlot[EquipmentSlot.CONTAINER].orEmpty()
        if (containerCandidates.isEmpty() || budget.exhausted()) {
            return TryContainersResult(currentBest)
        }

        var best = currentBest
        var bestBjornRace: String? = null
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

        for ((name, score) in containerCandidates) {
            if (budget.exhausted()) break
            val baseCount = accessibleCount(name, rankedBuckets)
            val seed = currentBest + (EquipmentSlot.CONTAINER to (name to score))
            if (MaximizerFoldDedup.availableCount(
                    itemName = name,
                    assignment = seed,
                    baseCount = baseCount,
                    foldablesEnabled = foldablesEnabled,
                    gameDatabase = gameDatabase,
                    excludeSlot = EquipmentSlot.CONTAINER,
                    excludeSlotsForSameItem = sameItemDedupSlots,
                ) <= 0
            ) {
                continue
            }

            val isBuddyBjorn = name.equals(MaximizerManager.BUDDY_BJORN, ignoreCase = true)
            val bjornBranches = if (isBuddyBjorn && usableBjornFamiliars.isNotEmpty()) {
                usableBjornFamiliars
            } else {
                listOf(null)
            }

            for (bjornRace in bjornBranches) {
                if (budget.exhausted()) break
                val branchBonus = familiarBonus + (bjornRace?.let { scoreFamiliar(it) } ?: 0.0)
                val result = MaximizerSpeculation.speculate(
                    spec = spec,
                    baseState = baseState,
                    candidatesBySlot = candidatesBySlot,
                    budget = budget,
                    familiarBonus = branchBonus,
                    thrallBonus = thrallBonus,
                    seed = seed,
                    priceFor = priceFor,
                    modeOverrides = modeOverrides,
                    preferences = preferences,
                )
                if (result.isEmpty()) continue

                val resultScore = MaximizerSpeculation.scoreLoadout(
                    baseState, result, spec.evaluator, branchBonus, thrallBonus,
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
                    bestBjornRace = bjornRace?.takeIf { isBuddyBjorn }
                }
            }
        }

        return TryContainersResult(best, bestBjornRace)
    }

    private fun accessibleCount(name: String, rankedBuckets: SlotList<MaximizerRankedItem>): Int =
        rankedBuckets.allItems(MaximizerSlot.CONTAINER)
            .firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?.accessibleCount
            ?: MaximizerEquipmentEnumerator.allRankedItems(rankedBuckets)
                .firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?.accessibleCount
            ?: 1
}
