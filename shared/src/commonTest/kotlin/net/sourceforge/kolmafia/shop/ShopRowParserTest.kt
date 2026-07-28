package net.sourceforge.kolmafia.shop

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse

class ShopRowParserTest {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
    }

    @Test
    fun parseShop_singleCostCoinRow() {
        registerItems()
        val html = """
            <tr rel="$VISIT_ITEM">
            <a onClick='javascript:descitem($VISIT_ITEM)'><b>visit-learned item</b></a>
            <span title="FDKOL commendation"><b>75</b></span>
            <form action="shop.php?action=buy&whichshop=fdkol&whichrow=1500">
            </tr>
        """.trimIndent()

        val rows = ShopRowParser.parseShop(html)
        assertEquals(1, rows.size)
        assertEquals(1500, rows[0].rowId)
        assertEquals(VISIT_ITEM, rows[0].item.itemId)
        assertEquals(FDKOL_COMMENDATION, rows[0].costs.single().itemId)
        assertEquals(75, rows[0].costs.single().count)
    }

    @Test
    fun parseShop_meatRow() {
        registerItems()
        val html = """
            <tr rel="$MEAT_ITEM">
            <a onClick='javascript:descitem($MEAT_ITEM)'><b>meat snack</b></a>
            <span title="Meat"><b>1,000</b></span>
            <form action="shop.php?action=buy&whichshop=fdkol&whichrow=1501">
            </tr>
        """.trimIndent()

        val rows = ShopRowParser.parseShop(html)
        assertEquals(1, rows.size)
        assertTrue(rows[0].isMeatPurchase)
        assertEquals(1000, rows[0].costs.single().count)
    }

    @Test
    fun parseShop_skipsMalformedRows() {
        val rows = ShopRowParser.parseShop("<table><tr><td>no shop row</td></tr></table>")
        assertTrue(rows.isEmpty())
    }

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
        ItemDatabase.registerForTest(
            ItemData(
                id = MEAT_ITEM,
                name = "meat snack",
                descId = "d$MEAT_ITEM",
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
        private const val VISIT_ITEM = 99101
        private const val FDKOL_COMMENDATION = 99102
        private const val MEAT_ITEM = 99103
    }
}
