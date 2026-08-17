package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP511Test {

    @Test
    fun revision_phase511() {
        assertEquals("phase605", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun ashwiki_printsKolmafiaWikiSearchUrl() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("ashwiki maximize");""")
        assertTrue(out.contains("https://wiki.kolmafia.us/index.php?search="))
        assertTrue(out.contains("maximize"))
    }

    @Test
    fun ashwiki_encodesSpaces() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("ashwiki cli execute");""")
        assertTrue(out.contains("wiki.kolmafia.us"))
        assertTrue(out.contains("cli+execute") || out.contains("cli%20execute"))
    }
}
