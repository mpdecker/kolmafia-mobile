package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.maximizer.MaximizerContinuation

class GameRuntimeLibraryAshP509Test {

    @AfterTest
    fun tearDown() {
        MaximizerContinuation.forceContinue()
    }

    @Test
    fun revision_phase509() {
        assertEquals("phase1070", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun try_runsContinuation() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("try; echo ok");""")
        assertTrue(out.contains("ok"))
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun try_abort_runsElse() {
        val lib = GameRuntimeLibrary()
        val aborted = outputLib(lib, """cli_execute("try; abort");""")
        val recovered = outputLib(lib, """cli_execute("else; echo recovered");""")
        assertTrue(aborted.contains("Script abort."))
        assertTrue(recovered.contains("recovered"))
        assertFalse(recovered.contains("[cli]"))
    }

    @Test
    fun try_withCondition_printsError() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("try 1 == 1; echo x");""")
        assertTrue(out.contains("Condition not allowed for try."))
        assertFalse(out.lines().any { it.trim() == "x" })
    }
}
