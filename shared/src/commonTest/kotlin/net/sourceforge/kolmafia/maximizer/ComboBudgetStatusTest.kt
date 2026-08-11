package net.sourceforge.kolmafia.maximizer

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComboBudgetStatusTest {

    @AfterTest
    fun reset() {
        MaximizerContinuation.forceContinue()
    }

    @Test
    fun tick_setsLimitHitWhenExceeded() {
        val budget = ComboBudget(2)
        assertEquals(false, budget.tick())
        assertEquals(false, budget.limitHit)
        assertEquals(false, budget.tick())
        assertEquals(false, budget.limitHit)
        assertEquals(true, budget.tick())
        assertTrue(budget.limitHit)
        assertTrue(budget.exhausted())
        assertEquals(3, budget.combinationsChecked)
    }

    @Test
    fun tick_setsInterruptedWhenAborted() {
        val budget = ComboBudget(100)
        MaximizerContinuation.abort()
        assertEquals(true, budget.tick())
        assertTrue(budget.interrupted)
        assertEquals(0, budget.combinationsChecked)
    }

    @Test
    fun markScoreCapReached_setsFlag() {
        val budget = ComboBudget(100)
        assertEquals(false, budget.scoreCapReached)
        budget.markScoreCapReached()
        assertTrue(budget.scoreCapReached)
    }
}
