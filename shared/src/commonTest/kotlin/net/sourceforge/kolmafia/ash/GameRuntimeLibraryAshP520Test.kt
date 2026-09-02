package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.KoLCharacter

class GameRuntimeLibraryAshP520Test {

    @Test
    fun revision_phase520() {
        assertEquals("phase4430", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun numberology_knownPrize() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("numberology 17");""")
        assertTrue(out.contains("1 Adventure"))
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun numberology_tryAgain() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("numberology 24");""")
        assertTrue(out.contains("Result 24 is Try Again"))
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun numberology_listsCalculateTheUniverse() {
        val out = outputLib(
            GameRuntimeLibrary(character = KoLCharacter()),
            """cli_execute("numberology");""",
        )
        assertTrue(out.contains("Calculate the Universe"))
        assertFalse(out.contains("[cli]"))
    }
}
