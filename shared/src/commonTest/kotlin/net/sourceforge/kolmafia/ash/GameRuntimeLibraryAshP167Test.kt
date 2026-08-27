package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterVisitInventory
import net.sourceforge.kolmafia.shop.FlowerTradeinAccessibility
import net.sourceforge.kolmafia.shop.FlowerTradeinSync
import net.sourceforge.kolmafia.shop.MerchTableSync

class GameRuntimeLibraryAshP167Test {

    @AfterTest
    fun cleanup() {
        CoinmasterVisitInventory.resetForTest()
    }

    @Test
    fun revision_phase186() {
        assertEquals("phase3050", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun flowerTradeinSync_populatesRoseRow759() {
        val prefs = Preferences(MapSettings())
        val html = """
            <tr rel="7567">
            <a onClick='javascript:descitem(7567)'><b>Chroner</b></a>
            <span title="rose"><b>1</b></span>
            <form action="shop.php?action=buy&whichshop=flowertradein&whichrow=759">
            </tr>
        """.trimIndent()

        FlowerTradeinSync.syncFromShopHtml(html, prefs)

        val row = CoinmasterVisitInventory.findBuyRow(CoinmasterVisitInventory.FLOWER_TRADEIN, FlowerTradeinSync.CHRONER)
        assertEquals(759, row?.rowId)
        assertEquals(FlowerTradeinSync.CHRONER, row?.item?.itemId)
        assertEquals(FlowerTradeinAccessibility.ROSE, row?.costs?.single()?.itemId)
        assertEquals(1, row?.costs?.single()?.count)
    }

    @Test
    fun merchTableSync_setsTokenBalancePrefs() {
        val prefs = Preferences(MapSettings())
        val html = """
            You have 1,234 Mr. Accessories to trade.
            You have 56 Mr. Chroner to trade.
        """.trimIndent()

        MerchTableSync.syncFromShopHtml(html, prefs)

        assertEquals(1234, prefs.getInt(MerchTableSync.AVAILABLE_MR_A_PREF, -1))
        assertEquals(56, prefs.getInt(MerchTableSync.AVAILABLE_CHRONERS_PREF, -1))
    }
}
