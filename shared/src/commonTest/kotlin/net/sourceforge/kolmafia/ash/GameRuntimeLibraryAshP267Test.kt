package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.chat.ChatProbe
import net.sourceforge.kolmafia.chat.ChatSender
import net.sourceforge.kolmafia.data.GameDatabase

class GameRuntimeLibraryAshP267Test {

    private class StubChatProbe(
        private val counts: Map<Int, Int> = emptyMap(),
        private val commands: MutableList<String> = mutableListOf(),
        client: HttpClient = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
    ) : ChatProbe(client) {
        override suspend fun slashCount(itemId: Int): Int = counts[itemId] ?: 0

        override suspend fun sendInternalCommand(graf: String): Result<String> {
            commands += graf
            return Result.success("ok")
        }
    }

    private class RecordingChatSender(
        client: HttpClient = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
    ) : ChatSender(client) {
        val channels = mutableListOf<Pair<String, String>>()
        val privateMessages = mutableListOf<Pair<String, String>>()

        override suspend fun send(channel: String, message: String): Result<Unit> {
            channels += channel to message
            return Result.success(Unit)
        }

        override suspend fun sendPrivate(recipient: String, message: String): Result<Unit> {
            privateMessages += recipient to message
            return Result.success(Unit)
        }
    }

    @Test
    fun slash_count_returnsProbeCount() = runBlocking {
        val db = GameDatabase()
        db.load()
        val itemId = db.item("seal tooth")!!.id
        val lib = GameRuntimeLibrary(
            gameDatabase = db,
            chatProbe = StubChatProbe(mapOf(itemId to 17)),
        )
        assertEquals("17", outputLib(lib, """print(slash_count(to_item("seal tooth")));""").trim())
    }

    @Test
    fun slash_count_withoutProbe_returnsZero() {
        val lib = GameRuntimeLibrary()
        assertEquals("0", outputLib(lib, """print(slash_count(to_item("seal tooth")));""").trim())
    }

    @Test
    fun chat_clan_oneArg_sendsToClanChannel() {
        val sender = RecordingChatSender()
        val lib = GameRuntimeLibrary(chatSender = sender)
        outputLib(lib, """chat_clan("hello clan");""")
        assertEquals(listOf("clan" to "hello clan"), sender.channels)
    }

    @Test
    fun chat_clan_twoArgs_sendsToNamedChannel() {
        val sender = RecordingChatSender()
        val lib = GameRuntimeLibrary(chatSender = sender)
        outputLib(lib, """chat_clan("hello hobos", "hobopolis");""")
        assertEquals(listOf("hobopolis" to "hello hobos"), sender.channels)
    }

    @Test
    fun chat_private_sendsPrivateMessage() {
        val sender = RecordingChatSender()
        val lib = GameRuntimeLibrary(chatSender = sender)
        outputLib(lib, """chat_private("player", "secret");""")
        assertEquals(listOf("player" to "secret"), sender.privateMessages)
    }

    @Test
    fun chat_private_skipsSlashPrefixedMessage() {
        val sender = RecordingChatSender()
        val lib = GameRuntimeLibrary(chatSender = sender)
        outputLib(lib, """chat_private("player", "/whois player");""")
        assertTrue(sender.privateMessages.isEmpty())
    }

    @Test
    fun chat_private_skipsEmptyMessage() {
        val sender = RecordingChatSender()
        val lib = GameRuntimeLibrary(chatSender = sender)
        outputLib(lib, """chat_private("player", "");""")
        assertTrue(sender.privateMessages.isEmpty())
    }

    @Test
    fun chat_macro_postsMacroGraf() {
        val commands = mutableListOf<String>()
        val probe = StubChatProbe(commands = commands)
        val lib = GameRuntimeLibrary(chatProbe = probe)
        outputLib(lib, """chat_macro("/count seal tooth");""")
        assertEquals(listOf("/count seal tooth"), commands)
    }

    @Test
    fun chat_macro_withoutProbe_isNoOp() {
        val lib = GameRuntimeLibrary()
        outputLib(lib, """chat_macro("/count seal tooth");""")
    }
}
