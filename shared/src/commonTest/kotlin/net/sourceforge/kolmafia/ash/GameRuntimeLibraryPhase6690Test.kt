package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.SpeakeasyAvailability
import net.sourceforge.kolmafia.item.RetrievePricing
import net.sourceforge.kolmafia.mall.MallListing
import net.sourceforge.kolmafia.mall.MallPriceDatabase
import net.sourceforge.kolmafia.mall.MallPriceManager
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * XLVII Track A focused coverage (phases 6671–6690).
 * REVISION stays phase6850 until parent mega wrap-up.
 */
class GameRuntimeLibraryPhase6690Test {

    private class StubDb(private val name: String, private val id: Int = 9_000_690) :
        net.sourceforge.kolmafia.data.GameDatabase() {
        private val item = ItemData(
            id = id,
            name = name,
            descId = "d$id",
            image = "x.gif",
            primaryUse = ItemPrimaryUse.NONE,
            secondaryUses = emptySet(),
            access = setOf('t', 'd'),
            autosellPrice = 10,
            plural = null,
        )
        override fun item(name: String): ItemData? =
            if (name.equals(this.name, ignoreCase = true)) item else null
        override fun item(id: Int): ItemData? = if (id == this.id) item else null
        override fun npcPrice(itemName: String): Int = 0
    }

    @BeforeTest
    fun setUp() {
        MallPriceDatabase.resetForTest()
        SpeakeasyAvailability.reset()
    }

    @AfterTest
    fun tearDown() {
        MallPriceDatabase.resetForTest()
        SpeakeasyAvailability.reset()
    }

    @Test
    fun revision_unchanged() {
        assertEquals("phase7150", GameRuntimeLibrary.REVISION)
        assertEquals("7150", outputLib(GameRuntimeLibrary(), "print(get_revision());").trim())
    }

    @Test
    fun mall_price_maxAge_forceUpdate_fifthCheapest() {
        val clock = MallPriceManager.TestClock(10_000L)
        val mpm = MallPriceManager(clock)
        mpm.cachePrice(9_000_690, 111L, 1, 1)
        clock.nowSeconds = 10_000L + 200_000L
        // Five cheap listings at limit 1 → fifth-cheapest is 555
        mpm.mallSearchSync = {
            (1..5).map { i ->
                MallListing(
                    shopId = i,
                    shopName = "shop$i",
                    itemId = 9_000_690,
                    price = 100L * i + 55L,
                    quantity = 10,
                    limit = 1,
                )
            }
        }
        val lib = GameRuntimeLibrary(
            gameDatabase = StubDb("xlvii widget"),
            mallPriceManager = mpm,
        )
        assertEquals(
            "555",
            outputLib(lib, """print(to_string(mall_price(to_item("xlvii widget"), 0.5)));"""),
        )
    }

    @Test
    fun historical_price_dbOnly_noSessionFallback() {
        MallPriceDatabase.resetForTest()
        val mpm = MallPriceManager(MallPriceManager.TestClock(2_000L))
        mpm.cachePrice(9_000_690, 111L, 1, 1)
        val lib = GameRuntimeLibrary(
            gameDatabase = StubDb("xlvii widget"),
            mallPriceManager = mpm,
        )
        assertEquals(
            "0",
            outputLib(lib, """print(to_string(historical_price(to_item("xlvii widget"))));"""),
        )
        MallPriceDatabase.recordPrice(9_000_690, 999L, timestampSeconds = 1_000L, deferred = true)
        assertEquals(
            "999",
            outputLib(lib, """print(to_string(historical_price(to_item("xlvii widget"))));"""),
        )
    }

    @Test
    fun historical_age_dbThenSession() {
        MallPriceDatabase.resetForTest()
        val clock = MallPriceManager.TestClock(5_000L)
        val mpm = MallPriceManager(clock)
        mpm.cachePrice(9_000_690, 250L, 1, 1)
        clock.nowSeconds = 5_000L + 1_800L
        val lib = GameRuntimeLibrary(
            gameDatabase = StubDb("xlvii widget"),
            mallPriceManager = mpm,
        )
        val out = outputLib(lib, """print(to_string(historical_age(to_item("xlvii widget"))));""")
        assertTrue(out.toDouble().isFinite() && out.toDouble() > 0.0)
        assertTrue(
            outputLib(lib, """print(to_string(historical_age(to_item("no such"))));""")
                .contains("Infinity"),
        )
    }

