package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.ClanLoungeRequest

class GameRuntimeLibraryAshP518Test {

    private class RecordingHotTub : ClanLoungeRequest(HttpClient(MockEngine { respond("") })) {
        var calls = 0
        override suspend fun useHotTub(preferences: Preferences?): Result<String> {
            calls++
            return Result.success("ok")
        }
    }

    @Test
    fun revision_phase518() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun hottub_invokesUseHotTub() {
        val lounge = RecordingHotTub()
        val out = outputLib(
            GameRuntimeLibrary(clanLoungeRequest = lounge),
            """cli_execute("hottub");""",
        )
        assertEquals(1, lounge.calls)
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun soak_invokesUseHotTub() {
        val lounge = RecordingHotTub()
        val out = outputLib(
            GameRuntimeLibrary(clanLoungeRequest = lounge),
            """cli_execute("soak");""",
        )
        assertEquals(1, lounge.calls)
        assertFalse(out.contains("[cli]"))
    }
}
