package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP523Test {

    @Test
    fun revision_phase523() {
        assertEquals("phase2450", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun alias_fullLine_keepsSemicolonInExpansion() {
        val prefs = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = prefs)
        val out = outputLib(lib, """cli_execute("alias foo => a; b");""")
        assertTrue(out.contains("String successfully aliased."))
        assertTrue(out.contains("foo => a; b"))
        assertEquals("a; b", lib.listCliAliases()["foo"])
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun alias_filter_listsMatching() {
        val prefs = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = prefs)
        lib.setCliAlias("foo", "a; b")
        lib.setCliAlias("bar", "echo x")
        val out = outputLib(lib, """cli_execute("alias foo");""")
        assertTrue(out.contains("foo => a; b"))
        assertFalse(out.contains("bar =>"))
    }

    @Test
    fun unalias_removes() {
        val prefs = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = prefs)
        lib.setCliAlias("foo", "a; b")
        val out = outputLib(lib, """cli_execute("unalias foo");""")
        assertTrue(out.contains("Alias removed."))
        assertTrue(lib.listCliAliases().isEmpty())
    }
}