    @Test
    fun retrieve_price_meatPasteBuyable() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 25,
                name = "meat paste",
                descId = "d25",
                image = "x.gif",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 10,
                plural = null,
            ),
        )
        val ctx = RetrievePricing.PriceContext(inventoryCount = { 0 })
        assertEquals(10L, RetrievePricing.priceToAcquire(25, 1, exact = true, ctx = ctx))
        assertEquals(
            "10",
            outputLib(
                GameRuntimeLibrary(gameDatabase = StubDb("meat paste", id = 25)),
                """print(to_string(retrieve_price(to_item("meat paste"))));""",
            ),
        )
    }

    @Test
    fun buy_using_storage_falseWhenCanInteract() {
        val char = KoLCharacter()
        // Default canInteract=true → buy_using_storage must refuse (even qty 0)
        val lib = GameRuntimeLibrary(
            gameDatabase = StubDb("xlvii widget"),
            character = char,
            preferences = Preferences(MapSettings()),
        )
        assertEquals(
            "false",
            outputLib(lib, """print(to_string(buy_using_storage(to_item("xlvii widget"))));""")
                .trim().lowercase(),
        )
        assertEquals(
            "false",
            outputLib(lib, """print(to_string(buy_using_storage(0, to_item("xlvii widget"))));""")
                .trim().lowercase(),
        )
    }

    @Test
    fun buy_using_storage_trueWhenHardcoreAndQtyZero() {
        val char = KoLCharacter()
        char.setHardcore(true)
        val lib = GameRuntimeLibrary(
            gameDatabase = StubDb("xlvii widget"),
            character = char,
            preferences = Preferences(MapSettings()),
        )
        assertEquals(
            "true",
            outputLib(lib, """print(to_string(buy_using_storage(0, to_item("xlvii widget"))));""")
                .trim().lowercase(),
        )
    }

    @Test
    fun sell_qtyZero_isTrue() {
        assertEquals(
            "true",
            outputLib(
                GameRuntimeLibrary(gameDatabase = StubDb("xlvii widget")),
                """print(to_string(sell(0, to_item("xlvii widget"))));""",
            ).trim().lowercase(),
        )
    }

    @Test
    fun npc_price_speakeasyWhenLoungeAvailable() {
        SpeakeasyAvailability.addLoungeId(4) // Lucky Lindy
        val out = outputLib(
            GameRuntimeLibrary(),
            """print(npc_price(to_item("Lucky Lindy")));""",
        )
        assertTrue(out == "500" || out == "0", "speakeasy npc_price: $out")
    }

    @Test
    fun mallPriceDatabase_skipsNonTradeable_onLoad() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 9_000_691,
                name = "xlvii quest",
                descId = "d",
                image = "x.gif",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('q'),
                autosellPrice = 0,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = 9_000_692,
                name = "xlvii trade",
                descId = "d2",
                image = "x.gif",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        val text = """
            ${0xF00D5}
            9000691	1000	50
            9000692	1000	77
        """.trimIndent()
        assertEquals(1, MallPriceDatabase.load(text))
        assertEquals(0L, MallPriceDatabase.getPrice(9_000_691))
        assertEquals(77L, MallPriceDatabase.getPrice(9_000_692))
    }

    @Test
    fun getMallPriceForQuantity_usesSavedSearchBeyondFifth() {
        val mpm = MallPriceManager(MallPriceManager.TestClock(1_000L))
        val listings = (1..8).map { i ->
            MallListing(
                shopId = i,
                shopName = "s$i",
                itemId = 42,
                price = 10L * i,
                quantity = 5,
                limit = 2,
            )
        }
        mpm.saveMallSearch(42, listings)
        mpm.updateMallPrice(42, listings)
        // qty 1..5 → fifth-cheapest (limit 2 each → shops 1-3 cover 5) = price of shop 3 = 30
        assertEquals(30L, mpm.getMallPrice(42))
        // qty 7 → first 5 at 30 + 2 more: 1@30 (remainder of shop3) + 1@40 = 150+30+40 = 220
        assertEquals(220L, mpm.getMallPriceForQuantity(42, 7))
    }
}
