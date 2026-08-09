package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseProbe
import net.sourceforge.kolmafia.shop.CoinmasterVisitInventory
import net.sourceforge.kolmafia.shop.Crimbo25SammySync
import net.sourceforge.kolmafia.shop.MerchTableSync
import net.sourceforge.kolmafia.shop.TimeTowerSync

class GameRuntimeLibraryAshP170Test {

    @AfterTest
    fun cleanup() {
        CoinmasterVisitInventory.resetForTest()
        CoinmasterDatabase.resetForTest()
    }

    @Test
    fun revision_phase189() {
        assertEquals("phase350", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun conmerchTattooValidateBlockedWithoutTower() {
        registerConmerchShop()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setBoolean(TimeTowerSync.PREF, false)

        assertFalse(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                MerchTableSync.TWITCHING_TELEVISION_TATTOO,
                net.sourceforge.kolmafia.character.CharacterState(meat = 100_000),
                prefs,
                accessibleCount = { if (it == 7567) 2000 else 0 },
            ),
        )
    }

    @Test
    fun conmerchTattooValidateBlockedBeforeVisitSync() {
        registerConmerchShop()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setBoolean(TimeTowerSync.PREF, true)

        assertFalse(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                MerchTableSync.TWITCHING_TELEVISION_TATTOO,
                net.sourceforge.kolmafia.character.CharacterState(meat = 100_000),
                prefs,
                accessibleCount = { if (it == 7567) 2000 else 0 },
            ),
        )
    }

    @Test
    fun conmerchTattooValidateAllowedAfterVisitSync() {
        registerConmerchShop()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setBoolean(TimeTowerSync.PREF, true)
        MerchTableSync.syncFromShopHtml(
            """
                <tr rel="9148">
                <a onClick='javascript:descitem(9148)'><b>Twitching Television Tattoo</b></a>
                <span title="Chroner"><b>1111</b></span>
                <form action="shop.php?action=buy&whichshop=conmerch&whichrow=895">
                </tr>
            """.trimIndent(),
            prefs,
        )

        assertTrue(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                MerchTableSync.TWITCHING_TELEVISION_TATTOO,
                net.sourceforge.kolmafia.character.CharacterState(meat = 100_000),
                prefs,
                accessibleCount = { if (it == 7567) 2000 else 0 },
            ),
        )
    }

    @Test
    fun crimbo25SammyValidateBlockedBeforeVisitSync() {
        registerSammyShop()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)

        assertFalse(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                Crimbo25SammySync.CRYMBOCURRENCY,
                net.sourceforge.kolmafia.character.CharacterState(meat = 100_000),
                prefs,
                accessibleCount = { if (it == Crimbo25SammySync.COLD_WAD) 5 else 0 },
            ),
        )
    }

    @Test
    fun crimbo25SammyValidateAllowedAfterVisitSyncWithColdWad() {
        registerSammyShop()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        Crimbo25SammySync.syncFromShopHtml(
            """
                <tr rel="12121">
                <a onClick='javascript:descitem(12121)'><b>Crymbocurrency (5)</b></a>
                <span title="cold wad"><b>2</b></span>
                <form action="shop.php?action=buy&whichshop=crimbo25_sammy&whichrow=1649">
                </tr>
            """.trimIndent(),
            prefs,
        )

        assertTrue(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                Crimbo25SammySync.CRYMBOCURRENCY,
                net.sourceforge.kolmafia.character.CharacterState(meat = 100_000),
                prefs,
                accessibleCount = { if (it == Crimbo25SammySync.COLD_WAD) 5 else 0 },
            ),
        )
    }

    private fun registerConmerchShop() {
        CoinmasterDatabase.loadFromText(
            shopsText = "conmerch\tKoL Con 13 Merch Table\n",
            coinText = "KoL Con 13 Merch Table\tbuy\t1\tTwitching Television Tattoo\tROW895\n",
        )
    }

    private fun registerSammyShop() {
        CoinmasterDatabase.loadFromText(
            shopsText = "crimbo25_sammy\tThe HMS Bounty Hunter\n",
            coinText = "The HMS Bounty Hunter\tROW1649\tCrymbocurrency (5)\tcold wad\n",
        )
    }
}
