package net.sourceforge.kolmafia.faxbot

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.chat.ChatProbe
import net.sourceforge.kolmafia.chat.ChatManager
import net.sourceforge.kolmafia.chat.ChatPoller
import net.sourceforge.kolmafia.chat.ChatSender
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.request.ClanLoungeRequest
import net.sourceforge.kolmafia.session.BreakfastManager

class FaxBotManagerTest {

    @AfterTest
    fun tearDown() {
        FaxBotDatabase.instance.clearForTest()
    }

    private class TestInventoryManager(
        initial: InventoryState,
    ) : InventoryManager(HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) }), GameEventBus()) {
        private val flow = MutableStateFlow(initial)
        override val state = flow.asStateFlow()
    }

    private fun vipInventoryManager(): InventoryManager = TestInventoryManager(
        InventoryState(
            items = mapOf(
                BreakfastManager.VIP_LOUNGE_KEY_ID to InventoryItem(
                    BreakfastManager.VIP_LOUNGE_KEY_ID,
                    "VIP key",
                    1,
                    ItemType.USABLE,
                ),
            ),
        ),
    )

    @Test
    fun requestFax_withoutVipKey_failsPreflight() = runTest {
        MonsterDatabase.load()
        val db = FaxBotDatabase.instance
        val bot = FaxBot("onlyfax", 1)
        bot.addMonsters(listOf(FaxBotMonster("rockfish", "rockfish", "rockfish", "fish", 848)))
        db.registerBotForTest(bot)

        val mgr = FaxBotManager(
            chatSender = ChatSender(HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) })),
            chatPoller = ChatPoller(HttpClient(MockEngine { respond("""{"msgs":[],"last":"0","delay":200}""") })),
            chatManager = ChatManager(),
            clanLoungeRequest = ClanLoungeRequest(HttpClient(MockEngine { respond("ok") })),
            database = db,
            gameDatabase = null,
            preferences = null,
        )
        val monster = MonsterDatabase.getByName("rockfish")!!
        val result = mgr.requestFax(monster)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("VIP key") == true)
    }

    @Test
    fun requestFax_successPollsBotAndReceivesFax() = runTest {
        MonsterDatabase.load()
        val db = FaxBotDatabase.instance
        val bot = FaxBot("onlyfax", 1)
        bot.addMonsters(listOf(FaxBotMonster("rockfish", "rockfish", "rockfish", "fish", 848)))
        db.registerBotForTest(bot)

        var receiveCalled = false
        val lounge = object : ClanLoungeRequest(HttpClient(MockEngine { respond("ok") })) {
            override suspend fun receiveFax(): Result<String> {
                receiveCalled = true
                return Result.success("Your fax is ready")
            }
        }

        val chatJson = """{"msgs":[
            {"type":"private","who":{"id":"1","name":"onlyfax"},
             "for":{"name":"player"},"msg":"Your fax is ready","format":"0","time":"1000"}
        ],"last":"1000","delay":200}"""

        val poller = ChatPoller(
            HttpClient(
                MockEngine { _ ->
                    respond(
                        chatJson,
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                },
            ),
        )

        val mgr = FaxBotManager(
            chatSender = ChatSender(HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) })),
            chatPoller = poller,
            chatManager = ChatManager(),
            clanLoungeRequest = lounge,
            database = db,
            gameDatabase = null,
            preferences = null,
            inventoryManager = vipInventoryManager(),
        )

        val monster = MonsterDatabase.getByName("rockfish")!!
        val result = mgr.requestFax(monster)

        assertTrue(result.isSuccess)
        assertTrue(receiveCalled)
    }

    @Test
    fun requestFax_skipsOfflineBot() = runTest {
        MonsterDatabase.load()
        val db = FaxBotDatabase.instance
        val easyfax = FaxBot("easyfax", 1)
        easyfax.addMonsters(listOf(FaxBotMonster("rockfish", "rockfish", "rockfish", "fish", 848)))
        val onlyfax = FaxBot("onlyfax", 2)
        onlyfax.addMonsters(listOf(FaxBotMonster("rockfish", "rockfish", "rockfish", "fish", 848)))
        db.registerBotForTest(easyfax)
        db.registerBotForTest(onlyfax)

        var pmRecipient = ""
        val sender = object : ChatSender(HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) })) {
            override suspend fun sendPrivate(recipient: String, message: String): Result<Unit> {
                pmRecipient = recipient
                return Result.success(Unit)
            }
        }

        var receiveCalled = false
        val lounge = object : ClanLoungeRequest(HttpClient(MockEngine { respond("ok") })) {
            override suspend fun receiveFax(): Result<String> {
                receiveCalled = true
                return Result.success("Your fax is ready")
            }
        }

        val chatJson = """{"msgs":[
            {"type":"private","who":{"id":"2","name":"onlyfax"},
             "for":{"name":"player"},"msg":"Your fax is ready","format":"0","time":"1000"}
        ],"last":"1000","delay":200}"""

        val poller = ChatPoller(
            HttpClient(
                MockEngine { _ ->
                    respond(chatJson, HttpStatusCode.OK)
                },
            ),
        )

        val probe = object : ChatProbe(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })) {
            override suspend fun isPlayerOnline(player: String): Boolean =
                player.equals("onlyfax", ignoreCase = true)
        }

        val mgr = FaxBotManager(
            chatSender = sender,
            chatPoller = poller,
            chatManager = ChatManager(),
            clanLoungeRequest = lounge,
            database = db,
            gameDatabase = null,
            preferences = null,
            inventoryManager = vipInventoryManager(),
            chatProbe = probe,
        )

        val monster = MonsterDatabase.getByName("rockfish")!!
        val result = mgr.requestFax(monster)

        assertTrue(result.isSuccess)
        assertEquals("onlyfax", pmRecipient)
        assertTrue(receiveCalled)
    }

    @Test
    fun chatManager_captureFaxBotMessage() {
        val chatManager = ChatManager()
        chatManager.activeFaxBot = "onlyfax"
        chatManager.captureFaxBotMessage(
            net.sourceforge.kolmafia.chat.ChatMessage(
                sender = "onlyfax",
                senderId = "1",
                recipient = "player",
                channel = null,
                content = "Your fax is ready",
                isAction = false,
                epochSeconds = 1L,
            ),
        )
        assertEquals("Your fax is ready", chatManager.consumeLastFaxBotMessage())
    }
}
