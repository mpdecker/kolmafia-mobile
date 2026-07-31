package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.StandardRewardDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.ArmoryAndLeggeryShopRows
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.StandardRewardRefresh

class GameRuntimeLibraryAshP181Test {

    @Test
    fun revision_phase198() {
        assertEquals("phase270", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun refreshArmoryRows_derivesPulverizationAndRebuildsBuyRows() {
        StandardRewardDatabase.loadFromText(SAMPLE_REWARDS, SAMPLE_PULVERIZED)
        CoinmasterDatabase.loadFromText(
            shopsText = "armory\tArmory and Leggery\n",
            coinText = "",
        )

        StandardRewardRefresh.refreshArmoryRows()

        assertEquals(MOSS_MULCH, EquipmentDatabase.getPulverization(MOSS_MACE))
        assertNotNull(CoinmasterDatabase.findBuyRowForItem(MOSS_MACE))
    }

    @Test
    fun refreshArmoryRows_canResetCurrencyRefreshPref() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_armoryAndLeggeryCurrencyRefresh", 3)

        StandardRewardRefresh.refreshArmoryRows(prefs, resetPref = true)

        assertEquals(0, prefs.getInt("_armoryAndLeggeryCurrencyRefresh", -1))
    }

    companion object {
        private const val MOSS_MACE = 11504
        private const val MOSS_MULCH = 11510

        private val SAMPLE_REWARDS = """
            11504	2024	norm	SC	ROW1454	moss mace
        """.trimIndent()

        private val SAMPLE_PULVERIZED = """
            11510	2024	norm	moss mulch
        """.trimIndent()
    }
}
