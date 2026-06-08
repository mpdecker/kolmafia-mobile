package net.sourceforge.kolmafia.adventure.choice.solvers

class ArcadeGameSolverImpl : ArcadeGameSolver {

    // 120-character script from desktop ArcadeRequest.java — EXACT, do not modify
    private val fistScript =
        "31111111111111111111111111111121121111111111111111111111111" +
        "1111111111111111211122211111121111111111111111122211133111113"

    init {
        require(fistScript.length == 120) { "FistScript must be exactly 120 chars, got ${fistScript.length}" }
    }

    override fun autoDungeonFist(stepCount: Int, responseText: String): Int? {
        if (stepCount < 0 || stepCount >= fistScript.length) return null

        // Shortcut: if "Finish from Memory" option is available, use it immediately.
        // Exact text match as in desktop ChoiceUtilities.actionOption().
        findActionOption("Finish from Memory", responseText)?.let { return it }

        return fistScript[stepCount].digitToInt()
    }

    /** Finds the option number whose text exactly equals [action]. Returns null if not found. */
    private fun findActionOption(action: String, responseText: String): Int? {
        val optionRegex = Regex("""option=(\d+)">([^<]+)""")
        return optionRegex.findAll(responseText)
            .firstOrNull { it.groupValues[2].trim() == action }
            ?.groupValues?.get(1)?.toIntOrNull()
    }
}
