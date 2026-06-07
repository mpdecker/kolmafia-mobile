package net.sourceforge.kolmafia.adventure.choice.solvers

class SafetyShelterSolverImpl : SafetyShelterSolver {

    // Scripts from desktop SafetyShelterManager.java. preference (1-6) selects index (pref-1).
    private val ronaldScripts = arrayOf(
        "11211",   // goal 1
        "1122",    // goal 2
        "12211",   // goal 3
        "12221",   // goal 4
        "1321",    // goal 5
        "1322",    // goal 6
    )
    private val grimaceScripts = arrayOf(
        "1121",    // goal 1
        "1122",    // goal 2
        "1211",    // goal 3
        "12121",   // goal 4
        "13211",   // goal 5
        "12221",   // goal 6
    )

    override fun autoRonald(preference: Int, stepCount: Int, responseText: String): Int? {
        val script = ronaldScripts.getOrNull(preference - 1) ?: return null
        return script.getOrNull(stepCount)?.digitToInt()
    }

    override fun autoGrimace(preference: Int, stepCount: Int, responseText: String): Int? {
        val script = grimaceScripts.getOrNull(preference - 1) ?: return null
        return script.getOrNull(stepCount)?.digitToInt()
    }
}
