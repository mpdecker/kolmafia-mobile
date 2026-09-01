package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP500Test {

    @Test
    fun revision_phase500() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun help_pull_matchesPull() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("help pull");""")
        assertTrue(out.lines().any { it.equals("pull", ignoreCase = true) })
    }

    @Test
    fun help_help_printsOptionalBlurb() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("help help");""")
        assertTrue(out.contains("brackets", ignoreCase = true) || out.contains("optional", ignoreCase = true))
    }

    @Test
    fun which_alias_listsMatchingCommand() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("which lookup");""")
        assertTrue(out.lines().any { it.equals("lookup", ignoreCase = true) })
    }

    @Test
    fun lookup_item_printsWikiUrl() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("lookup item seal tooth");""")
        assertEquals("https://wiki.a.kolmafia.us/wiki/seal_tooth", out.trim())
    }

    @Test
    fun lookup_unknownType_printsUrlForRemainder() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("lookup widget seal tooth");""")
        assertEquals("https://wiki.a.kolmafia.us/wiki/widget_seal_tooth", out.trim())
    }
}
