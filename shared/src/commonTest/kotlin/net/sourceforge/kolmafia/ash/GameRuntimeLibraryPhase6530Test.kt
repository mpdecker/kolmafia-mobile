package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionRuntimeState
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.item.RetrievePricing
import net.sourceforge.kolmafia.mall.MallListing
import net.sourceforge.kolmafia.mall.MallPriceDatabase
import net.sourceforge.kolmafia.mall.MallPriceManager
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * XLIV Track B focused coverage (phases 6511–6530).
 * REVISION stays phase6490 until parent mega wrap-up.
 */
class GameRuntimeLibraryPhase6530Test {

    private class StubDb(private val name: String, private val id: Int = 9_000_530) :
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

    @Test
    fun revision_unchanged() {
        assertEquals("phase6670", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun mall_price_maxAge_forceUpdatePrefetch() {
        val clock = MallPriceManager.TestClock(10_000L)
        val mpm = MallPriceManager(clock)
        mpm.cachePrice(9_000_530, 111L, 1, 1)
        clock.nowSeconds = 10_000L + 200_000L // stale vs 1.0 day? 200000/86400 ≈ 2.3 days
        mpm.mallSearchSync = { listOf(
            MallListing(
                shopId = 7,
                shopName = "shop",
                itemId = 9_000_530,
                price = 555L,
                quantity = 10,
                limit = 10,
            ),
        ) }
        val lib = GameRuntimeLibrary(
            gameDatabase = StubDb("xliv tooth"),
            mallPriceManager = mpm,
        )
        assertEquals(
            "555",
            outputLib(lib, """print(to_string(mall_price(to_item("xliv tooth"), 0.5)));"""),
        )
    }

    @Test
    fun historical_age_dbInfinity_thenCacheDays() {
        MallPriceDatabase.resetForTest()
        val clock = MallPriceManager.TestClock(5_000L)
        val mpm = MallPriceManager(clock)
        mpm.cachePrice(9_000_530, 250L, 1, 1)
        clock.nowSeconds = 5_000L + 1_800L // within TTL (3600s) so session age is finite
        val lib = GameRuntimeLibrary(
            gameDatabase = StubDb("xliv tooth"),
            mallPriceManager = mpm,
        )
        val out = outputLib(lib, """print(to_string(historical_age(to_item("xliv tooth"))));""")
        assertTrue(out.toDouble() > 0.0 && out.toDouble().isFinite())
        assertTrue(
            outputLib(lib, """print(to_string(historical_age(to_item("no such"))));""")
                .contains("Infinity"),
        )
    }

    @Test
    fun retrieve_price_qtyZero_andExactArity() {
        val lib = GameRuntimeLibrary(gameDatabase = StubDb("xliv tooth"))
        assertEquals(
            "0",
            outputLib(lib, """print(to_string(retrieve_price(to_item("xliv tooth"), 0)));"""),
        )
        assertEquals(
            "0",
            outputLib(lib, """print(to_string(retrieve_price(0, to_item("xliv tooth"))));"""),
        )
    }

    @Test
    fun accessibleOnHand_subtractsQueuedPulls() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 9_000_531,
                name = "xliv pullable",
                descId = "d",
                image = "x.gif",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        ConcoctionDatabase.setRuntimeForTest(
            "xliv pullable",
            ConcoctionRuntimeState(queuedPulls = 2),
        )
        try {
            assertEquals(
                3,
                RetrievePricing.accessibleOnHandCount(
                    itemId = 9_000_531,
                    inventoryCount = { 5 },
                    physicalAccessible = { 5 },
                ),
            )
        } finally {
            ConcoctionDatabase.setRuntimeForTest(
                "xliv pullable",
                ConcoctionRuntimeState(),
            )
        }
    }

    @Test
    fun buy_coinmaster_and_autosell_batch_coalesce() {
        val lib = GameRuntimeLibrary(gameDatabase = StubDb("seal tooth", id = 2))
        val out = outputLib(
            lib,
            """
            batch_open();
            autosell(1, to_item("seal tooth"));
            sell(2, to_item("seal tooth"));
            print(has_queued_commands());
            batch_close();
            print(has_queued_commands());
            """.trimIndent(),
        )
        assertTrue(out.contains("true"), "queued while batch open: $out")
        assertTrue(out.trim().endsWith("false") || out.contains("true\nfalse"), "cleared after close: $out")
    }

    @Test
    fun retrieve_item_countZero_true() {
        assertEquals(
            "true",
            outputLib(
                GameRuntimeLibrary(),
                """print(retrieve_item(0, to_item("none")));""",
            ).trim().lowercase(),
        )
    }

    @Test
    fun historical_price_prefersDb() {
        MallPriceDatabase.resetForTest()
        MallPriceDatabase.recordPrice(9_000_530, 999L, timestampSeconds = 1_000L, deferred = true)
        val mpm = MallPriceManager(MallPriceManager.TestClock(2_000L))
        mpm.cachePrice(9_000_530, 111L, 1, 1)
        val lib = GameRuntimeLibrary(
            gameDatabase = StubDb("xliv tooth"),
            mallPriceManager = mpm,
        )
        assertEquals(
            "999",
            outputLib(lib, """print(to_string(historical_price(to_item("xliv tooth"))));"""),
        )
        MallPriceDatabase.resetForTest()
    }

    @Test
    fun prefs_smoke() {
        Preferences(MapSettings()).setBoolean("debugBuy", false)
    }
}
