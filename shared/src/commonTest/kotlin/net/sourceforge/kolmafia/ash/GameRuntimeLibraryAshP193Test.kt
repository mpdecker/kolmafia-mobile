package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger
import net.sourceforge.kolmafia.shop.ShopInventorySync
import net.sourceforge.kolmafia.shop.ShopRowDatabase
import net.sourceforge.kolmafia.shop.ShopType

class GameRuntimeLibraryAshP193Test {

    @Test
    fun revision_phase200() {
        assertEquals("phase333", GameRuntimeLibrary.REVISION)
    }

    @AfterTest
    fun cleanup() {
        ShopRowDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun stillShopVisit_logsConcoctionSessionFormat() {
        registerStillItems()
        ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "still\tNash Crosby's Still\tCONC\tSTILL\n",
        )
        assertEquals(ShopType.CONC, ShopRowDatabase.shopType("still"))
        assertEquals("STILL", ShopRowDatabase.craftingType("still"))

        val prefs = Preferences(MapSettings())
        val sessionLogger = SessionLogger(prefs, GameEventBus())
        ShopInventorySync.parseAndLearn(
            html = """
                <tr rel="$STILL_RESULT">
                <a onClick='javascript:descitem($STILL_RESULT)'><b>bottle of gin</b></a>
                <span title="bottle of vodka"><b>1</b></span>
                <form action="shop.php?action=buy&whichshop=still&whichrow=600">
                </tr>
            """.trimIndent(),
            url = "shop.php?whichshop=still",
            sessionLogger = sessionLogger,
        )

        val log = prefs.getString(SessionLogger.SESSION_LOG_KEY, "")
        assertTrue(log.contains("bottle of gin\tSTILL, ROW600\tbottle of vodka"))
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

    companion object {
        private const val STILL_RESULT = 99601
        private const val STILL_INGREDIENT = 99602
    }
}
