package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.clan.ClanManager
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.inventory.CollectionCacheSync
import net.sourceforge.kolmafia.mall.MallListing
import net.sourceforge.kolmafia.mall.MallListingSource
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CafeDailySpecialSync
import net.sourceforge.kolmafia.request.CafeRequest
import net.sourceforge.kolmafia.request.StoragePullRules
import net.sourceforge.kolmafia.request.StorageRequest
import net.sourceforge.kolmafia.session.StoreManager

class GameRuntimeLibraryPhase6571Test {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    @AfterTest
    fun tearDown() {
        CollectionCacheSync.resetRetrievedFlags()
        ClanManager.resetForTest()
        StoreManager.clearCache()
    }

    @Test
    fun storagePullRules_classifiesNopullsBucket() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 9001,
                name = "nopull gadget",
                descId = "d9001",
                image = "x.gif",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = emptySet(),
                autosellPrice = 0,
                plural = null,
            ),
        )
        ModifierDatabase.injectForTest("Item", "nopull gadget", "No Pull")
        val classified = StoragePullRules.classifyContents(
            raw = mapOf(9001 to 3, 42 to 1),
            characterState = CharacterState(isHardcore = true, roninLeft = 5),
        )
        assertEquals(3, classified.nopulls[9001])
        assertEquals(1, classified.storage[42])
        assertTrue(classified.freepulls.isEmpty())
    }

    @Test
    fun get_no_pulls_readsCachedNopullsOnly() {
        val p = prefs()
        CollectionCacheSync.saveStorage(
            p,
            storage = mapOf(1 to 9),
            freepulls = emptyMap(),
            nopulls = mapOf(2 to 4),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = 2,
                name = "locked relic",
                descId = "d2",
                image = "x.gif",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = emptySet(),
                autosellPrice = 0,
                plural = null,
            ),
        )
        val db = object : GameDatabase() {
            override fun item(id: Int): ItemData? = ItemDatabase.getById(id)
            override fun item(name: String): ItemData? = ItemDatabase.getByName(name)
        }
        val lib = GameRuntimeLibrary(preferences = p, gameDatabase = db)
        assertEquals("1", outputLib(lib, "print(count(get_no_pulls()));").trim())
        assertEquals("4", outputLib(lib, """print(get_no_pulls()[to_item("locked relic")]);""").trim())
    }

    @Test
    fun closet_amount_lazyRefreshesWhenNeverRetrieved() {
        CollectionCacheSync.resetRetrievedFlags()
        val fakeCloset = object : net.sourceforge.kolmafia.request.ClosetRequest(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
        ) {
            override suspend fun fetchContents(): Map<Int, Int> = mapOf(42 to 7)
        }
        val shiny = ItemData(
            42, "shiny item", "desc", "item.gif",
            ItemPrimaryUse.NONE, emptySet(), setOf('t', 'd'), 0, null,
        )
        val db = object : GameDatabase() {
            override fun item(id: Int): ItemData? = if (id == 42) shiny else null
            override fun item(name: String): ItemData? =
                if (name.equals("shiny item", ignoreCase = true)) shiny else null
        }
        val p = prefs()
        val lib = GameRuntimeLibrary(closetRequest = fakeCloset, gameDatabase = db, preferences = p)
        assertEquals("7", outputLib(lib, """print(to_string(closet_amount(to_item("shiny item"))));""").trim())
        assertTrue(CollectionCacheSync.closetRetrieved)
    }

    @Test
    fun cafeDailySpecialSync_parsesTodaysSpecial() {
        val p = prefs()
        val html = """
            Today's Special:
            <input type=hidden name=whichitem value=806>
            <a href='javascript:void()' onclick='descitem("12345")'></a>
            <td>special lager (150 Meat)</td>
        """.trimIndent()
        val name = CafeDailySpecialSync.parseResponse("cafe.php?cafeid=2", html, p)
        assertEquals("special lager", name)
        assertEquals("special lager", p.getString("_dailySpecial", ""))
        assertEquals(150, p.getInt("_dailySpecialPrice", 0))
    }

    @Test
    fun daily_special_liveCafeVisitSync() {
        val p = prefs()
        val html = """
            Today's Special:
            <input type=hidden name=whichitem value=900>
            <a href='javascript:void()' onclick='descitem("99")'></a>
            <td>brunch special (200 Meat)</td>
        """.trimIndent()
        val cafe = object : CafeRequest(HttpClient(MockEngine { respond(html, HttpStatusCode.OK) })) {
            override suspend fun visitMenu(cafeId: String, preferences: Preferences?): Result<String> {
                CafeDailySpecialSync.parseResponse("cafe.php?cafeid=$cafeId", html, preferences)
                return Result.success(html)
            }
        }
        // Force Chez availability via null state (CafeAccessibility returns true)
        val lib = GameRuntimeLibrary(preferences = p, cafeRequest = cafe)
        assertEquals("brunch special", outputLib(lib, "print(daily_special());").trim())
    }

    @Test
    fun well_stocked_ignoresNpcAndRespectsLimitZero() {
        val item = ItemData(
            55, "stocked widget", "d55", "w.gif",
            ItemPrimaryUse.NONE, emptySet(), setOf('t'), 10, null,
        )
        val db = object : GameDatabase() {
            override fun item(name: String): ItemData? =
                if (name.equals("stocked widget", ignoreCase = true)) item else null
            override fun item(id: Int): ItemData? = if (id == 55) item else null
        }
        ItemDatabase.registerForTest(item)
        val mall = object : MallManager(
            searchRequest = net.sourceforge.kolmafia.mall.MallSearchRequest(
                HttpClient(MockEngine { respond("", HttpStatusCode.OK) }),
            ),
            purchaseRequest = net.sourceforge.kolmafia.mall.MallPurchaseRequest(
                HttpClient(MockEngine { respond("", HttpStatusCode.OK) }),
            ),
            gameDatabase = db,
        ) {
            override suspend fun searchListings(itemName: String, limit: Int): List<MallListing> =
                listOf(
                    MallListing(
                        shopId = 0, shopName = "NPC", itemId = 55, price = 25,
                        quantity = 100, limit = 0, source = MallListingSource.NPC,
                    ),
                    MallListing(
                        shopId = 123, shopName = "Mall", itemId = 55, price = 30,
                        quantity = 10, limit = 0, source = MallListingSource.MALL,
                    ),
                )
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, mallManager = mall)
        // quantity 6, price >= 2*autosell(10)=20 → mall row with limit 0 counts full qty
        assertEquals(
            "true",
            outputLib(lib, """print(well_stocked("stocked widget", 6, 50));""").trim().lowercase(),
        )
    }

    @Test
    fun refresh_shop_clearsThenRefetches() {
        StoreManager.addItem(1, 2, 100, 0)
        StoreManager.markSoldItemsRetrieved()
        assertTrue(StoreManager.soldItemsRetrieved)
        val req = object : net.sourceforge.kolmafia.request.ManageStoreRequest(
            HttpClient(MockEngine { respond("<table></table>", HttpStatusCode.OK) }),
        ) {
            override suspend fun fetchSoldItems(): Result<String> {
                StoreManager.update("<tr class=\"deets\"></tr>", StoreManager.TableType.DEETS)
                return Result.success("ok")
            }
        }
        val lib = GameRuntimeLibrary(manageStoreRequest = req, character = KoLCharacter())
        assertEquals("true", outputLib(lib, "print(refresh_shop());").trim().lowercase())
        assertTrue(StoreManager.soldItemsRetrieved)
    }

    @Test
    fun getStorage_persistsNopullsCache() {
        val fakeStorage = object : StorageRequest(
            HttpClient(MockEngine { respond("") }),
        ) {
            override suspend fun fetchClassifiedContents(
                characterState: CharacterState?,
                prefs: Preferences?,
            ): StoragePullRules.StorageContents =
                StoragePullRules.StorageContents(
                    storage = mapOf(1 to 2),
                    freepulls = emptyMap(),
                    nopulls = mapOf(3 to 5),
                )
        }
        val p = prefs()
        val lib = GameRuntimeLibrary(storageRequest = fakeStorage, preferences = p)
        outputLib(lib, "print(count(get_storage()));")
        assertEquals(5, CollectionCache.load(p, Preferences.CACHED_NOPULLS)[3])
    }
}
