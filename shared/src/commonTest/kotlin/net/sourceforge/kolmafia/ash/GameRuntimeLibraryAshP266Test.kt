package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.chat.ChatProbe

class GameRuntimeLibraryAshP266Test {

    private class StubChatProbe(
        private val onlineNames: Set<String>,
        client: HttpClient = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
    ) : ChatProbe(client) {
        override suspend fun isPlayerOnline(player: String): Boolean =
            onlineNames.any { it.equals(player, ignoreCase = true) }
    }

    @Test
    fun is_online_returnsTrueWhenProbeFindsPlayer() {
        val lib = GameRuntimeLibrary(chatProbe = StubChatProbe(setOf("onlyfax")))
        assertEquals("true", outputLib(lib, """print(is_online("onlyfax"));""").trim())
    }

    @Test
    fun is_online_returnsFalseWhenProbeFindsPlayerOffline() {
        val lib = GameRuntimeLibrary(chatProbe = StubChatProbe(emptySet()))
        assertEquals("false", outputLib(lib, """print(is_online("onlyfax"));""").trim())
    }

    @Test
    fun is_online_withoutProbe_returnsFalse() {
        val lib = GameRuntimeLibrary()
        assertEquals("false", outputLib(lib, """print(is_online("onlyfax"));""").trim())
    }
}
