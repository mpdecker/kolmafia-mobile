package net.sourceforge.kolmafia.adventure.choice.solvers

interface GameproSolver {
    /** Mirrors GameproManager.autoSolve(stepCount) */
    fun autoSolve(stepCount: Int): Int?

    object NoOp : GameproSolver {
        override fun autoSolve(stepCount: Int) = null
    }
}
