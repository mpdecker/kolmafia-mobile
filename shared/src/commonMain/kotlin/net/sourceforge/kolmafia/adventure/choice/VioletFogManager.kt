package net.sourceforge.kolmafia.adventure.choice

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.MainStat
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.GoalManager

/**
 * Desktop [VioletFogManager] routing engine (Phases 1671–1685).
 * Omits wiki graph / relay decorate.
 */
object VioletFogManager {

    const val FIRST_CHOICE = 48
    const val LAST_CHOICE = 70
    private const val FIRST_GOAL_LOCATION = 62

    private val CHOICE_PATTERN = Regex("""whichchoice\s+value=(\d+)""", RegexOption.IGNORE_CASE)

    private val LOCATION_EXITS = arrayOf(
        intArrayOf(49, 50, 51), // 48
        intArrayOf(52, 53, 56), // 49
        intArrayOf(53, 54, 57), // 50
        intArrayOf(52, 54, 55), // 51
        intArrayOf(61, 65, 68), // 52
        intArrayOf(61, 66, 69), // 53
        intArrayOf(61, 67, 70), // 54
        intArrayOf(58, 65, 70), // 55
        intArrayOf(59, 66, 68), // 56
        intArrayOf(60, 67, 69), // 57
        intArrayOf(51, 52, 63), // 58
        intArrayOf(49, 53, 62), // 59
        intArrayOf(50, 54, 64), // 60
        intArrayOf(49, 50, 51), // 61
        intArrayOf(50, 52, 61), // 62
        intArrayOf(51, 53, 61), // 63
        intArrayOf(49, 54, 61), // 64
        intArrayOf(50, 51, 54), // 65
        intArrayOf(49, 51, 52), // 66
        intArrayOf(49, 50, 53), // 67
        intArrayOf(49, 50, 53), // 68
        intArrayOf(50, 51, 54), // 69
        intArrayOf(49, 51, 52), // 70
    )

    /** Goal item ids indexed by violetFogGoal 1–9 (0 = escape). */
    private val GOAL_ITEMS = intArrayOf(
        0,
        1615, // C_CLOCHE
        1617, // C_CROSSBOW
        1616, // C_CULOTTES
        0, 0, 0,
        1618, // ICE_STEIN
        1619, // MUNCHIES_PILL
        1620, // HOMEOPATHIC
    )

    // FogChoiceTable[source-48][decision-1] = dest choice, 0 unknown, -1 goal
    private val choiceTable = Array(LAST_CHOICE - FIRST_CHOICE + 1) { IntArray(4) }

    // routingTable[sourceIdx][destIdx] = (nextHop, hopCount); destIdx is destination-49
    private val routingTable: Array<Array<IntArray>> = buildRoutingTable()

    fun fogChoice(choice: Int): Boolean = choice in FIRST_CHOICE..LAST_CHOICE

    fun reset(preferences: Preferences, ascensions: Int) {
        for (i in FIRST_CHOICE..LAST_CHOICE) {
            val row = choiceTable[i - FIRST_CHOICE]
            row[0] = if (i < FIRST_GOAL_LOCATION) 0 else -1
            row[1] = 0
            row[2] = 0
            row[3] = if (i < FIRST_GOAL_LOCATION) -1 else 0
        }
        val lastMap = preferences.getInt("lastVioletFogMap", 0)
        if (lastMap != ascensions) {
            preferences.setInt("lastVioletFogMap", ascensions)
            preferences.setString("violetFogLayout", "")
        }
        val layout = preferences.getString("violetFogLayout", "")
        if (layout.isBlank()) return
        val parts = layout.split(",")
        var idx = 0
        for (i in choiceTable.indices) {
            for (j in choiceTable[i].indices) {
                if (idx >= parts.size) return
                choiceTable[i][j] = parts[idx++].toIntOrNull() ?: 0
            }
        }
    }

    fun saveMap(preferences: Preferences, ascensions: Int) {
        val map = buildString {
            for (i in choiceTable.indices) {
                for (j in choiceTable[i].indices) {
                    if (i != 0 || j != 0) append(',')
                    append(choiceTable[i][j])
                }
            }
        }
        preferences.setInt("lastVioletFogMap", ascensions)
        preferences.setString("violetFogLayout", map)
    }

    fun handleChoice(
        source: Int,
        preferences: Preferences,
        goalManager: GoalManager,
        characterState: CharacterState,
    ): Int? {
        if (!fogChoice(source)) return null
        val goal = parseGoal(preferences, goalManager, characterState)
        if (goal == 0) return 4
        val destination = FIRST_GOAL_LOCATION + goal - 1
        if (!fogChoice(destination)) return null
        if (source == destination) return 1
        val nextHop = nextHop(source, destination)
        if (nextHop < 0) return null
        val path = choiceTable[source - FIRST_CHOICE]
        for (i in path.indices) {
            if (path[i] == nextHop) return i + 1
        }
        for (i in path.indices) {
            if (path[i] == 0) return i + 1
        }
        return null
    }

