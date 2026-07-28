package net.sourceforge.kolmafia.shop

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences

class Crimbo25SammySyncTest {

    @AfterTest
    fun cleanup() {
        CoinmasterVisitInventory.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun syncFromShopHtml_parsesWadCostAndCrymbocurrencyStack() {
        registerItems()
        val prefs = Preferences(MapSettings())
        val html = """
            <tr rel="$TRADE_ITEM">
            <a onClick='javascript:descitem($TRADE_ITEM)'><b>trade good</b></a>
            <span title="cold wad"><b>2</b></span>
            <form action="shop.php?whichshop=crimbo25_sammy&whichrow=200">
            </tr>
            <tr rel="${Crimbo25SammySync.CRYMBOCURRENCY}">
            <a onClick='javascript:descitem(${Crimbo25SammySync.CRYMBOCURRENCY})'><b>Crymbocurrency (4)</b></a>
            <span title="twinkly wad (3)"><b>1</b></span>
            <form action="shop.php?whichshop=crimbo25_sammy&whichrow=201">
            </tr>
        """.trimIndent()

        Crimbo25SammySync.syncFromShopHtml(html, prefs)

        val tradeRow = CoinmasterVisitInventory.findBuyRow(Crimbo25SammySync.SHOP_ID, TRADE_ITEM)
        assertEquals(Crimbo25SammySync.COLD_WAD, tradeRow?.costs?.single()?.itemId)
        assertEquals(2, tradeRow?.costs?.single()?.count)

        val crymRow = CoinmasterVisitInventory.findBuyRow(
            Crimbo25SammySync.SHOP_ID,
            Crimbo25SammySync.CRYMBOCURRENCY,
        )
        assertEquals(4, crymRow?.item?.count)
        assertEquals(Crimbo25SammySync.TWINKLY_WAD, crymRow?.costs?.single()?.itemId)
        assertEquals(3, crymRow?.costs?.single()?.count)
    }

    private fun registerItems() {
        listOf(
            TRADE_ITEM to "trade good",
            Crimbo25SammySync.COLD_WAD to "cold wad",
            Crimbo25SammySync.TWINKLY_WAD to "twinkly wad",
            Crimbo25SammySync.CRYMBOCURRENCY to "Crymbocurrency",
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
        private const val TRADE_ITEM = 99501
    }
}
