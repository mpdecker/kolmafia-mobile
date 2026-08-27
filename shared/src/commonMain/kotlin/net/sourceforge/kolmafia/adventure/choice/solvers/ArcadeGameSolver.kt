package net.sourceforge.kolmafia.adventure.choice.solvers

interface ArcadeGameSolver {
    /** Mirrors ArcadeRequest.autoDungeonFist(stepCount, responseText) */
    fun autoDungeonFist(stepCount: Int, responseText: String): Int?

    /** Mirrors ArcadeRequest.autoChoiceFightersOfFighting */
    fun autoFightersOfFighting(responseText: String): Int? = FightersOfFighting.autoChoice(responseText)

    object NoOp : ArcadeGameSolver {
        override fun autoDungeonFist(stepCount: Int, responseText: String) = null
        override fun autoFightersOfFighting(responseText: String) = null
    }
}
