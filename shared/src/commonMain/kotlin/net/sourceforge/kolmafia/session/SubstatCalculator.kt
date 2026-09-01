package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.MainStat

/** Desktop KoLCharacter.calculateSubpoints / calculatePointSubpoints helpers. */
object SubstatCalculator {

    fun calculatePointSubpoints(basePoints: Int): Long = basePoints.toLong() * basePoints

    fun calculateSubpoints(baseValue: Int, sinceLastBase: Int = 0): Long =
        calculatePointSubpoints(baseValue) + sinceLastBase

    fun primeIndex(characterClass: Int): Int = when (CharacterClass.fromId(characterClass).mainStat) {
        MainStat.MUSCLE -> 0
        MainStat.MYSTICALITY -> 1
        MainStat.MOXIE -> 2
    }

    fun totalSubpoints(state: CharacterState, index: Int): Long = when (index) {
        0 -> calculateSubpoints(state.baseMusc, state.muscSubpoints.toInt())
        1 -> calculateSubpoints(state.baseMyst, state.mystSubpoints.toInt())
        else -> calculateSubpoints(state.baseMoxie, state.moxieSubpoints.toInt())
    }

    /** Desktop ConditionsCommand level N → prime substat points to reach level. */
    fun substatPointsForLevel(targetLevel: Int, state: CharacterState): IntArray {
        val prime = primeIndex(state.characterClass)
        val counts = IntArray(3)
        val requiredTotal = calculateSubpoints((targetLevel - 1) * (targetLevel - 1) + 4, 0)
        val currentPrime = totalSubpoints(state, prime)
        counts[prime] = (requiredTotal - currentPrime).coerceAtLeast(0).toInt()
        return counts
    }

    /** Desktop N muscle/myst/mox condition — remaining subpoints for that stat. */
    fun remainingSubstatPoints(targetBase: Int, state: CharacterState, statIndex: Int): Int {
        val required = calculateSubpoints(targetBase, 0)
        val current = totalSubpoints(state, statIndex)
        return (required - current).coerceAtLeast(0).toInt()
    }
}
