package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.CoinmasterData
import net.sourceforge.kolmafia.shop.CoinmasterManager
import net.sourceforge.kolmafia.shop.CoinmasterRequest

class GameRuntimeLibraryAshP537Test {

    data class TradeCall(val nick: String, val itemId: Int, val qty: Int, val buy: Boolean)

    private class RecordingCoinmaster : CoinmasterManager(
        CoinmasterRequest(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })),
        null,
        null,
        HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
    ) {
        val calls = mutableListOf<TradeCall>()

        override fun resolveMaster(value: String): CoinmasterData? =
            CoinmasterDatabase.findByNickname(value)

        override suspend fun buy(master: CoinmasterData, itemId: Int, quantity: Int): Int {
            calls += TradeCall(master.nickname, itemId, quantity, buy = true)
            return quantity
        }

        override suspend fun sell(master: CoinmasterData, itemId: Int, quantity: Int): Int {
            calls += TradeCall(master.nickname, itemId, quantity, buy = false)
            return quantity
        }
    }

    @BeforeTest
    fun setUp() {
        CoinmasterDatabase.resetForTest()
        ItemDatabase.registerForTest(
            ItemData(146, "dinghy plans", "d146", "d.gif", ItemPrimaryUse.NONE, emptySet(), setOf('t'), 0, null),
        )
        ItemDatabase.registerForTest(
            ItemData(147, "tropical bikini", "d147", "t.gif", ItemPrimaryUse.NONE, emptySet(), setOf('t'), 0, null),
        )
        CoinmasterDatabase.registerForTest(
            CoinmasterData(
                masterName = "The Shore",
                nickname = "shore",
                token = "ticket",
                shopId = "shore",
                buyItems = emptyList(),
                sellItems = emptyList(),
            ),
        )
    }

    @AfterTest
    fun tearDown() {
        CoinmasterDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun revision_phase538() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun coinmaster_buy_multiItem_records() {
        val manager = RecordingCoinmaster()
        outputLib(
            GameRuntimeLibrary(
                gameDatabase = net.sourceforge.kolmafia.data.GameDatabase(),
                coinmasterManager = manager,
            ),
            """cli_execute("coinmaster buy shore dinghy plans, 2 tropical bikini");""",
        )
        assertEquals(
            listOf(
                TradeCall("shore", 146, 1, buy = true),
                TradeCall("shore", 147, 2, buy = true),
            ),
            manager.calls,
        )
    }

    @Test
    fun help_listsCoinmaster() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("help coinmaster");""")
        assertTrue(out.lines().any { it.trim() == "coinmaster" })
    }
}
