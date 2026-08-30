package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase
import net.sourceforge.kolmafia.session.CakeArenaManager.ArenaOpponent

/**
 * Desktop [FamiliarTool] — pick best Cake Arena opponent/match/weight
 * (Phases 3246–3260).
 */
class FamiliarTool(opponents: List<ArenaOpponent>) {

    private val opponents: Array<Opponent> =
        opponents.map { Opponent(it) }.toTypedArray()

    private var bestOpponent: Opponent? = null
    private var bestMatch: Int = -1
    private var bestWeight: Int = -1
    private var difference: Int = 500

    fun bestOpponent(ownFamiliarId: Int, possibleOwnWeights: IntArray): ArenaOpponent? {
        val ownSkills = FamiliarDefinitionDatabase.getFamiliarSkills(ownFamiliarId)
        return bestOpponent(ownSkills, possibleOwnWeights)
    }

    fun bestOpponent(ownSkills: IntArray, possibleOwnWeights: IntArray): ArenaOpponent? {
        bestOpponent = null
        bestMatch = -1
        bestWeight = -1
        difference = 500

        for (match in 0 until 4) {
            val ownSkill = ownSkills.getOrElse(match) { 0 }
            if (ownSkill == 0) continue

            for (opp in opponents) {
                val opponentWeight = opp.weight
                for (ownWeight in possibleOwnWeights) {
                    val ownPower = ownWeight + ownSkill * 3
                    val opponentSkill = opp.getSkill(match)
                    val opponentPower = if (opponentSkill == 0) {
                        5
                    } else {
                        opponentWeight + opponentSkill * 3
                    }
                    if (betterWeightDifference(ownPower - (opponentPower + 3), difference)) {
                        difference = ownPower - (opponentPower + 3)
                        bestOpponent = opp
                        bestMatch = match
                        bestWeight = ownWeight
                    }
                }
            }
        }
        return bestOpponent?.opponent
    }

    /** 1-based event id. */
    fun bestMatch(): Int = bestMatch + 1

    fun bestWeight(): Int = bestWeight

    fun difference(): Int = difference

    private class Opponent(val opponent: ArenaOpponent) {
        val weight: Int = opponent.weight
        private val arena: IntArray =
            FamiliarDefinitionDatabase.getFamiliarSkills(
                FamiliarDefinitionDatabase.getByName(opponent.race)?.id ?: -1,
            )

        fun getSkill(match: Int): Int = arena.getOrElse(match) { 0 }
    }

    companion object {
        private fun betterWeightDifference(newVal: Int, oldVal: Int): Boolean = when (oldVal) {
            0 -> false
            1 -> newVal == 0
            -1 -> newVal == 0 || newVal == 1
            2 -> newVal == 0 || newVal == 1 || newVal == -1
            3 -> newVal == 0 || newVal == 1 || newVal == -1 || newVal == 2
            -2 -> newVal == 0 || newVal == 1 || newVal == -1 || newVal == 2 || newVal == 3
            else -> newVal == 0 || (newVal < oldVal && newVal >= -2)
        }
    }
}
