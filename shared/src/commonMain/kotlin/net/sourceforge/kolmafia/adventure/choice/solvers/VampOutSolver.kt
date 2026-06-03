package net.sourceforge.kolmafia.adventure.choice.solvers

interface VampOutSolver {
    /** Mirrors VampOutManager.autoVampOut(vampOutGoal, stepCount, responseText) */
    fun autoVampOut(preference: Int, stepCount: Int, responseText: String): Int?

    object NoOp : VampOutSolver {
        override fun autoVampOut(preference: Int, stepCount: Int, responseText: String) = null
    }
}
