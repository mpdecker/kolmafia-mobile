package net.sourceforge.kolmafia.character

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KoLCharacterTest {

    @Test
    fun initialState_isDefault() {
        val character = KoLCharacter()
        val state = character.state.value
        assertEquals("", state.name)
        assertEquals(0, state.playerId)
        assertFalse(state.isLoggedIn)
    }

    @Test
    fun updateFromApiResponse_populatesAllFields() {
        val character = KoLCharacter()
        character.updateFromApiResponse(
            CharacterApiResponse(
                name = "TestPlayer",
                playerid = "12345",
                level = "10",
                classId = "3",
                hp = "150",
                hpmax = "200",
                mp = "80",
                mpmax = "100",
                meat = "5000",
                adventures = "40",
                fullness = "5",
                drunk = "2",
                spleen = "1"
            )
        )
        val state = character.state.value
        assertEquals("TestPlayer", state.name)
        assertEquals(12345, state.playerId)
        assertEquals(10, state.level)
        assertEquals(150, state.currentHp)
        assertEquals(200, state.maxHp)
        assertEquals(80, state.currentMp)
        assertEquals(100, state.maxMp)
        assertEquals(5000, state.meat)
        assertEquals(40, state.adventuresLeft)
        assertEquals(5, state.fullness)
        assertEquals(2, state.inebriety)
        assertEquals(1, state.spleenUsed)
        assertTrue(state.isLoggedIn)
    }

    @Test
    fun updateFromApiResponse_handlesMalformedNumbers_withZero() {
        val character = KoLCharacter()
        character.updateFromApiResponse(CharacterApiResponse(name = "Player", hp = "not-a-number"))
        assertEquals(0, character.state.value.currentHp)
    }

    @Test
    fun reset_clearsState() {
        val character = KoLCharacter()
        character.updateFromApiResponse(CharacterApiResponse(name = "Player", hp = "100", hpmax = "100"))
        character.reset()
        val state = character.state.value
        assertEquals("", state.name)
        assertFalse(state.isLoggedIn)
    }
}
