package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.maximizer.MaximizerContinuation

class GameRuntimeLibraryAshP507Test {

    @AfterTest
    fun tearDown() {
        MaximizerContinuation.forceContinue()
    }

    @Test
    fun revision_phase507() {
        assertEquals("phase1070", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun while_false_neverRunsBody() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("while 0 > 1; echo never");""")
        assertFalse(out.contains("never"))
    }

    @Test
    fun while_true_runsUntilAbort() {
        val lib = GameRuntimeLibrary()
        val out = try {
            outputLib(lib, """cli_execute("while 1 == 1; echo once; abort");""")
        } catch (_: ScriptException) {
            lib.lastCliOutput.toString()
        }
        assertTrue(out.contains("once"))
        assertFalse(out.contains("[cli]"))
        assertEquals(1, Regex("once").findAll(out).count())
    }
}
