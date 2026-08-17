package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.ShrineRequest

class GameRuntimeLibraryAshP526Test {

    @Test
    fun revision_phase526() {
        assertEquals("phase605", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun donate_boris_recordsHeroAndAmount() {
        val calls = mutableListOf<Pair<Int, Int>>()
        val fake = object : ShrineRequest(HttpClient(MockEngine { respond("You gain", HttpStatusCode.OK) })) {
            override suspend fun donate(
                heroId: Int,
                amount: Int,
                preferences: Preferences?,
                hasStatueKey: Boolean,
            ): Result<String> {
                calls += heroId to amount
                return Result.success("You gain")
            }
        }
        val out = outputLib(
            GameRuntimeLibrary(shrineRequest = fake),
            """cli_execute("donate boris 1000");""",
        )
        assertEquals(listOf(ShrineRequest.BORIS to 1000), calls)
        assertTrue(out.contains("Donating 1000 to the shrine..."))
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun donate_badStatue_errors() {
        var called = false
        val fake = object : ShrineRequest(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })) {
            override suspend fun donate(
                heroId: Int,
                amount: Int,
                preferences: Preferences?,
                hasStatueKey: Boolean,
            ): Result<String> {
                called = true
                return Result.success("ok")
            }
        }
        val out = outputLib(
            GameRuntimeLibrary(shrineRequest = fake),
            """cli_execute("donate statue 100");""",
        )
        assertFalse(called)
        assertTrue(out.contains("is not a statue."))
    }
}
