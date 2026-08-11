package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.ash.currentTimeMillis
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MaximizerProgressTest {

    @AfterTest
    fun tearDown() {
        MaximizerProgress.reset()
        MaximizerProgress.clockMs = { currentTimeMillis() }
        MaximizerProgress.sink = {}
    }

    @Test
    fun format_includesFailSuffixWhenConstraintsFail() {
        assertEquals(
            "12 combinations checked, best score 5.00 (FAIL)",
            MaximizerProgress.format(12, 5.0, failed = true),
        )
        assertEquals(
            "3 combinations checked, best score 1.25",
            MaximizerProgress.format(3, 1.25, failed = false),
        )
    }

    @Test
    fun maybeShow_throttlesWithinInterval() {
        var now = 0L
        MaximizerProgress.clockMs = { now }
        val messages = mutableListOf<String>()
        MaximizerProgress.reset()
        MaximizerProgress.sink = { messages += it }

        MaximizerProgress.maybeShow(1, 1.0, false)
        assertEquals(1, messages.size)

        now = 2_000L
        MaximizerProgress.maybeShow(2, 2.0, false)
        assertEquals(1, messages.size, "second call within 5s must be suppressed")

        now = 5_001L
        MaximizerProgress.maybeShow(3, 3.0, false)
        assertEquals(2, messages.size)
        assertEquals("3 combinations checked, best score 3.00", messages.last())
    }

    @Test
    fun showFinal_alwaysEmitsEvenWhenThrottled() {
        var now = 0L
        MaximizerProgress.clockMs = { now }
        val messages = mutableListOf<String>()
        MaximizerProgress.reset()
        MaximizerProgress.sink = { messages += it }

        MaximizerProgress.maybeShow(1, 1.0, false)
        now = 100L
        MaximizerProgress.showFinal(9, 9.0, false)
        assertEquals(2, messages.size)
        assertEquals("9 combinations checked, best score 9.00", messages.last())
        assertEquals("9 combinations checked, best score 9.00", MaximizerProgress.lastMessage)
    }
}
