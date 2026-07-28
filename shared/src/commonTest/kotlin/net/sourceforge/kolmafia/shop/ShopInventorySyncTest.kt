package net.sourceforge.kolmafia.shop

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

class ShopInventorySyncTest {

    @AfterTest
    fun cleanup() {
        CoinmasterDatabase.resetForTest()
        ShopRowDatabase.resetForTest()
        CoinmasterVisitInventory.resetForTest()
        NpcStoreVisitOverlay.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun parseAndLearn_logsUnknownRowAndRegistersOverlay() {
        registerItems()
        CoinmasterDatabase.loadFromText(
            shopsText = "fdkol\tFDKOL Requisitions Tent\tNPCCOIN\n",
            coinText = "",
        )
        ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "fdkol\tFDKOL Requisitions Tent\n",
        )
        val prefs = Preferences(MapSettings())
        val sessionLogger = SessionLogger(prefs, GameEventBus())
        val html = """
            <tr rel="$VISIT_ITEM">
            <a onClick='javascript:descitem($VISIT_ITEM)'><b>visit-learned item</b></a>
            <span title="FDKOL commendation"><b>75</b></span>
            <form action="shop.php?action=buy&whichshop=fdkol&whichrow=1500">
            </tr>
        """.trimIndent()

        ShopInventorySync.parseAndLearn(
            html = html,
            url = "shop.php?whichshop=fdkol",
            sessionLogger = sessionLogger,
        )

        assertTrue(CoinmasterVisitInventory.hasVisited("fdkol"))
        assertTrue(CoinmasterVisitInventory.containsItem("fdkol", VISIT_ITEM))
        assertNotNull(ShopRowDatabase.getShopRow(1500))
        val log = prefs.getString(SessionLogger.SESSION_LOG_KEY, "")
        assertTrue(log.contains("1500\tfdkol\tvisit-learned item\tFDKOL commendation (75)"))
        assertTrue(log.contains("FDKOL Requisitions Tent\tROW1500\tvisit-learned item"))
    }

    @Test
    fun parseAndLearn_skipsAjaxAndUhOh() {
        registerItems()
        ShopInventorySync.parseAndLearn(
            html = visitHtml(),
            url = "shop.php?whichshop=fdkol&ajax=1",
            sessionLogger = SessionLogger(Preferences(MapSettings()), GameEventBus()),
        )
        assertFalse(CoinmasterVisitInventory.hasVisited("fdkol"))

        ShopInventorySync.parseAndLearn(
            html = "<b style=\"color: white\">Uh Oh!</b>",
            url = "shop.php?whichshop=fdkol",
            sessionLogger = SessionLogger(Preferences(MapSettings()), GameEventBus()),
        )
        assertFalse(CoinmasterVisitInventory.hasVisited("fdkol"))
    }

    @Test
    fun parseAndLearn_skipsKnownBundledRow() {
        registerItems()
        ShopRowDatabase.loadFromText(
            shopRowsText = "1500\tfdkol\tvisit-learned item\tFDKOL commendation (75)\n",
            shopsText = "fdkol\tFDKOL Requisitions Tent\n",
        )
        val prefs = Preferences(MapSettings())
        val sessionLogger = SessionLogger(prefs, GameEventBus())
        ShopInventorySync.parseAndLearn(
            html = visitHtml(),
            url = "shop.php?whichshop=fdkol",
            sessionLogger = sessionLogger,
        )
        assertEquals("", prefs.getString(SessionLogger.SESSION_LOG_KEY, ""))
    }

    @Test
    fun parseAndLearn_logsConcoctionFormatForConcShop() {
        registerStillItems()
        ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "still\tNash Crosby's Still\tCONC\tSTILL\n",
        )
        val prefs = Preferences(MapSettings())
        val sessionLogger = SessionLogger(prefs, GameEventBus())
        val html = """
            <tr rel="$STILL_RESULT">
            <a onClick='javascript:descitem($STILL_RESULT)'><b>bottle of gin</b></a>
            <span title="bottle of vodka"><b>1</b></span>
            <form action="shop.php?action=buy&whichshop=still&whichrow=500">
            </tr>
        """.trimIndent()

        ShopInventorySync.parseAndLearn(
            html = html,
            url = "shop.php?whichshop=still",
            sessionLogger = sessionLogger,
        )

        assertFalse(CoinmasterVisitInventory.hasVisited("still"))
        val log = prefs.getString(SessionLogger.SESSION_LOG_KEY, "")
        assertTrue(log.contains("bottle of gin\tSTILL, ROW500\tbottle of vodka"))
        assertFalse(log.contains("Nash Crosby's Still\tROW500"))
    }

    private fun registerStillItems() {
        ItemDatabase.registerForTest(
            ItemData(
                id = STILL_RESULT,
                name = "bottle of gin",
                descId = "d$STILL_RESULT",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = STILL_INGREDIENT,
                name = "bottle of vodka",
                descId = "d$STILL_INGREDIENT",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    private fun visitHtml() = """
        <tr rel="$VISIT_ITEM">
        <a onClick='javascript:descitem($VISIT_ITEM)'><b>visit-learned item</b></a>
        <span title="FDKOL commendation"><b>75</b></span>
        <form action="shop.php?action=buy&whichshop=fdkol&whichrow=1500">
        </tr>
    """.trimIndent()

    private fun registerItems() {
        ItemDatabase.registerForTest(
            ItemData(
                id = VISIT_ITEM,
                name = "visit-learned item",
                descId = "d$VISIT_ITEM",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = FDKOL_COMMENDATION,
                name = "FDKOL commendation",
                descId = "d$FDKOL_COMMENDATION",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    companion object {
        private const val VISIT_ITEM = 99201
        private const val FDKOL_COMMENDATION = 99202
        private const val STILL_RESULT = 99203
        private const val STILL_INGREDIENT = 99204
    }
}