    fun mapChoice(
        lastChoice: Int,
        lastDecision: Int,
        text: String,
        preferences: Preferences,
        ascensions: Int,
    ): Boolean {
        if (!fogChoice(lastChoice)) return false
        if (lastDecision !in 1..4) return true
        if (choiceTable[lastChoice - FIRST_CHOICE][lastDecision - 1] != 0) return true
        val source = CHOICE_PATTERN.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return false
        if (!fogChoice(source)) return false
        val choices = choiceTable[lastChoice - FIRST_CHOICE]
        choices[lastDecision - 1] = source
        saveMap(preferences, ascensions)

        var unknownIndex = -1
        for (i in choices.indices) {
            if (choices[i] != 0) continue
            if (unknownIndex != -1) return true
            unknownIndex = i
        }
        if (unknownIndex == -1) return true
        for (exit in LOCATION_EXITS[lastChoice - FIRST_CHOICE]) {
            if (choices.none { it == exit }) {
                choices[unknownIndex] = exit
                saveMap(preferences, ascensions)
                return true
            }
        }
        return true
    }

    /** Exposed for tests. */
    internal fun nextHop(source: Int, destination: Int): Int {
        val tuple = routingTuple(source, destination) ?: return -1
        return tuple[0]
    }

    internal fun choiceAt(source: Int, decision: Int): Int =
        choiceTable[source - FIRST_CHOICE][decision - 1]

    internal fun setChoiceForTest(source: Int, decision: Int, dest: Int) {
        choiceTable[source - FIRST_CHOICE][decision - 1] = dest
    }

    private fun parseGoal(
        preferences: Preferences,
        goalManager: GoalManager,
        characterState: CharacterState,
    ): Int {
        for (i in 1 until GOAL_ITEMS.size) {
            val id = GOAL_ITEMS[i]
            if (id > 0 && goalManager.hasItemGoal(id)) return i
        }
        var goal = preferences.getInt("violetFogGoal", 0)
        if (goal < 0 || goal > 11) return -1
        when (goal) {
            10 -> goal = when (characterState.mainStat) {
                MainStat.MUSCLE -> 4
                MainStat.MYSTICALITY -> 5
                MainStat.MOXIE -> 6
            }
            11 -> {
                val mus = characterState.buffedMusc.toLong()
                val mys = characterState.buffedMyst.toLong()
                val mox = characterState.buffedMoxie.toLong()
                goal = when {
                    mus <= mys && mus <= mox -> 4
                    mys <= mus && mys <= mox -> 5
                    else -> 6
                }
            }
        }
        return goal
    }

    private fun routingTuple(source: Int, destination: Int): IntArray? {
        if (source !in FIRST_CHOICE..LAST_CHOICE) return null
        if (destination !in (FIRST_CHOICE + 1)..LAST_CHOICE) return null
        return routingTable[source - FIRST_CHOICE][destination - FIRST_CHOICE - 1]
    }

    private fun buildRoutingTable(): Array<Array<IntArray>> {
        val sources = LAST_CHOICE - FIRST_CHOICE + 1
        val dests = LAST_CHOICE - FIRST_CHOICE
        val table = Array(sources) { Array(dests) { IntArray(2) } }
        fun tuple(source: Int, destination: Int): IntArray =
            table[source - FIRST_CHOICE][destination - FIRST_CHOICE - 1]

        var unfilled = sources * dests
        for (source in (FIRST_CHOICE + 1)..LAST_CHOICE) {
            val t = tuple(source, source)
            t[0] = -1
            t[1] = 0
            unfilled--
        }
        for (source in FIRST_CHOICE..LAST_CHOICE) {
            for (destination in LOCATION_EXITS[source - FIRST_CHOICE]) {
                val t = tuple(source, destination)
                t[0] = destination
                t[1] = 1
                unfilled--
            }
        }
        while (unfilled > 0) {
            var filled = 0
            for (source in FIRST_CHOICE..LAST_CHOICE) {
                for (destination in (FIRST_CHOICE + 1)..LAST_CHOICE) {
                    val t = tuple(source, destination)
                    if (t[0] != 0) continue
                    var nextHop = 0
                    var hopCount = Int.MAX_VALUE
                    for (exit in LOCATION_EXITS[source - FIRST_CHOICE]) {
                        val destTuple = tuple(exit, destination)
                        if (destTuple[0] != 0 && destTuple[1] < hopCount) {
                            nextHop = exit
                            hopCount = destTuple[1]
                        }
                    }
                    if (nextHop != 0) {
                        t[0] = nextHop
                        t[1] = hopCount + 1
                        filled++
                    }
                }
            }
            if (filled == 0) break
            unfilled -= filled
        }
        return table
    }
}
