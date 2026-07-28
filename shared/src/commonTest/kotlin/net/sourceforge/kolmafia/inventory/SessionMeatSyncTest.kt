package net.sourceforge.kolmafia.inventory

import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.KoLCharacter

class SessionMeatSyncTest {

    @Test
    fun parseMeatGained_withCommas() {
        assertEquals(12_345, SessionMeatSync.parseMeatGained("You gain 12,345 Meat for the win."))
    }

    @Test
    fun parseMeatGained_missingPatternReturnsZero() {
        assertEquals(0, SessionMeatSync.parseMeatGained("<html>no meat here</html>"))
    }

    @Test
    fun apply_incrementsCharacterState() {
        val char = KoLCharacter()
        SessionMeatSync.apply(char, "You gain 500 Meat.")
        assertEquals(500L, char.state.value.sessionMeat)
    }

    @Test
    fun apply_accumulatesMultipleGains() {
        val char = KoLCharacter()
        SessionMeatSync.apply(char, "You gain 100 Meat.")
        SessionMeatSync.apply(char, "You gain 250 Meat.")
        assertEquals(350L, char.state.value.sessionMeat)
    }

    @Test
    fun apply_ignoresZeroOrMissingMeat() {
        val char = KoLCharacter().also { it.addSessionMeat(42L) }
        SessionMeatSync.apply(char, "<html>nothing</html>")
        assertEquals(42L, char.state.value.sessionMeat)
    }
}
