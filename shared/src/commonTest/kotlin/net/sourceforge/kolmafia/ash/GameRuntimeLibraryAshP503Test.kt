package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.maximizer.MaximizerContinuation

class GameRuntimeLibraryAshP503Test {

    @AfterTest
    fun tearDown() {
        MaximizerContinuation.forceContinue()
    }

    @Test
    fun revision_phase503() {
        assertEquals("phase3050", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun repeat_replaysPreviousCliExecute() {
        val lib = GameRuntimeLibrary()
        outputLib(lib, """cli_execute("echo hello");""")
        val out = outputLib(lib, """cli_execute("repeat");""")
        assertTrue(out.contains("Repetition 1 of 1..."))
        assertTrue(out.contains("hello"))
    }

    @Test
    fun repeat_n_replaysThatManyTimes() {
        val lib = GameRuntimeLibrary()
        outputLib(lib, """cli_execute("echo hello");""")
        val out = outputLib(lib, """cli_execute("repeat 3");""")
        assertTrue(out.contains("Repetition 1 of 3..."))
        assertTrue(out.contains("Repetition 2 of 3..."))
        assertTrue(out.contains("Repetition 3 of 3..."))
        assertEquals(3, Regex("hello").findAll(out).count())
    }

    @Test
    fun repeat_withNoPreviousLine_isNoOp() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("repeat");""")
        assertEquals("", out.trim())
    }

    @Test
    fun repeat_replaysWholePriorLineIncludingSemicolons() {
        val lib = GameRuntimeLibrary()
        outputLib(lib, """cli_execute("echo a; echo b");""")
        val out = outputLib(lib, """cli_execute("repeat");""")
        assertTrue(out.contains("a"))
        assertTrue(out.contains("b"))
    }

    @Test
    fun repeat_doesNotRunWhenAborted() {
        val lib = GameRuntimeLibrary()
        outputLib(lib, """cli_execute("echo hello");""")
        MaximizerContinuation.abort()
        val out = outputLib(lib, """cli_execute("repeat 3");""")
        assertFalse(out.contains("hello"))
        assertFalse(out.contains("Repetition"))
    }
}
