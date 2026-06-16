package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

/**
 * ASH-P20 behavioral batch — Naughty Sorceress hedge maze automation prefs.
 */
internal fun GameRuntimeLibrary.registerAshP20Batch(scope: AshScope) {
    regFn(scope, "hedge_maze", AshType.BOOLEAN, listOf("tag" to AshType.STRING)) { _, args ->
        val tag = args[0].toString().lowercase()
        val mode = when (tag) {
            "traps" -> HedgeMazeMode.TRAPS
            "gopher", "duck" -> HedgeMazeMode.GOPHER_DUCK
            "chihuahua", "kiwi" -> HedgeMazeMode.CHIHUAHUA_KIWI
            "nugglets" -> HedgeMazeMode.NUGGLETS
            else -> return@regFn AshValue.FALSE
        }
        AshValue.of(configureHedgeMaze(mode))
    }
}

private enum class HedgeMazeMode {
    TRAPS,
    GOPHER_DUCK,
    CHIHUAHUA_KIWI,
    NUGGLETS,
}

private fun GameRuntimeLibrary.configureHedgeMaze(mode: HedgeMazeMode): Boolean {
    val db = questDatabase ?: return false
    if (db.getProgress(Quest.FINAL) != "step4") return false
    val prefs = preferences ?: return false
    when (mode) {
        HedgeMazeMode.TRAPS -> {
            prefs.setInt("choiceAdventure1005", 2)
            prefs.setInt("choiceAdventure1008", 2)
            prefs.setInt("choiceAdventure1011", 2)
            prefs.setInt("choiceAdventure1013", 1)
            prefs.setInt("choiceAdventure1006", 1)
            prefs.setInt("choiceAdventure1007", 1)
            prefs.setInt("choiceAdventure1009", 1)
            prefs.setInt("choiceAdventure1010", 1)
            prefs.setInt("choiceAdventure1012", 1)
        }
        HedgeMazeMode.GOPHER_DUCK -> {
            prefs.setInt("choiceAdventure1005", 1)
            prefs.setInt("choiceAdventure1006", 2)
            prefs.setInt("choiceAdventure1008", 1)
            prefs.setInt("choiceAdventure1009", 2)
            prefs.setInt("choiceAdventure1011", 1)
            prefs.setInt("choiceAdventure1012", 1)
            prefs.setInt("choiceAdventure1013", 1)
            prefs.setInt("choiceAdventure1007", 1)
            prefs.setInt("choiceAdventure1010", 1)
        }
        HedgeMazeMode.CHIHUAHUA_KIWI -> {
            prefs.setInt("choiceAdventure1005", 1)
            prefs.setInt("choiceAdventure1006", 1)
            prefs.setInt("choiceAdventure1007", 2)
            prefs.setInt("choiceAdventure1009", 1)
            prefs.setInt("choiceAdventure1010", 2)
            prefs.setInt("choiceAdventure1012", 1)
            prefs.setInt("choiceAdventure1013", 1)
            prefs.setInt("choiceAdventure1008", 1)
            prefs.setInt("choiceAdventure1011", 1)
        }
        HedgeMazeMode.NUGGLETS -> {
            for (room in 1005..1013) {
                prefs.setInt("choiceAdventure$room", 1)
            }
        }
    }
    return true
}
