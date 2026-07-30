package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.chat.ChatProbe
import net.sourceforge.kolmafia.chat.PlayerIdRegistry

class GameRuntimeLibraryAshP268Test {

    @AfterTest
    fun tearDown() {
        PlayerIdRegistry.clearForTest()
    }

    private class StubChatProbe(
        private val whoClanMap: Map<String, Boolean> = emptyMap(),
        private val lookupIds: Map<String, String> = emptyMap(),
        private val lookupNames: Map<String, String> = emptyMap(),
        client: HttpClient = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
    ) : ChatProbe(client) {
        override suspend fun whoClan(): Map<String, Boolean> = whoClanMap

        override suspend fun lookupPlayerId(name: String): String =
            lookupIds.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
                ?: PlayerIdRegistry.getPlayerId(name, retrieveId = false)

        override suspend fun lookupPlayerName(id: String): String =
            lookupNames[id] ?: PlayerIdRegistry.getPlayerName(id, retrieveName = false)
    }

    @Test
    fun who_clan_returnsContactMap() {
        val lib = GameRuntimeLibrary(
            chatProbe = StubChatProbe(
                whoClanMap = mapOf("onlyfax" to true, "easyfax" to false),
            ),
        )
        val out = outputLib(
            lib,
            """
            boolean[string] contacts = who_clan();
            print(contains_key(contacts, "onlyfax"));
            print(contacts["onlyfax"]);
            print(contacts["easyfax"]);
            """.trimIndent(),
        ).lines()
        assertEquals("true", out[0].trim())
        assertEquals("true", out[1].trim())
        assertEquals("false", out[2].trim())
    }

    @Test
    fun who_clan_withoutProbe_returnsEmptyMap() {
        val lib = GameRuntimeLibrary()
        assertEquals("0", outputLib(lib, """print(count(who_clan()));""").trim())
    }

    @Test
    fun get_player_id_returnsLookupResult() {
        val lib = GameRuntimeLibrary(
            chatProbe = StubChatProbe(lookupIds = mapOf("onlyfax" to "12345")),
        )
        assertEquals("12345", outputLib(lib, """print(get_player_id("onlyfax"));""").trim())
    }

    @Test
    fun get_player_name_returnsLookupResult() {
        val lib = GameRuntimeLibrary(
            chatProbe = StubChatProbe(lookupNames = mapOf("12345" to "onlyfax")),
        )
        assertEquals("onlyfax", outputLib(lib, """print(get_player_name(12345));""").trim())
    }

    @Test
    fun get_player_id_withoutProbe_fallsBackToRegistry() {
        PlayerIdRegistry.register("cached", "777")
        val lib = GameRuntimeLibrary()
        assertEquals("777", outputLib(lib, """print(get_player_id("cached"));""").trim())
    }

    @Test
    fun get_player_id_withoutProbe_unknownReturnsName() {
        val lib = GameRuntimeLibrary()
        assertEquals("unknown", outputLib(lib, """print(get_player_id("unknown"));""").trim())
    }
}
