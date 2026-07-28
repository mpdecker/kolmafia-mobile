package net.sourceforge.kolmafia.shop

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import net.sourceforge.kolmafia.preferences.Preferences

class FlowerTradeinSyncTest {

    @AfterTest
    fun cleanup() {
        CoinmasterVisitInventory.resetForTest()
    }

    @Test
    fun roseForChroner_mapsItem7567AndRoseCostTimes2() {
        val prefs = Preferences(MapSettings())
        FlowerTradeinSync.syncFromShopHtml(
            """
                <tr rel="7567">
                <a onClick='javascript:descitem(7567)'><b>Chroner</b></a>
                <span title="rose"><b>2</b></span>
                <form action="shop.php?action=buy&whichshop=flowertradein&whichrow=759">
                </tr>
            """.trimIndent(),
            prefs,
        )

        val row = CoinmasterVisitInventory.findBuyRow(FlowerTradeinSync.SHOP_ID, FlowerTradeinSync.CHRONER)
        assertEquals(759, row?.rowId)
        assertEquals(FlowerTradeinSync.CHRONER, row?.item?.itemId)
        assertEquals(1, row?.item?.count)
        assertEquals(FlowerTradeinAccessibility.ROSE, row?.costs?.single()?.itemId)
        assertEquals(2, row?.costs?.single()?.count)
    }

    @Test
    fun chronerStackRow_mapsItemCount16() {
        val prefs = Preferences(MapSettings())
        FlowerTradeinSync.syncFromShopHtml(
            """
                <tr rel="7567">
                <a onClick='javascript:descitem(7567)'><b>Chroner (16)</b></a>
                <span title="red tulip"><b>1</b></span>
                <form action="shop.php?action=buy&whichshop=flowertradein&whichrow=760">
                </tr>
            """.trimIndent(),
            prefs,
        )

        val row = CoinmasterVisitInventory.findBuyRow(FlowerTradeinSync.SHOP_ID, FlowerTradeinSync.CHRONER)
        assertEquals(760, row?.rowId)
        assertEquals(16, row?.item?.count)
        assertEquals(FlowerTradeinAccessibility.RED_TULIP, row?.costs?.single()?.itemId)
        assertEquals(1, row?.costs?.single()?.count)
    }

    @Test
    fun mapRow_regressionMatchesAshP167RoseRow() {
        val row = FlowerTradeinSync.mapRow(
            ShopRowParser.ParsedSingleCostRow(
                rowId = 759,
                itemId = 7567,
                itemName = "Chroner",
                currencyName = "rose",
                price = 1,
            ),
        )
        assertEquals(759, row?.rowId)
        assertEquals(FlowerTradeinSync.CHRONER, row?.item?.itemId)
        assertEquals(1, row?.item?.count)
        assertEquals(FlowerTradeinAccessibility.ROSE, row?.costs?.single()?.itemId)
        assertEquals(1, row?.costs?.single()?.count)
    }

    @Test
    fun mapRow_unknownCurrencyReturnsNull() {
        assertNull(
            FlowerTradeinSync.mapRow(
                ShopRowParser.ParsedSingleCostRow(
                    rowId = 1,
                    itemId = 7567,
                    itemName = "Chroner",
                    currencyName = "mystery bloom",
                    price = 1,
                ),
            ),
        )
    }
}
