package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.GameDatabase
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
        scoring: MaximizerScoringOptions = MaximizerScoringOptions(),
        preferences: Preferences? = null,
    ): Map<EquipmentSlot, Pair<String, Double>> {
        if (currentBest[EquipmentSlot.FAMILIAR]?.first?.isNotBlank() == true) return currentBest
        val familiarCandidates = candidatesBySlot[EquipmentSlot.FAMILIAR].orEmpty()
        if (familiarCandidates.isEmpty() || budget.exhausted()) return currentBest

        var best = currentBest
        var bestScore = scoreAssignment(baseState, best, spec, familiarBonus, thrallBonus, scoring, preferences)
        var bestFailed = spec.evaluator.failed
        var bestTie = if (best.isNotEmpty()) {
            tieAssignment(baseState, best, spec, scoring, preferences)
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
                bestModes = scoring.bestModes,
                carryFamiliars = scoring.carryFamiliars,
                gameDatabase = gameDatabase,
                cardInSleeve = scoring.cardInSleeve,
                foldablesEnabled = scoring.foldablesEnabled,
                countFor = scoring.countFor,
                preferences = preferences,
                activeEffects = scoring.activeEffects,
                passiveSkillNames = scoring.passiveSkillNames,
            )
            if (result.isEmpty()) continue

            val resultScore = scoreAssignment(baseState, result, spec, familiarBonus, thrallBonus, scoring, preferences)
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
            passiveSkillNames = scoring.passiveSkillNames,
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
            passiveSkillNames = scoring.passiveSkillNames,
        )
    }
}
