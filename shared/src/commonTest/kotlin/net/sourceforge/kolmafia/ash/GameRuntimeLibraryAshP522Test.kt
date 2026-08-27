package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.NumberologyRequest

class GameRuntimeLibraryAshP522Test {

    @Test
    fun revision_phase522() {
        assertEquals("phase3050", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun numberology_submitsWhenCurrentlyAvailable() {
        val submitted = mutableListOf<Int>()
        val fake = object : NumberologyRequest(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })) {
            override suspend fun calculate(
                seed: Int,
                preferences: Preferences?,
                characterState: CharacterState,
            ): Result<String> {
                submitted += seed
                return Result.success("ok")
            }
        }
        val char = KoLCharacter()
        char.updateFromApiResponse(
            CharacterApiResponse(
                adventures = "5",
                level = "1",
                spleen = "0",
                ascensions = "0",
                moonsign = "0",
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setInt(NumberologyRequest.PREF_SKILL_LEVEL, 1)
        prefs.setInt(NumberologyRequest.PREF_UNIVERSE_CALCULATED, 0)
        val out = outputLib(
            GameRuntimeLibrary(
                character = char,
                preferences = prefs,
                numberologyRequest = fake,
            ),
            """cli_execute("numberology 17");""",
        )
        assertEquals(listOf(12), submitted)
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun numberology_checkOnly_doesNotSubmit() {
        var called = false
        val fake = object : NumberologyRequest(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })) {
            override suspend fun calculate(
                seed: Int,
                preferences: Preferences?,
                characterState: CharacterState,
            ): Result<String> {
                called = true
                return Result.success("ok")
            }
        }
        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse(adventures = "5"))
        val out = outputLib(
            GameRuntimeLibrary(character = char, numberologyRequest = fake),
            """cli_execute("numberology? 17");""",
        )
        assertFalse(called)
        assertTrue(out.contains("is currently available."))
    }
}
