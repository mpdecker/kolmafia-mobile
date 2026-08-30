package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.maximizer.MaximizerContinuation

class GameRuntimeLibraryAshP508Test {

    @AfterTest
    fun tearDown() {
        MaximizerContinuation.forceContinue()
    }

    @Test
    fun revision_phase508() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun if_true_skipsElse() {
        val lib = GameRuntimeLibrary()
        val yes = outputLib(lib, """cli_execute("if 1 == 1; echo yes");""")
        val no = outputLib(lib, """cli_execute("else; echo no");""")
        assertTrue(yes.contains("yes"))
        assertFalse(no.contains("no"))
        assertFalse(no.contains("must follow a conditional"))
    }

    @Test
    fun if_false_runsElse() {
        val lib = GameRuntimeLibrary()
        val skipped = outputLib(lib, """cli_execute("if 1 == 0; echo yes");""")
        val ran = outputLib(lib, """cli_execute("else; echo no");""")
        assertFalse(skipped.contains("yes"))
        assertTrue(ran.contains("no"))
        assertFalse(ran.contains("[cli]"))
    }

    @Test
    fun elseif_chain_runsFirstTrueBranch() {
        val lib = GameRuntimeLibrary()
        outputLib(lib, """cli_execute("if 1 == 0; echo a");""")
        val mid = outputLib(lib, """cli_execute("elseif 1 == 1; echo b");""")
        val last = outputLib(lib, """cli_execute("else; echo c");""")
        assertTrue(mid.contains("b"))
        assertFalse(last.contains("c"))
    }

    @Test
    fun elseif_false_fallsThroughToElse() {
        val lib = GameRuntimeLibrary()
        outputLib(lib, """cli_execute("if 1 == 0; echo a");""")
        val mid = outputLib(lib, """cli_execute("elseif 1 == 0; echo b");""")
        val last = outputLib(lib, """cli_execute("else; echo c");""")
        assertFalse(mid.contains("b"))
        assertTrue(last.contains("c"))
    }

    @Test
    fun stray_else_printsError() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("else; echo no");""")
        assertTrue(
            out.contains(
                "'else' must follow a conditional command, and both must be at the outermost level.",
            ),
        )
        assertFalse(out.lines().any { it.trim() == "no" })
    }

    @Test
    fun else_withCondition_printsError() {
        val lib = GameRuntimeLibrary()
        outputLib(lib, """cli_execute("if 1 == 0; echo a");""")
        val out = outputLib(lib, """cli_execute("else 1 == 1; echo no");""")
        assertTrue(out.contains("Condition not allowed for else."))
        assertFalse(out.lines().any { it.trim() == "no" })
    }
}
