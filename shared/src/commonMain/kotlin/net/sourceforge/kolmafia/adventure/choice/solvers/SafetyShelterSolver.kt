package net.sourceforge.kolmafia.adventure.choice.solvers

interface SafetyShelterSolver {
    /** Mirrors SafetyShelterManager.autoRonald(decision, stepCount, responseText) */
    fun autoRonald(preference: Int, stepCount: Int, responseText: String): Int?
    /** Mirrors SafetyShelterManager.autoGrimace(decision, stepCount, responseText) */
    fun autoGrimace(preference: Int, stepCount: Int, responseText: String): Int?

    object NoOp : SafetyShelterSolver {
        override fun autoRonald(preference: Int, stepCount: Int, responseText: String) = null
        override fun autoGrimace(preference: Int, stepCount: Int, responseText: String) = null
    }
}
