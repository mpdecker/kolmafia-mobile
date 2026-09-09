package net.sourceforge.kolmafia.shop

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences

class CoinmasterResponseSyncTest {

    private fun inventory(): InventoryManager =
        InventoryManager(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }), GameEventBus())

    @BeforeTest
    fun resetCoinmasterDatabase() {
        CoinmasterDatabase.resetForTest()
    }

    @Test
    fun apply_buyitem_deductsPropertyTokensAndAddsItem() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("testTokens", 10)

        CoinmasterDatabase.registerForTest(
            CoinmasterData(
                masterName = "Test Token Shop",
                nickname = "testshop",
                token = "Test Token",
                property = "testTokens",
                shopId = "testshop",
                buyItems = listOf(
                    ShopRow(
                        rowId = 100,
                        item = ItemStack(itemId = 999, count = 1),
                        price = 3,
                    ),
                ),
                sellItems = emptyList(),
            ),
        )

        val inv = inventory()
        val url =
            "https://www.kingdomofloathing.com/shop.php?whichshop=testshop&action=buyitem&whichrow=100&quantity=2"
        val html = "<html><body>You acquire an item.</body></html>"

        assertTrue(
            CoinmasterResponseSync.apply(
                url = url,
                html = html,
                preferences = prefs,
                inventory = inv,
                character = null,
            ),
        )

        assertEquals(4, prefs.getInt("testTokens", 0))
        assertEquals(2, inv.state.value.items[999]?.quantity)
    }

    @Test
    fun apply_visit_parsesPropertyBalance() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("testTokens", 0)

        CoinmasterDatabase.registerForTest(
            CoinmasterData(
                masterName = "Test Token Shop",
                nickname = "testshop",
                token = "Test Token",
                property = "testTokens",
                shopId = "testshop",
                buyItems = emptyList(),
                sellItems = emptyList(),
            ),
        )

        val url = "https://www.kingdomofloathing.com/shop.php?whichshop=testshop"
        val html = "<html><body>You have 12 Test Tokens left.</body></html>"

        assertTrue(
            CoinmasterResponseSync.apply(
                url = url,
                html = html,
                preferences = prefs,
                inventory = null,
                character = null,
            ),
        )

        assertEquals(12, prefs.getInt("testTokens", 0))
    }

    @Test
    fun apply_buyFailure_doesNotAdjustInventory() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("testTokens", 10)

        CoinmasterDatabase.registerForTest(
            CoinmasterData(
                masterName = "Test Token Shop",
                nickname = "testshop",
                token = "Test Token",
                property = "testTokens",
                shopId = "testshop",
                buyItems = listOf(
                    ShopRow(
                        rowId = 100,
                        item = ItemStack(itemId = 999, count = 1),
                        price = 3,
                    ),
                ),
                sellItems = emptyList(),
            ),
        )

        val inv = inventory()
        val url =
            "https://www.kingdomofloathing.com/shop.php?whichshop=testshop&action=buyitem&whichrow=100&quantity=1"
        val html = "<html><body>You don't have enough Test Tokens.</body></html>"

        CoinmasterResponseSync.apply(
            url = url,
            html = html,
            preferences = prefs,
            inventory = inv,
            character = null,
        )

        assertEquals(10, prefs.getInt("testTokens", 0))
        assertTrue(inv.state.value.items.isEmpty())
    }

    @Test
    fun apply_sellitem_creditsPropertyTokens() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("testTokens", 1)

        CoinmasterDatabase.registerForTest(
            CoinmasterData(
                masterName = "Test Token Shop",
                nickname = "testshop",
                token = "Test Token",
                property = "testTokens",
                shopId = "testshop",
                buyItems = emptyList(),
                sellItems = listOf(
                    ShopRow(
                        rowId = 200,
                        item = ItemStack(itemId = 888, count = 1),
                        price = 5,
                    ),
                ),
            ),
        )

        val inv = inventory()
        inv.gainItemLocally(888, 2)

        val url =
            "https://www.kingdomofloathing.com/shop.php?whichshop=testshop&action=sellitem&whichrow=200&quantity=1"
        val html = "<html><body>Sold.</body></html>"

        CoinmasterResponseSync.apply(
            url = url,
            html = html,
            preferences = prefs,
            inventory = inv,
            character = null,
        )

        assertEquals(6, prefs.getInt("testTokens", 0))
        assertEquals(1, inv.state.value.items[888]?.quantity)
    }
}
