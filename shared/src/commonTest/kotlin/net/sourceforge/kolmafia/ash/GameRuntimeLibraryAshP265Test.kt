package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.chat.ChatProbe
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.faxbot.FaxBot
import net.sourceforge.kolmafia.faxbot.FaxBotDatabase
import net.sourceforge.kolmafia.faxbot.FaxBotMonster

class GameRuntimeLibraryAshP265Test {

    @AfterTest
    fun tearDown() {
        FaxBotDatabase.instance.clearForTest()
    }

    private class StubChatProbe(
        private val onlineNames: Set<String>,
        client: HttpClient = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
    ) : ChatProbe(client) {
        override suspend fun isPlayerOnline(player: String): Boolean =
            onlineNames.any { it.equals(player, ignoreCase = true) }
    }

    @Test
    fun can_faxbot_requiresOnlineBotWhenProbeWired() = runTest {
        val db = GameDatabase()
        db.load()
        val faxDb = FaxBotDatabase.instance
        val bot = FaxBot("onlyfax", 1)
        bot.addMonsters(
            listOf(
                FaxBotMonster("rockfish", "rockfish", "rockfish", "fish", 848),
            ),
        )
        faxDb.registerBotForTest(bot)

        val offlineLib = GameRuntimeLibrary(
            gameDatabase = db,
            faxBotDatabase = faxDb,
            chatProbe = StubChatProbe(emptySet()),
        )
        assertEquals(
            "false",
            outputLib(offlineLib, """print(can_faxbot(to_monster("rockfish")));""").trim(),
        )

        val onlineLib = GameRuntimeLibrary(
            gameDatabase = db,
            faxBotDatabase = faxDb,
            chatProbe = StubChatProbe(setOf("onlyfax")),
        )
        assertEquals(
            "true",
            outputLib(onlineLib, """print(can_faxbot(to_monster("rockfish")));""").trim(),
        )
        assertEquals(
            "true",
            outputLib(onlineLib, """print(can_faxbot(to_monster("rockfish"), "OnlyFax"));""").trim(),
        )
    }
}
