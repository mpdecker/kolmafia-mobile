package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.PeeVPeeSync
import net.sourceforge.kolmafia.shop.CoinmasterVisitInventory
import net.sourceforge.kolmafia.shop.ShopRow
import net.sourceforge.kolmafia.shop.ItemStack
import net.sourceforge.kolmafia.shop.SwaggerShopSync

class GameRuntimeLibraryAshP724Test {

    @BeforeTest
    fun reset() {
        CoinmasterVisitInventory.resetForTest()
    }

    @AfterTest
    fun cleanup() {
        CoinmasterVisitInventory.resetForTest()
    }

    private fun inventory(): InventoryManager {
        val client = HttpClient(MockEngine { respond("", HttpStatusCode.OK) })
        return InventoryManager(client, GameEventBus())
    }

    @Test
    fun revision_phase848() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun buySuccess_decrementsSwaggerAndGrantsItem() {
        CoinmasterVisitInventory.replaceBuyRows(
            SwaggerShopSync.SHOP_ID,
            listOf(ShopRow(rowId = 5656, item = ItemStack(itemId = 5656, count = 1), price = 50)),
        )
        val prefs = Preferences(MapSettings())
        prefs.setInt("availableSwagger", 200)
        val inv = inventory()
        PeeVPeeSync.apply(
            html = "You acquire an item",
            url = "peevpee.php?place=shop&action=buy&whichitem=5656&howmany=2",
            character = null,
            preferences = prefs,
            inventoryManager = inv,
        )
        assertEquals(100, prefs.getInt("availableSwagger", 0))
        assertEquals(2, inv.state.value.items[5656]?.quantity)
    }

    @Test
    fun buyFailure_noOps() {
        CoinmasterVisitInventory.replaceBuyRows(
            SwaggerShopSync.SHOP_ID,
            listOf(ShopRow(rowId = 5656, item = ItemStack(itemId = 5656, count = 1), price = 50)),
        )
        val prefs = Preferences(MapSettings())
        prefs.setInt("availableSwagger", 200)
        val inv = inventory()
        SwaggerShopSync.applyBuy(
            html = "You don't have enough swagger",
            url = "peevpee.php?place=shop&action=buy&whichitem=5656&howmany=1",
            prefs = prefs,
            inventoryManager = inv,
            sessionLogger = null,
        )
        assertEquals(200, prefs.getInt("availableSwagger", 0))
        assertEquals(null, inv.state.value.items[5656])
    }

    @Test
    fun buyHtmlBalance_overridesDecrement() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("availableSwagger", 200)
        val inv = inventory()
        SwaggerShopSync.applyBuy(
            html = "You have 175 swagger",
            url = "peevpee.php?place=shop&action=buy&whichitem=5656&howmany=1",
            prefs = prefs,
            inventoryManager = inv,
            sessionLogger = null,
        )
        assertEquals(175, prefs.getInt("availableSwagger", 0))
        assertEquals(1, inv.state.value.items[5656]?.quantity)
    }
}
