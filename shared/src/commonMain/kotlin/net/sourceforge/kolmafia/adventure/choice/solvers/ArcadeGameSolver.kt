package net.sourceforge.kolmafia.adventure.choice.solvers

interface ArcadeGameSolver {
    /** Mirrors ArcadeRequest.autoDungeonFist(stepCount, responseText) */
    fun autoDungeonFist(stepCount: Int, responseText: String): Int?

    object NoOp : ArcadeGameSolver {
        override fun autoDungeonFist(stepCount: Int, responseText: String) = null
    }
}
