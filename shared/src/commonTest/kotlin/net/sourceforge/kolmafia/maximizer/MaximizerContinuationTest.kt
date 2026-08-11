package net.sourceforge.kolmafia.maximizer

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MaximizerContinuationTest {

    @AfterTest
    fun reset() {
        MaximizerContinuation.forceContinue()
    }

    @Test
    fun forceContinue_clearsAbort() {
        MaximizerContinuation.abort()
        assertFalse(MaximizerContinuation.permitsContinue())
        MaximizerContinuation.forceContinue()
        assertTrue(MaximizerContinuation.permitsContinue())
    }

    @Test
    fun abort_stopsContinuation() {
        MaximizerContinuation.forceContinue()
        assertTrue(MaximizerContinuation.permitsContinue())
        MaximizerContinuation.abort()
        assertFalse(MaximizerContinuation.permitsContinue())
    }
}
