package net.sourceforge.kolmafia.maximizer

/** Desktop Maximizer search outcome flags surfaced in speculate/maximize output. */
data class MaximizerSearchStatus(
    val combinationsChecked: Int = 0,
    val combinationLimitHit: Boolean = false,
    val scoreCapReached: Boolean = false,
    val interrupted: Boolean = false,
    val progressLine: String? = null,
) {
    fun statusLines(): List<String> = buildList {
        progressLine?.let { add(it) }
        if (scoreCapReached) add("(maximum achieved, no further combinations checked)")
        if (combinationLimitHit) add("(hit combination limit, optimality not guaranteed)")
        if (interrupted) add("(interrupted, optimality not guaranteed)")
    }

    companion object {
        fun from(budget: ComboBudget, progressLine: String? = null): MaximizerSearchStatus =
            MaximizerSearchStatus(
                combinationsChecked = budget.combinationsChecked,
                combinationLimitHit = budget.limitHit,
                scoreCapReached = budget.scoreCapReached,
                interrupted = budget.interrupted,
                progressLine = progressLine,
            )
    }
}
