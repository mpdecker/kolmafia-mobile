package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.CraftTypeDescription
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.item.CreatableTurns
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CafeDailySpecialSync
import net.sourceforge.kolmafia.request.CraftRequest
import net.sourceforge.kolmafia.session.StoreManager
import net.sourceforge.kolmafia.shop.CoinmasterData
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.ItemStack
import net.sourceforge.kolmafia.shop.ShopRow

/**
 * Focused XLVII Track B coverage (phases 6691–6710).
 * Revision bump deferred to parent (stays phase6850).
 */
class GameRuntimeLibraryPhase6710Test {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    @AfterTest
    fun tearDown() {
        StoreManager.clearCache()
        CoinmasterDatabase.resetForTest()
        ItemDatabase.resetForTest()
        ConcoctionDatabase.resetForTest()
    }

    @Test
    fun revisionStaysPhase6670() {
        assertEquals("phase6910", GameRuntimeLibrary.REVISION)
        assertEquals("6910", outputLib(GameRuntimeLibrary(), "print(get_revision());").trim())
    }

    @Test
    fun skillBuyPrice_singleCostShopRowOnly() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 9001, name = "skill token", descId = "d9001", image = "x.gif",
                primaryUse = ItemPrimaryUse.NONE, secondaryUses = emptySet(),
                access = setOf('t'), autosellPrice = 1, plural = null,
            ),
        )
        val master = CoinmasterData(
            masterName = "Skill Shop",
            nickname = "skillshop",
            token = "skill token",
            shopId = "skillshop",
            buyItems = listOf(
                ShopRow(
                    rowId = 11,
                    item = ItemStack(itemId = 55, count = 1, isSkill = true),
                    costs = listOf(ItemStack(itemId = 9001, count = 7)),
                ),
                ShopRow(
                    rowId = 12,
                    item = ItemStack(itemId = 56, count = 1, isSkill = true),
                    costs = listOf(
                        ItemStack(itemId = 9001, count = 3),
                        ItemStack(itemId = 9001, count = 4),
                    ),
                ),
            ),
            sellItems = emptyList(),
        )
        assertNotNull(master.skillBuyPrice(55))
        assertEquals(7, master.skillBuyPrice(55)?.count)
        assertNull(master.skillBuyPrice(56), "multi-cost skill rows are not skillBuyPrice")
        CoinmasterDatabase.registerForTest(master)
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals(
            "true",
            outputLib(lib, """print(sells_skill(to_coinmaster("skillshop"), to_skill(55)));""")
                .trim().lowercase(),
        )
        assertEquals(
            "false",
            outputLib(lib, """print(sells_skill(to_coinmaster("skillshop"), to_skill(56)));""")
                .trim().lowercase(),
        )
        assertEquals(
            "7",
            outputLib(lib, """print(sell_price(to_coinmaster("skillshop"), to_skill(55)));""").trim(),
        )
        assertEquals(
            "7",
            outputLib(
                lib,
                """print(sell_cost(to_coinmaster("skillshop"), to_skill(55))[to_item("skill token")]);""",
            ).trim(),
        )
    }

    @Test
    fun reprice_shop_updatesStoreManagerLocally() {
        StoreManager.addItem(42, 3, 100, 1)
        StoreManager.markSoldItemsRetrieved()
        val req = object : net.sourceforge.kolmafia.request.ManageStoreRequest(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
        ) {
            override suspend fun repriceItem(itemId: Int, price: Int, limit: Int): Result<String> =
                Result.success("no-json")
            override suspend fun fetchSoldItems(): Result<String> = Result.success("")
        }
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(hasstore = "1"))
        }
        val widget = ItemData(
            42, "widget", "d", "w.gif", ItemPrimaryUse.NONE, emptySet(), setOf('t'), 1, null,
        )
        ItemDatabase.registerForTest(widget)
        val db = object : GameDatabase() {
            override fun item(id: Int) = if (id == 42) widget else null
            override fun item(name: String) =
                if (name.equals("widget", true)) widget else null
        }
        val lib = GameRuntimeLibrary(
            manageStoreRequest = req,
            character = char,
            gameDatabase = db,
            preferences = prefs(),
        )
        assertEquals(
            "true",
            outputLib(lib, """print(reprice_shop(250, 5, to_item("widget")));""").trim().lowercase(),
        )
        assertEquals(250L, StoreManager.getPrice(42))
        assertEquals(5, StoreManager.getLimit(42))
        assertEquals(3, StoreManager.shopAmount(42))
    }

    @Test
    fun cafeDailySpecial_storesItemId() {
        val p = prefs()
        val html = """
            Today's Special:
            <input type=hidden name=whichitem value=806>
            <a href='javascript:void()' onclick='descitem("12345")'></a>
            <td>special lager (150 Meat)</td>
        """.trimIndent()
        CafeDailySpecialSync.parseResponse("cafe.php?cafeid=2", html, p)
        assertEquals(806, CafeDailySpecialSync.currentSpecialItemId(p))
        assertEquals("special lager", CafeDailySpecialSync.currentSpecialName(p))
        assertEquals(150, p.getInt("_dailySpecialPrice", 0))
    }

    @Test
    fun craft_rejectsUnknownMode() = runBlocking {
        val craft = CraftRequest(HttpClient(MockEngine { respond("x", HttpStatusCode.OK) }))
        assertEquals(0, craft.craft("alchemy", 1, 1, 2))
    }

    @Test
    fun creatableTurns_multiYieldAndPartialInventory() {
        ItemDatabase.registerForTest(
            ItemData(
                9512, "stacked product", "d", "x.gif",
                ItemPrimaryUse.NONE, emptySet(), setOf('t'), 1, null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                9513, "partial smith product", "d", "x.gif",
                ItemPrimaryUse.NONE, emptySet(), setOf('t'), 1, null,
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "stacked product",
                resultQuantity = 3,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "partial smith product",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )
        assertEquals(
            0,
            CreatableTurns.adventuresNeeded(9512, 3, inventoryCount = { 0 }, isPermitted = { true }),
        )
        assertEquals(
            1,
            CreatableTurns.adventuresNeeded(9513, 3, inventoryCount = { 2 }, isPermitted = { true }),
        )
    }

    @Test
    fun craft_type_manualQualifier() {
        ItemDatabase.registerForTest(
            ItemData(
                9600, "manual belt", "d", "x.gif",
                ItemPrimaryUse.NONE, emptySet(), setOf('t'), 1, null,
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "manual belt",
                resultQuantity = 1,
                methods = setOf("SMITH", "MANUAL"),
                ingredients = emptyList(),
            ),
        )
        assertEquals("Meatsmithing (MANUAL)", CraftTypeDescription.describe(setOf("SMITH", "MANUAL")))
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals(
            "Meatsmithing (MANUAL)",
            outputLib(lib, """print(craft_type(to_item("manual belt")));""").trim(),
        )
    }

    @Test
    fun get_shop_and_shop_amount_fromStoreManager() {
        StoreManager.addItem(7, 4, 99, 0)
        StoreManager.markSoldItemsRetrieved()
        val seeded = ItemData(
            7, "seeded item", "d", "x.gif",
            ItemPrimaryUse.NONE, emptySet(), setOf('t'), 1, null,
        )
        ItemDatabase.registerForTest(seeded)
        val db = object : GameDatabase() {
            override fun item(id: Int) = ItemDatabase.getById(id)
            override fun item(name: String) = ItemDatabase.getByName(name)
        }
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(hasstore = "1"))
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char, preferences = prefs())
        assertEquals(
            "4",
            outputLib(lib, """print(shop_amount(to_item("seeded item")));""").trim(),
        )
        assertEquals(
            "4",
            outputLib(lib, """print(get_shop()[to_item("seeded item")]);""").trim(),
        )
        assertTrue(StoreManager.soldItemsRetrieved)
    }
}
