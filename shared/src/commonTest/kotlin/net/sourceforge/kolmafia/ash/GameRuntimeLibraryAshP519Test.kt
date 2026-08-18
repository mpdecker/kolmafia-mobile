package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class GameRuntimeLibraryAshP519Test {

    @Test
    fun revision_phase519() {
        assertEquals("phase635", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun set_keepsSemicolonInValue() {
        val p = prefs()
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("set foo = a; b");""")
        assertEquals("a; b", p.getString("foo", ""))
        assertEquals("foo => a; b", out.trim())
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun get_printsStoredValue() {
        val p = prefs()
        p.setString("foo", "a; b")
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("get foo");""")
        assertEquals("a; b", out)
    }

    @Test
    fun set_withoutEquals_printsValue() {
        val p = prefs()
        p.setString("foo", "stored")
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("set foo");""")
        assertEquals("stored", out)
    }

    @Test
    fun set_stripsWrappingQuotes() {
        val p = prefs()
        val lib = GameRuntimeLibrary(preferences = p)
        outputLib(lib, """cli_execute("set bar = \"quoted\"");""")
        assertEquals("quoted", p.getString("bar", ""))
    }
}
