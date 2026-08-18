package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseProbe
import net.sourceforge.kolmafia.shop.CoinmasterSyncedTokenCount
import net.sourceforge.kolmafia.shop.CoinmasterVisitInventory
import net.sourceforge.kolmafia.shop.Crimbo23ShopSync
import net.sourceforge.kolmafia.shop.MerchTableSync
import net.sourceforge.kolmafia.shop.TimeTowerSync

class GameRuntimeLibraryAshP174Test {

    @AfterTest
    fun cleanup() {
        CoinmasterVisitInventory.resetForTest()
        CoinmasterDatabase.resetForTest()
    }

    @Test
    fun revision_phase189() {
        assertEquals("phase635", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun conmerchTattooValidateAllowedWithChronerPrefOnly() {
        registerConmerchShop()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setBoolean(TimeTowerSync.PREF, true)
        prefs.setInt(MerchTableSync.AVAILABLE_CHRONERS_PREF, 2000)
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

        assertTrue(canPurchase(MerchTableSync.TWITCHING_TELEVISION_TATTOO, prefs))
    }

    @Test
    fun mulledWineValidateBlockedWhenBarClosed() {
        registerBarShop()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setString("crimbo23BarControl", "none")
        prefs.setInt(Crimbo23ShopSync.AVAILABLE_ELF_MPC_PREF, 20)

        assertFalse(canPurchase(MULLED_WINE, prefs))
    }

    @Test
    fun mulledWineValidateBlockedWhenPiratesControlBar() {
        registerBarShop()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setString("crimbo23BarControl", "pirate")
        prefs.setInt(Crimbo23ShopSync.AVAILABLE_ELF_MPC_PREF, 20)

        assertFalse(canPurchase(MULLED_WINE, prefs))
    }

    @Test
    fun mulledWineValidateAllowedWithElfControlAndMpcPref() {
        registerBarShop()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setString("crimbo23BarControl", "elf")
        prefs.setInt(Crimbo23ShopSync.AVAILABLE_ELF_MPC_PREF, 20)

        assertTrue(canPurchase(MULLED_WINE, prefs))
    }

    @Test
    fun sugarplumRationValidateBlockedWhenCafeContested() {
        registerCafeShop()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setString("crimbo23CafeControl", "contested")
        prefs.setInt(Crimbo23ShopSync.AVAILABLE_ELF_MPC_PREF, 20)

        assertFalse(canPurchase(SUGARPLUM_RATION, prefs))
    }

    @Test
    fun sugarplumRationValidateAllowedWithElfControlAndMpcPref() {
        registerCafeShop()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setString("crimbo23CafeControl", "elf")
        prefs.setInt(Crimbo23ShopSync.AVAILABLE_ELF_MPC_PREF, 20)

        assertTrue(canPurchase(SUGARPLUM_RATION, prefs))
    }

    private fun canPurchase(itemId: Int, prefs: Preferences): Boolean =
        CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
            itemId,
            CharacterState(meat = 100_000),
            prefs,
            accessibleCount = { tokenId ->
                CoinmasterSyncedTokenCount.accessibleCount(tokenId, prefs, physicalCount = 0)
            },
        )

    private fun registerConmerchShop() {
        CoinmasterDatabase.loadFromText(
            shopsText = "conmerch\tKoL Con 13 Merch Table\n",
            coinText = "KoL Con 13 Merch Table\tbuy\t1\tTwitching Television Tattoo\tROW895\n",
        )
    }

    private fun registerBarShop() {
        registerTestItem(MULLED_WINE, "mulled wine")
        registerTestItem(Crimbo23ShopSync.ELF_MPC, "Elf Guard MPC")
        CoinmasterDatabase.loadFromText(
            shopsText = "crimbo23_elf_bar\tElf Guard Officers' Club\n",
            coinText = "Elf Guard Officers' Club\tROW1406\tmulled wine\tElf Guard MPC (5)\n",
        )
    }

    private fun registerCafeShop() {
        registerTestItem(SUGARPLUM_RATION, "sugarplum ration")
        registerTestItem(Crimbo23ShopSync.ELF_MPC, "Elf Guard MPC")
        CoinmasterDatabase.loadFromText(
            shopsText = "crimbo23_elf_cafe\tElf Guard Mess Hall\n",
            coinText = "Elf Guard Mess Hall\tROW1400\tsugarplum ration\tElf Guard MPC (5)\n",
        )
    }

    private fun registerTestItem(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    companion object {
        private const val MULLED_WINE = 11465
        private const val SUGARPLUM_RATION = 11459
    }
}
