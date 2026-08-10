package net.sourceforge.kolmafia.maximizer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MaximizerBoostVerboseSuffixTest {

    @Test
    fun appendVerboseBrackets_durationAndUses() {
        val text = MaximizerBoostVerboseSuffix.appendVerboseBrackets(
            "eat food (+5)",
            MaximizerBoostVerboseSuffix.BracketInfo(duration = 3, usesRemaining = 2),
            verboseMaximizer = true,
        )
        assertTrue(text.contains("3 advs duration"))
        assertTrue(text.contains("2 uses remaining"))
    }

    @Test
    fun appendVerboseBrackets_singleUseAndIntrinsic() {
        val single = MaximizerBoostVerboseSuffix.appendVerboseBrackets(
            "cmd (+1)",
            MaximizerBoostVerboseSuffix.BracketInfo(usesRemaining = 1),
            verboseMaximizer = true,
        )
        assertTrue(single.contains("1 use remaining"))

        val intrinsic = MaximizerBoostVerboseSuffix.appendVerboseBrackets(
            "cmd (+1)",
            MaximizerBoostVerboseSuffix.BracketInfo(duration = 999),
            verboseMaximizer = true,
        )
        assertTrue(intrinsic.contains("intrinsic"))
    }

    @Test
    fun appendVerboseBrackets_skipsWhenDisabled() {
        val text = MaximizerBoostVerboseSuffix.appendVerboseBrackets(
            "eat food (+5)",
            MaximizerBoostVerboseSuffix.BracketInfo(duration = 3),
            verboseMaximizer = false,
        )
        assertEquals("eat food (+5)", text)
    }

    @Test
    fun appendVerboseBrackets_meatCost() {
        val text = MaximizerBoostVerboseSuffix.appendVerboseBrackets(
            "horsery normal horse (+2)",
            MaximizerBoostVerboseSuffix.BracketInfo(meatCost = 500),
            verboseMaximizer = true,
        )
        assertTrue(text.contains("[500 meat]"))
    }
}
