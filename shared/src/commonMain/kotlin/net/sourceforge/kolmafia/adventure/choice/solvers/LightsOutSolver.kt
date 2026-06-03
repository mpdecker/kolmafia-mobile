package net.sourceforge.kolmafia.adventure.choice.solvers

interface LightsOutSolver {
    /** Mirrors ChoiceManager.lightsOutAutomation(choice, responseText) */
    fun autoLightsOut(choiceId: Int, responseText: String): Int?

    object NoOp : LightsOutSolver {
        override fun autoLightsOut(choiceId: Int, responseText: String) = null
    }
}
