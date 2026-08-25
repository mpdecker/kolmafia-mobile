package net.sourceforge.kolmafia.adventure.choice

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.MainStat
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.GoalManager

/**
 * Desktop [LouvreManager] goal routing for choices 904–913 (Phases 1701–1715).
 * Omits spoiler/decorate UI.
 */
object LouvreManager {

    const val FIRST_CHOICE = 904
    const val LAST_CHOICE = 913

    private val LOCATION_EXITS = arrayOf(
        intArrayOf(0, 0, 0), // 904
        intArrayOf(7, 908, 906), // 905
        intArrayOf(907, 6, 909), // 906
        intArrayOf(904, 908, 1), // 907
        intArrayOf(8, 911, 909), // 908
        intArrayOf(910, 4, 912), // 909
        intArrayOf(904, 911, 2), // 910
        intArrayOf(9, 905, 912), // 911
        intArrayOf(913, 5, 906), // 912
        intArrayOf(904, 905, 3), // 913
    )

    private val GOAL_ITEMS = intArrayOf(1949, 1950, 1951) // Manetwich, Vangoghbitussin, Pinot Renoir
    private const val GOAL_COUNT = 9

    private val nodeMarks = BooleanArray(LAST_CHOICE - FIRST_CHOICE + 1)

    fun louvreChoice(choice: Int): Boolean = choice in FIRST_CHOICE..LAST_CHOICE

    fun resetDecisions(
        preferences: Preferences,
        goalManager: GoalManager,
        characterState: CharacterState,
    ) {
        for (i in GOAL_ITEMS.indices) {
            if (goalManager.hasItemGoal(GOAL_ITEMS[i])) {
                preferences.setInt("louvreGoal", i + 1)
                return
            }
        }
        val desired = preferences.getInt("louvreDesiredGoal", 0)
        when (desired) {
            GOAL_COUNT + 1 -> preferences.setInt(
                "louvreGoal",
                when (characterState.mainStat) {
                    MainStat.MUSCLE -> 4
                    MainStat.MYSTICALITY -> 5
                    MainStat.MOXIE -> 6
                },
            )
            GOAL_COUNT + 2 -> {
                val mus = characterState.buffedMusc.toLong()
                val mys = characterState.buffedMyst.toLong()
                val mox = characterState.buffedMoxie.toLong()
                preferences.setInt(
                    "louvreGoal",
                    when {
                        mus <= mys && mus <= mox -> 4
                        mys <= mus && mys <= mox -> 5
                        else -> 6
                    },
                )
            }
            else -> if (desired > 0) preferences.setInt("louvreGoal", desired)
        }
    }

    fun handleChoice(
        source: Int,
        stepCount: Int,
        preferences: Preferences,
        goalManager: GoalManager,
        characterState: CharacterState,
    ): Int? {
        if (!louvreChoice(source)) return null
        val override = preferences.getString("louvreOverride", "")
        if (override.contains(",")) {
            val options = override.split(",").map { it.trim() }
            if (stepCount < options.size) {
                return when (options[stepCount].lowercase()) {
                    "up" -> 1
                    "down" -> 2
                    else -> 3
                }
            }
        }
        resetDecisions(preferences, goalManager, characterState)
        val goal = preferences.getInt("louvreGoal", 0)
        return pickNewExit(source, goal)
    }

    private fun choiceTuple(source: Int): IntArray? =
        if (louvreChoice(source)) LOCATION_EXITS[source - FIRST_CHOICE] else null

    private fun pickNewExit(source: Int, goal: Int): Int {
        val choices = choiceTuple(source) ?: return 1
        var choice = 0
        var hops = Int.MAX_VALUE
        for (i in choices.indices) {
            nodeMarks.fill(false)
            nodeMarks[source - FIRST_CHOICE] = true
            val dist = hopsTo(0, source, choices[i], goal)
            if (dist < hops) {
                choice = i
                hops = dist
            }
        }
        return choice + 1
    }

    private fun hopsTo(hops: Int, source: Int, destinationIn: Int, goal: Int): Int {
        var hopsAcc = hops
        var destination = destinationIn
        if (destination == 0) {
            hopsAcc += 20
            destination = LOCATION_EXITS[source - FIRST_CHOICE][0]
        }
        if (destination == goal) return hopsAcc
        if (destination in 1..GOAL_COUNT) return Int.MAX_VALUE
        if (!louvreChoice(destination)) return hopsAcc + 100
        if (nodeMarks[destination - FIRST_CHOICE]) return Int.MAX_VALUE
        nodeMarks[destination - FIRST_CHOICE] = true
        val choices = choiceTuple(destination) ?: return Int.MAX_VALUE
        var nextHops = Int.MAX_VALUE
        for (choice in choices) {
            val dist = hopsTo(hopsAcc + 1, destination, choice, goal)
            if (dist < nextHops) nextHops = dist
        }
        return nextHops
    }
}
