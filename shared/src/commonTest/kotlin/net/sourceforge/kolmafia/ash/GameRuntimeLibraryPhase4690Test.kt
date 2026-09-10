package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.mall.MallPurchaseRequest
import net.sourceforge.kolmafia.mall.MallSearchRequest
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.AltarOfLiteracyRequest
import net.sourceforge.kolmafia.request.ArtistRequest
import net.sourceforge.kolmafia.request.BurningLeavesRequest
import net.sourceforge.kolmafia.request.DreadsylvaniaRequest
import net.sourceforge.kolmafia.request.FantasyRealmRequest
import net.sourceforge.kolmafia.request.GnomePartRequest
import net.sourceforge.kolmafia.request.MummeryRequest
import net.sourceforge.kolmafia.request.PantogramRequest
import net.sourceforge.kolmafia.request.SausageOMaticRequest
import net.sourceforge.kolmafia.request.UseSkillRequest
import net.sourceforge.kolmafia.request.WildfireCampRequest
import net.sourceforge.kolmafia.session.StoreManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameRuntimeLibraryPhase4690Test {

    @Test
    fun revision_phase4870() {
        assertEquals("phase6370", GameRuntimeLibrary.REVISION)
        assertEquals("phase6370", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }

    @Test
    fun buy_twoArg_returnsBoolean() {
        val db = object : GameDatabase() {
            override fun item(id: Int) = if (id == 42) ItemData(
                id = 42, name = "test widget", descId = "", image = "",
                primaryUse = ItemPrimaryUse.NONE, secondaryUses = emptySet(),
                access = setOf('t', 'd'), autosellPrice = 10, plural = null,
            ) else null
            override fun item(name: String) = if (name == "test widget") item(42) else null
        }
        val client = HttpClient(MockEngine { respond("") })
        val mall = object : MallManager(MallSearchRequest(client), MallPurchaseRequest(client), null) {
            override suspend fun buy(itemId: Int, count: Int, maxPrice: Int) = count
            override suspend fun cheapestPrice(itemName: String) = 100L
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, mallManager = mall)
        assertEquals("true", outputLib(lib, """print(to_string(buy(2, to_item("test widget"))));"""))
        assertEquals("2", outputLib(lib, """print(to_string(buy(2, to_item("test widget"), 999)));"""))
    }

    @Test
    fun retrieve_price_withCount() {
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        // Unknown item → -1
        assertEquals("-1", outputLib(lib, """print(to_string(retrieve_price(to_item("no such item xyz"), 1)));"""))
    }

    @Test
    fun take_shop_itemOnly_usesCachedQuantity() {
        StoreManager.clearCache()
        StoreManager.addItem(99, 5, 100, 0)
        val lib = GameRuntimeLibrary(
            gameDatabase = object : GameDatabase() {
                override fun item(id: Int) = if (id == 99) ItemData(
                    id = 99, name = "shop item", descId = "", image = "",
                    primaryUse = ItemPrimaryUse.NONE, secondaryUses = emptySet(),
                    access = setOf('t', 'd'), autosellPrice = 1, plural = null,
                ) else null
                override fun item(name: String) = if (name == "shop item") item(99) else null
            },
            manageStoreRequest = object : net.sourceforge.kolmafia.request.ManageStoreRequest(
                HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            ) {
                override suspend fun removeItem(itemId: Int, quantity: Int) =
                    Result.success("removed $quantity")
            },
        )
        assertEquals("true", outputLib(lib, """print(to_string(take_shop(to_item("shop item"))));"""))
        StoreManager.clearCache()
    }

    @Test
    fun altarOfLiteracy_setsPref() {
        val p = Preferences(MapSettings())
        AltarOfLiteracyRequest.parseResponse(
            "town_altar.php",
            "You have been granted access to the Kingdom of Loathing chat.",
            p,
        )
        assertTrue(p.getBoolean(AltarOfLiteracyRequest.CHAT_LITERATE_PREF))
    }

    @Test
    fun wildfireCamp_rainbarrel() {
        val p = Preferences(MapSettings())
        WildfireCampRequest.parseResponse(
            "place.php?whichplace=wildfire_camp&action=wildfire_rainbarrel",
            "You collect 150 water from the barrel.",
            p,
        )
        assertTrue(p.getBoolean("_wildfireBarrelHarvested"))
        assertTrue(p.getBoolean("wildfireBarrelCaulked"))
    }

    @Test
    fun requestHubs_registerUrls() {
        assertTrue(UseSkillRequest.registerRequest("skills.php?action=Skillz&whichskill=1"))
        assertTrue(WildfireCampRequest.registerRequest("place.php?whichplace=wildfire_camp"))
        assertTrue(ArtistRequest.registerRequest("place.php?whichplace=town_wrong&action=townwrong_artist_quest"))
        assertTrue(AltarOfLiteracyRequest.registerRequest("town_altar.php"))
        assertTrue(DreadsylvaniaRequest.registerRequest("clan_dreadsylvania.php?action=forceloc&loc=1"))
        assertTrue(DreadsylvaniaRequest.registerRequest("clan_dreadsylvania.php?action=feedbooze&whichbooze=1&boozequantity=1"))
        assertEquals(1, DreadsylvaniaRequest.getAdventuresUsed("clan_dreadsylvania.php?loc=2"))
        assertTrue(PantogramRequest.registerRequest("choice.php?whichchoice=1270&option=1"))
        assertTrue(MummeryRequest.registerRequest("choice.php?whichchoice=1271&option=1"))
        assertTrue(BurningLeavesRequest.registerRequest("choice.php?whichchoice=1510"))
        assertTrue(SausageOMaticRequest.registerRequest("inv_use.php?whichitem=sausage"))
        assertTrue(FantasyRealmRequest.registerRequest("shop.php?whichshop=fantasyrealm"))
        assertTrue(GnomePartRequest.registerRequest("gnomes.php?action=tinksomething"))
    }

    @Test
    fun put_closet_meat_nullRequest_returnsFalse() {
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()), closetRequest = null)
        assertEquals("false", outputLib(lib, """print(to_string(put_closet(100)));"""))
    }
}
