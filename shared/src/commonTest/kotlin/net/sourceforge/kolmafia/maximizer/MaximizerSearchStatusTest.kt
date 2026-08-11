package net.sourceforge.kolmafia.maximizer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MaximizerSearchStatusTest {

    @Test
    fun statusLines_includesProgressLineFirst() {
        val status = MaximizerSearchStatus(
            progressLine = "4 combinations checked, best score 5.00",
            scoreCapReached = true,
        )
        assertEquals(
            listOf(
                "4 combinations checked, best score 5.00",
                "(maximum achieved, no further combinations checked)",
            ),
            status.statusLines(),
        )
    }

    @Test
    fun statusLines_inDesktopOrder() {
        val status = MaximizerSearchStatus(
            combinationsChecked = 42,
            scoreCapReached = true,
            combinationLimitHit = true,
            interrupted = true,
        )
        assertEquals(
            listOf(
                "(maximum achieved, no further combinations checked)",
                "(hit combination limit, optimality not guaranteed)",
                "(interrupted, optimality not guaranteed)",
            ),
            status.statusLines(),
        )
    }

    @Test
    fun statusLines_emptyWhenNoFlags() {
        assertEquals(emptyList(), MaximizerSearchStatus().statusLines())
    }

    @Test
    fun fromComboBudget_copiesFlags() {
        val budget = ComboBudget(1)
        budget.tick()
        budget.tick()
        budget.markScoreCapReached()
        val status = MaximizerSearchStatus.from(budget)
        assertEquals(2, status.combinationsChecked)
        assertTrue(status.combinationLimitHit)
        assertTrue(status.scoreCapReached)
    }
}
