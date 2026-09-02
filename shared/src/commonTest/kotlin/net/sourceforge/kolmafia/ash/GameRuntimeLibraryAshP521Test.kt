package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter

class GameRuntimeLibraryAshP521Test {

    @Test
    fun revision_phase521() {
        assertEquals("phase4430", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun numberology_forecast_whenNotCurrentlyAvailable() {
        val char = KoLCharacter()
        char.updateFromApiResponse(
            CharacterApiResponse(
                adventures = "0",
                level = "1",
                spleen = "1",
                ascensions = "0",
                moonsign = "0",
            ),
        )
        val out = outputLib(GameRuntimeLibrary(character = char), """cli_execute("numberology 17");""")
        assertTrue(out.contains("\"numberology 17\""))
        assertTrue(out.contains("1 Adventure"))
        assertTrue(out.contains("is not currently available but will be in"))
        assertTrue(out.contains("turn"))
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun numberology_checkOnly_currentlyAvailable() {
        val out = outputLib(
            GameRuntimeLibrary(character = KoLCharacter()),
            """cli_execute("numberology? 17");""",
        )
        assertTrue(out.contains("\"numberology 17\" (1 Adventure) is currently available."))
        assertFalse(out.contains("[cli]"))
    }
}
