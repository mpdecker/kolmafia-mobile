package net.sourceforge.kolmafia.shop

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences

class MerchTableSyncTest {

    @AfterTest
    fun cleanup() {
        CoinmasterVisitInventory.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun syncFromShopHtml_mapsChronerAndMrAccessoryCosts() {
        registerItems()
        val prefs = Preferences(MapSettings())
        val html = """
            <tr rel="$SHOP_ITEM">
            <a onClick='javascript:descitem($SHOP_ITEM)'><b>merch item</b></a>
            <span title="Chroner"><b>3</b></span>
            <form action="shop.php?whichshop=conmerch&whichrow=100">
            </tr>
            <tr rel="$SHOP_ITEM2">
            <a onClick='javascript:descitem($SHOP_ITEM2)'><b>merch item two</b></a>
            <span title="Mr. Accessory"><b>5</b></span>
            <form action="shop.php?whichshop=conmerch&whichrow=101">
            </tr>
        """.trimIndent()

        MerchTableSync.syncFromShopHtml(html, prefs)

        val chronerRow = CoinmasterVisitInventory.findBuyRow(MerchTableSync.SHOP_ID, SHOP_ITEM)
        assertEquals(7567, chronerRow?.costs?.single()?.itemId)
        assertEquals(3, chronerRow?.costs?.single()?.count)

        val mrARow = CoinmasterVisitInventory.findBuyRow(MerchTableSync.SHOP_ID, SHOP_ITEM2)
        assertEquals(194, mrARow?.costs?.single()?.itemId)
        assertEquals(5, mrARow?.costs?.single()?.count)
    }

    private fun registerItems() {
        listOf(
            SHOP_ITEM to "merch item",
            SHOP_ITEM2 to "merch item two",
            7567 to "Chroner",
            194 to "Mr. Accessory",
        ).forEach { (id, name) ->
            ItemDatabase.registerForTest(
                ItemData(
                    id = id,
                    name = name,
                    descId = "d$id",
                    image = "img",
                    primaryUse = ItemPrimaryUse.USABLE,
                    secondaryUses = emptySet(),
                    access = setOf('t'),
                    autosellPrice = 1,
                    plural = null,
                ),
            )
        }
    }

    companion object {
        private const val SHOP_ITEM = 99401
        private const val SHOP_ITEM2 = 99402
    }
}
