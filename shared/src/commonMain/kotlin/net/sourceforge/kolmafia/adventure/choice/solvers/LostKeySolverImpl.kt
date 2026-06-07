package net.sourceforge.kolmafia.adventure.choice.solvers

class LostKeySolverImpl : LostKeySolver {

    // Scripts from desktop LostKeyManager.java. preference (1-3) selects index (pref-1).
    private val keyScripts = arrayOf(
        "121111",   // goal 1: glasses
        "131212",   // goal 2: comb
        "131113",   // goal 3: pill bottle
    )

    override fun autoKey(preference: Int, stepCount: Int, responseText: String): Int? {
        val script = keyScripts.getOrNull(preference - 1) ?: return null
        return script.getOrNull(stepCount)?.digitToInt()
    }
}
