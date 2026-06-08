package net.sourceforge.kolmafia.adventure.choice.solvers

import net.sourceforge.kolmafia.preferences.Preferences

class GameproSolverImpl(private val preferences: Preferences) : GameproSolver {

    override fun autoSolve(stepCount: Int): Int? {
        val raw = preferences.getString("choiceAdventure665").trim()
        if (raw.isBlank()) return null
        val choices = raw.split(",").map { it.trim() }
        return choices.getOrNull(stepCount)?.toIntOrNull()
    }
}
