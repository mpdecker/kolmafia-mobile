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
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.RaffleRequest

class GameRuntimeLibraryAshP530Test {

    private class RecordingRaffle : RaffleRequest(HttpClient(MockEngine { respond("Here you go", HttpStatusCode.OK) })) {
        val calls = mutableListOf<Pair<Int, RaffleRequest.RaffleSource>>()
        override suspend fun buy(quantity: Int, source: RaffleRequest.RaffleSource): Result<String> {
            calls += quantity to source
            return Result.success("Here you go")
        }
    }

    @Test
    fun revision_phase530() {
        assertEquals("phase635", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun raffle_defaultInventory_whenUnlocked() {
        val raffle = RecordingRaffle()
        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse(ascensions = "3"))
        val prefs = Preferences(MapSettings())
        prefs.setInt("lastDesertUnlock", 3)
        val out = outputLib(
            GameRuntimeLibrary(character = char, preferences = prefs, raffleRequest = raffle),
            """cli_execute("raffle 5");""",
        )
        assertEquals(listOf(5 to RaffleRequest.RaffleSource.INVENTORY), raffle.calls)
        assertTrue(out.contains("Visiting the Raffle House..."))
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun raffle_storageSource() {
        val raffle = RecordingRaffle()
        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse(ascensions = "1"))
        val prefs = Preferences(MapSettings())
        prefs.setInt("lastDesertUnlock", 1)
        outputLib(
            GameRuntimeLibrary(character = char, preferences = prefs, raffleRequest = raffle),
            """cli_execute("raffle 2 storage");""",
        )
        assertEquals(listOf(2 to RaffleRequest.RaffleSource.STORAGE), raffle.calls)
    }

    @Test
    fun raffle_blockedWithoutBeach() {
        val raffle = RecordingRaffle()
        val out = outputLib(
            GameRuntimeLibrary(character = KoLCharacter(), raffleRequest = raffle),
            """cli_execute("raffle 1");""",
        )
        assertTrue(out.contains("You can't make it to the raffle house"))
        assertTrue(raffle.calls.isEmpty())
    }
}
