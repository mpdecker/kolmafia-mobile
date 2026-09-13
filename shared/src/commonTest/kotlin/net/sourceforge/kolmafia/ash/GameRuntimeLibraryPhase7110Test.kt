package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.BURTRequest
import net.sourceforge.kolmafia.request.CRIMBCOGiftShopRequest
import net.sourceforge.kolmafia.request.MiscShopTokenResponseParse
import net.sourceforge.kolmafia.shop.CoinmasterData
import net.sourceforge.kolmafia.shop.CoinmasterResponseSync
import net.sourceforge.kolmafia.shop.ItemStack
import net.sourceforge.kolmafia.shop.ShopRow

/**
 * Focused HTTP Residual LIV Track A–C coverage (phases 7091–7150).
 * Parent wrap bumps REVISION to phase7150.
 */
class GameRuntimeLibraryPhase7110Test {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    private fun inventory(): InventoryManager =
        InventoryManager(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }), GameEventBus())

    @Test
    fun revision_isPhase7150() {
        assertEquals("phase7150", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun miscShop_parsesVolcoinoAndKa() {
        val inv = inventory()
        assertTrue(
            MiscShopTokenResponseParse.parseResponse(
                "shop.php?whichshop=infernodisco",
                "<td>12 Volcoino",
                prefs(),
                inv,
            ),
        )
        assertEquals(12, inv.getCount(MiscShopTokenResponseParse.VOLCOINO))
        assertTrue(
            MiscShopTokenResponseParse.parseResponse(
                "shop.php?whichshop=edunder_shopshop",
                "<td>4 Ka coin",
                prefs(),
                inv,
            ),
        )
        assertEquals(4, inv.getCount(MiscShopTokenResponseParse.KA_COIN))
    }

    @Test
    fun miscShop_parsesKruegerandWordCountAndShoreScrip() {
        val inv = inventory()
        assertTrue(
            MiscShopTokenResponseParse.parseResponse(
                "shop.php?whichshop=dv",
                "<td>three Freddy Kruegerand",
                prefs(),
                inv,
            ),
        )
        assertEquals(3, inv.getCount(MiscShopTokenResponseParse.KRUEGERAND))
        assertTrue(
            MiscShopTokenResponseParse.parseResponse(
                "shop.php?whichshop=shore",
                "You have 8 Shore Inc. Ship Trip Scrip",
                prefs(),
                inv,
            ),
        )
        assertEquals(8, inv.getCount(MiscShopTokenResponseParse.SHIP_TRIP_SCRIP))
    }

    @Test
    fun miscShop_beachBucksFanInAndMrStore2002Credits() {
        val inv = inventory()
        val p = prefs()
        assertTrue(
            MiscShopTokenResponseParse.parseResponse(
                "shop.php?whichshop=sbb_jimmy",
                "<td>5 Beach Bucks",
                p,
                inv,
            ),
        )
        assertEquals(5, inv.getCount(MiscShopTokenResponseParse.BEACH_BUCK))
        assertEquals(5, p.getInt("availableBeachBucks", 0))
        assertTrue(
            MiscShopTokenResponseParse.parseResponse(
                "shop.php?whichshop=mrstore2002",
                "<b>You have 3 Mr. Store 2002 Credits.</b>",
                p,
                inv,
            ),
        )
        assertEquals(3, p.getInt("availableMrStore2002Credits", 0))
    }

    @Test
    fun burtAndCrimbco_syncInventory() {
        val inv = inventory()
        BURTRequest.parseResponse(
            "inv_use.php?whichitem=5683",
            "You have 2 BURT",
            prefs(),
            inv,
        )
        assertEquals(2, inv.getCount(MiscShopTokenResponseParse.BURT))
        assertTrue(
            CRIMBCOGiftShopRequest.parseResponse(
                "crimbo10.php?place=giftshop",
                "You have <b>9</b> CRIMBCO scrip",
                prefs(),
                inv,
            ),
        )
        assertEquals(9, inv.getCount(MiscShopTokenResponseParse.CRIMBCO_SCRIP))
    }

    @Test
    fun responseSync_propertyShopRowDeductsCredits() {
        val p = prefs { putInt("availableMrStore2002Credits", 5) }
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.registerForTest(
            CoinmasterData(
                masterName = "Mr. Store 2002",
                nickname = "mrstore2002",
                token = "Mr. Store 2002 Credit",
                property = "availableMrStore2002Credits",
                shopId = "mrstore2002",
                buyItems = listOf(
                    ShopRow(
                        rowId = 1378,
                        item = ItemStack(itemId = 9991, count = 1),
                        costs = listOf(ItemStack(itemId = 0, count = 1)),
                    ),
                ),
                sellItems = emptyList(),
            ),
        )
        val inv = inventory()
        assertTrue(
            CoinmasterResponseSync.apply(
                url = "shop.php?whichshop=mrstore2002&action=buyitem&whichrow=1378&quantity=2",
                html = "You acquire",
                preferences = p,
                inventory = inv,
                character = null,
            ),
        )
        assertEquals(3, p.getInt("availableMrStore2002Credits", 0))
        assertEquals(2, inv.getCount(9991))
    }
}
