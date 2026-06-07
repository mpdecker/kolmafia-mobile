package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryCliTest {

    @Test
    fun cliExecute_setCommand_writesPreference() {
        val p = prefs()
        val lib = GameRuntimeLibrary(preferences = p)
        runLib(lib, """cli_execute("set myPref=hello");""")
        assertEquals("hello", p.getString("myPref", ""))
    }

    @Test
    fun cliExecute_getCommand_printsPrefValue() {
        val p = prefs()
        p.setString("myKey", "someValue")
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("get myKey");""")
        assertEquals("someValue", out)
    }

    @Test
    fun cliExecute_unknownCommand_echoesWithCliPrefix() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("doSomethingWeird");""")
        assertEquals("[cli] doSomethingWeird", out)
    }

    @Test
    fun cliExecute_alwaysReturnsTrue() {
        val lib = GameRuntimeLibrary()
        // cli_execute returns boolean true; print captures it alongside the echo fallback
        val out = outputLib(lib, """boolean result = cli_execute("anything"); print(result);""")
        assertTrue(out.contains("true"), "Expected output to contain 'true', got: $out")
    }
}
