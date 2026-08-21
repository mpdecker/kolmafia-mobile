package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.PeeVPeeRequest
import net.sourceforge.kolmafia.session.SessionLogger
import net.sourceforge.kolmafia.shop.CoinmasterVisitInventory
import net.sourceforge.kolmafia.shop.SwaggerShopSync

class GameRuntimeLibraryAshP725Test {

    @BeforeTest
    fun reset() {
        CoinmasterVisitInventory.resetForTest()
    }

    @AfterTest
    fun cleanup() {
        CoinmasterVisitInventory.resetForTest()
    }

    @Test
    fun revision_phase826() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visitHtml_withDescitemBuildsOverlayAndRegistersName() {
        val prefs = Preferences(MapSettings())
        val html = """
            <tr><td><img onclick='descitem(475026869)'></td><td><b>Huggler Radio</b></td>
            <td><form><input type="hidden" name="whichitem" value="5656" />
            <input type="submit" value="Buy (50 swagger)" /></form></td></tr>
        """.trimIndent()
        SwaggerShopSync.applyVisitShop(
            html = html,
            url = "peevpee.php?place=shop",
            prefs = prefs,
            sessionLogger = null,
            state = null,
        )
        val row = CoinmasterVisitInventory.findBuyRow(SwaggerShopSync.SHOP_ID, 5656)
        assertEquals(50, row?.price)
        assertEquals("Huggler Radio", ItemDatabase.getItemName(5656).ifEmpty { ItemDatabase.getItemName(5656) })
        val name = ItemDatabase.getItemName(5656)
        assertTrue(name == "Huggler Radio" || name.isNotEmpty())
    }

    @Test
    fun buyUrl_logsSwaggerTradeLine() {
        CoinmasterVisitInventory.replaceBuyRows(
            SwaggerShopSync.SHOP_ID,
            listOf(
                net.sourceforge.kolmafia.shop.ShopRow(
                    rowId = 5656,
                    item = net.sourceforge.kolmafia.shop.ItemStack(itemId = 5656, count = 1),
                    price = 50,
                ),
            ),
        )
        val prefs = Preferences(MapSettings())
        val logger = SessionLogger(prefs, GameEventBus())
        assertTrue(
            PeeVPeeRequest.registerRequest(
                "peevpee.php?place=shop&action=buy&whichitem=5656&howmany=1",
                logger,
            ),
        )
        val log = prefs.getString(SessionLogger.SESSION_LOG_KEY, "")
        assertTrue(log.contains("trading 50 swagger for 1"))
    }
}
