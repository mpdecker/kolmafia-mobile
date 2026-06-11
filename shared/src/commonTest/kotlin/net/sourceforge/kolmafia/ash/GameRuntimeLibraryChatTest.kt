package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import net.sourceforge.kolmafia.chat.ChatSender
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryChatTest {

    @Test
    fun say_usesDefaultChannelPref() {
        var channel = ""
        val sender = object : ChatSender(HttpClient(MockEngine { respond("") })) {
            override suspend fun send(ch: String, msg: String): Result<Unit> {
                channel = ch
                return Result.success(Unit)
            }
        }
        val p = prefs()
        p.setString(Preferences.CURRENT_CHAT_CHANNEL, "trade")
        val lib = GameRuntimeLibrary(chatSender = sender, preferences = p)
        assertEquals("true", outputLib(lib, """print(to_string(say("hi")));"""))
        assertEquals("trade", channel)
    }

    @Test
    fun say_withChannel_sendsToNamedChannel() {
        var channel = ""
        val sender = object : ChatSender(HttpClient(MockEngine { respond("") })) {
            override suspend fun send(ch: String, msg: String): Result<Unit> {
                channel = ch
                return Result.success(Unit)
            }
        }
        val lib = GameRuntimeLibrary(chatSender = sender)
        assertTrue(outputLib(lib, """print(to_string(say("party", "yo")));""").contains("true"))
        assertEquals("party", channel)
    }

    @Test
    fun msg_sendsPrivateMessage() {
        var recipient = ""
        val sender = object : ChatSender(HttpClient(MockEngine { respond("") })) {
            override suspend fun sendPrivate(to: String, msg: String): Result<Unit> {
                recipient = to
                return Result.success(Unit)
            }
        }
        val lib = GameRuntimeLibrary(chatSender = sender)
        assertTrue(outputLib(lib, """print(to_string(msg("Bob", "secret")));""").contains("true"))
        assertEquals("Bob", recipient)
    }
}
