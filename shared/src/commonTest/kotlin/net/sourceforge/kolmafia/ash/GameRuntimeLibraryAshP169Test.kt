package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterVisitInventory
import net.sourceforge.kolmafia.shop.Crimbo25SammySync

class GameRuntimeLibraryAshP169Test {

    @AfterTest
    fun cleanup() {
        CoinmasterVisitInventory.resetForTest()
    }

    @Test
    fun revision_phase189() {
        assertEquals("phase247", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun crimbo25SammySync_populatesColdWadRow1649() {
        val prefs = Preferences(MapSettings())
        val html = """
            <tr rel="12121">
            <a onClick='javascript:descitem(12121)'><b>Crymbocurrency (5)</b></a>
            <span title="cold wad"><b>2</b></span>
            <form action="shop.php?action=buy&whichshop=crimbo25_sammy&whichrow=1649">
            </tr>
        """.trimIndent()

        Crimbo25SammySync.syncFromShopHtml(html, prefs)

        val row = CoinmasterVisitInventory.findBuyRow(
            CoinmasterVisitInventory.CRIMBO25_SAMMY,
            Crimbo25SammySync.CRYMBOCURRENCY,
        )
        assertEquals(1649, row?.rowId)
        assertEquals(Crimbo25SammySync.CRYMBOCURRENCY, row?.item?.itemId)
        assertEquals(5, row?.item?.count)
        assertEquals(Crimbo25SammySync.COLD_WAD, row?.costs?.single()?.itemId)
        assertEquals(2, row?.costs?.single()?.count)
    }

    @Test
    fun crimbo25SammySync_updatesTwinklyWadRow1650Cost() {
        val prefs = Preferences(MapSettings())
        Crimbo25SammySync.syncFromShopHtml(
            """
                <tr rel="12121">
                <a onClick='javascript:descitem(12121)'><b>Crymbocurrency (5)</b></a>
                <span title="twinkly wad (3)"><b>4</b></span>
                <form action="shop.php?action=buy&whichshop=crimbo25_sammy&whichrow=1650">
                </tr>
            """.trimIndent(),
            prefs,
        )

        val first = CoinmasterVisitInventory.findBuyRow(
            CoinmasterVisitInventory.CRIMBO25_SAMMY,
            Crimbo25SammySync.CRYMBOCURRENCY,
        )
        assertEquals(1650, first?.rowId)
        assertEquals(3, first?.costs?.single()?.count)

        Crimbo25SammySync.syncFromShopHtml(
            """
                <tr rel="12121">
                <a onClick='javascript:descitem(12121)'><b>Crymbocurrency (5)</b></a>
                <span title="twinkly wad (3)"><b>6</b></span>
                <form action="shop.php?action=buy&whichshop=crimbo25_sammy&whichrow=1650">
                </tr>
            """.trimIndent(),
            prefs,
        )

        val updated = CoinmasterVisitInventory.findBuyRow(
            CoinmasterVisitInventory.CRIMBO25_SAMMY,
            Crimbo25SammySync.CRYMBOCURRENCY,
        )
        assertEquals(1650, updated?.rowId)
        assertEquals(Crimbo25SammySync.TWINKLY_WAD, updated?.costs?.single()?.itemId)
        assertEquals(3, updated?.costs?.single()?.count)
    }
}
