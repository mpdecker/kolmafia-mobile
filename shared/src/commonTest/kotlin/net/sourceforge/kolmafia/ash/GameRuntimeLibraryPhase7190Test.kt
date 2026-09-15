package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.AltarOfBonesRequest
import net.sourceforge.kolmafia.request.HermitRequest
import net.sourceforge.kolmafia.request.MiscShopTokenResponseParse
import net.sourceforge.kolmafia.request.SkeletonOfCrimboPastRequest
import net.sourceforge.kolmafia.request.TownGiftShopRequestHub
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Focused HTTP Residual LV Track B coverage (phases 7171–7190).
 */
class GameRuntimeLibraryPhase7190Test {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    private fun inventory(): InventoryManager =
        InventoryManager(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }), GameEventBus())

    @Test
    fun hermit_parseResponse_setsTradableAndCloverPrefs() {
        val p = prefs()
        HermitRequest.parseResponse(
            "hermit.php",
            "You have 42 tradable items. 3 left in stock for today",
            p,
        )
        assertEquals(42, p.getInt("hermitTradableItems"))
        assertEquals(3, p.getInt("hermitCloverCount"))
    }

    @Test
    fun hermit_parseResponse_incrementsCloversPurchasedOnTrade() {
        val p = prefs()
        HermitRequest.parseResponse(
            "hermit.php?action=trade&whichitem=10881&quantity=2",
            "You acquire an item: <b>2 eleven-leaf clovers</b>",
            p,
        )
        assertEquals(2, p.getInt("_cloversPurchased"))
    }

    @Test
    fun skeleton_parseResponse_syncsKnucklebonesPrefAndInventory() {
        val p = prefs()
        val inv = inventory()
        SkeletonOfCrimboPastRequest.parseResponse(
            "choice.php?whichchoice=1567",
            "You've got <b>7</b> knucklebones.",
            p,
            inv,
        )
        assertEquals(7, p.getInt("availableKnucklebones"))
        assertEquals(7, inv.getCount(MiscShopTokenResponseParse.KNUCKLEBONE))
    }

    @Test
    fun altar_parseResponse_syncsBoneChipsInventory() {
        val p = prefs()
        val inv = inventory()
        inv.gainItemLocally(MiscShopTokenResponseParse.BONE_CHIPS, 1)
        AltarOfBonesRequest.parseResponse(
            "bone_altar.php",
            "You have 15 bone chips",
            p,
            inv,
        )
        assertEquals(15, p.getInt("availableBoneChips"))
        assertEquals(15, inv.getCount(MiscShopTokenResponseParse.BONE_CHIPS))
    }

    @Test
    fun townGiftShop_registerRequest_logsBuyLine() {
        ItemDatabase.registerItem(999, "daisy", "999")
        NpcStoreDatabase.loadFromText(
            "Gift Shop\ttown_giftshop.php\tdaisy\t100\n",
        )
        val logger = SessionLogger(prefs(), GameEventBus())
        assertTrue(
            TownGiftShopRequestHub.registerRequest(
                "town_giftshop.php?whichitem=999&action=buy&howmany=2",
                logger,
            ),
        )
        assertTrue(
            logger.recentLines().any {
                it.contains("buy 2 daisy for 100 each from The Town Gift Shop")
            },
        )
        NpcStoreDatabase.resetForTest()
    }
}
