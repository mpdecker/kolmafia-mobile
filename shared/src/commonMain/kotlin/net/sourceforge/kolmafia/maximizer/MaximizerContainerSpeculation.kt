package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.GameDatabase
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
        usableBjornFamiliars: List<String> = emptyList(),
        scoreFamiliar: (String?) -> Double = { 0.0 },
        scoring: MaximizerScoringOptions = MaximizerScoringOptions(),
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
        var bestScore = scoreAssignment(baseState, best, spec, familiarBonus, thrallBonus, scoring, preferences)
        var bestFailed = spec.evaluator.failed
        var bestTie = if (best.isNotEmpty()) {
            tieAssignment(baseState, best, spec, scoring, preferences)
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
                    foldablesEnabled = scoring.foldablesEnabled,
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
                    bestModes = scoring.bestModes,
                    carryFamiliars = scoring.carryFamiliars,
                    gameDatabase = gameDatabase,
                    cardInSleeve = scoring.cardInSleeve,
                    foldablesEnabled = scoring.foldablesEnabled,
                    countFor = scoring.countFor,
                    preferences = preferences,
                    activeEffects = scoring.activeEffects,
                )
                if (result.isEmpty()) continue

                val resultScore = scoreAssignment(
                    baseState, result, spec, branchBonus, thrallBonus, scoring, preferences,
                )
                val failed = spec.evaluator.failed
                if (failed) continue
                val tie = tieAssignment(baseState, result, spec, scoring, preferences)
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

    private fun scoreAssignment(
        baseState: CharacterState,
        assignment: Map<EquipmentSlot, Pair<String, Double>>,
        spec: MaximizeSpec,
        familiarBonus: Double,
        thrallBonus: Double,
        scoring: MaximizerScoringOptions,
        preferences: Preferences?,
    ): Double {
        val card = MaximizerCardSelection.cardForOffhand(
            assignment[EquipmentSlot.OFFHAND]?.first, scoring.cardInSleeve, baseState,
        )
        return MaximizerSpeculation.scoreLoadout(
            baseState, assignment, spec.evaluator, familiarBonus, thrallBonus,
            bestModes = scoring.bestModes,
            carryFamiliars = scoring.carryFamiliars,
            gameDatabase = scoring.gameDatabase,
            cardInSleeve = card,
            preferences = preferences,
            maxBeeosity = spec.maxBeeosity,
            activeEffects = scoring.activeEffects,
        )
    }

    private fun tieAssignment(
        baseState: CharacterState,
        assignment: Map<EquipmentSlot, Pair<String, Double>>,
        spec: MaximizeSpec,
        scoring: MaximizerScoringOptions,
        preferences: Preferences?,
    ): Double {
        val card = MaximizerCardSelection.cardForOffhand(
            assignment[EquipmentSlot.OFFHAND]?.first, scoring.cardInSleeve, baseState,
        )
        return MaximizerSpeculation.tiebreakerScore(
            baseState, assignment, spec.evaluator,
            bestModes = scoring.bestModes,
            carryFamiliars = scoring.carryFamiliars,
            gameDatabase = scoring.gameDatabase,
            cardInSleeve = card,
            preferences = preferences,
            activeEffects = scoring.activeEffects,
        )
    }
}
