package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.equipment.Modeable
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.data.OutfitData

/**
 * Desktop MaximizerSpeculation.tryOutfits speculation pre-pass (Phase 372).
 * Seeds DFS with surviving useful outfit piece sets before the main speculate call.
 */
object MaximizerOutfitSpeculation {

    fun survivingUsefulOutfits(
        buckets: SlotList<MaximizerRankedItem>,
        usefulOutfits: List<OutfitData>,
    ): List<OutfitData> = MaximizerOutfitSlots.survivingUsefulOutfits(buckets, usefulOutfits)

    fun tryOutfits(
        spec: MaximizeSpec,
        baseState: CharacterState,
        survivingOutfits: List<OutfitData>,
        rankedBuckets: SlotList<MaximizerRankedItem>,
        candidatesBySlot: Map<EquipmentSlot, List<Pair<String, Double>>>,
        budget: ComboBudget,
        currentBest: Map<EquipmentSlot, Pair<String, Double>>,
        gameDatabase: GameDatabase,
        familiarBonus: Double = 0.0,
        thrallBonus: Double = 0.0,
        priceFor: ((String) -> Int)? = null,
        modeOverrides: Map<Modeable, String> = emptyMap(),
        preferences: Preferences? = null,
    ): Map<EquipmentSlot, Pair<String, Double>> {
        if (survivingOutfits.isEmpty() || budget.exhausted()) return currentBest

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

        for (outfit in survivingOutfits) {
            if (budget.exhausted()) break
            val assignment = MaximizerOutfitSlots.buildOutfitAssignment(
                outfit, rankedBuckets, spec, gameDatabase,
            ) ?: continue
            val seed = assignment.mapValues { (_, name) -> name to 0.0 }
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

            val score = MaximizerSpeculation.scoreLoadout(
                baseState, result, spec.evaluator, familiarBonus, thrallBonus,
                modeOverrides, preferences,
            )
            val failed = spec.evaluator.failed
            if (failed) continue
            val tie = MaximizerSpeculation.tiebreakerScore(baseState, result, modeOverrides, preferences)
            val price = priceFor?.let { MaximizerSpeculation.assignmentPrice(result, it) } ?: Int.MAX_VALUE
            if (MaximizerSpeculation.isBetterLoadout(
                    score, tie, price, failed,
                    bestScore, bestTie, bestPrice, bestFailed, preferLowerPrice,
                )
            ) {
                best = result
                bestScore = score
                bestFailed = false
                bestTie = tie
                bestPrice = price
            }
        }

        return best
    }
}
