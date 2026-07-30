package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.StandardRewardDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.ArmoryAndLeggeryShopRows
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseProbe
import net.sourceforge.kolmafia.shop.CoinmasterVisitInventory
import net.sourceforge.kolmafia.shop.StandardRewardCurrencySync

class GameRuntimeLibraryAshP180Test {

    @AfterTest
    fun cleanup() {
        CoinmasterVisitInventory.resetForTest()
        CoinmasterDatabase.resetForTest()
        StandardRewardDatabase.resetForTest()
        EquipmentDatabase.resetForTest()
    }

    @Test
    fun revision_phase195() {
        assertEquals("phase249", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun pulverizedCurrencyGain_bumpsArmoryRefreshPref() {
        val prefs = Preferences(MapSettings())
        StandardRewardDatabase.loadFromText(SAMPLE_REWARDS, SAMPLE_PULVERIZED)

        StandardRewardCurrencySync.onInventoryDelta(
            before = emptyMap(),
            after = mapOf(CREPE_BITS to 2),
            prefs = prefs,
        )

        assertEquals(1, prefs.getInt(StandardRewardCurrencySync.REFRESH_PREF, 0))
    }

    @Test
    fun pulverizedCurrencyGain_noOpWhenQuantityUnchanged() {
        val prefs = Preferences(MapSettings())
        StandardRewardDatabase.loadFromText(SAMPLE_REWARDS, SAMPLE_PULVERIZED)

        StandardRewardCurrencySync.onInventoryDelta(
            before = mapOf(CREPE_BITS to 3),
            after = mapOf(CREPE_BITS to 3),
            prefs = prefs,
        )

        assertEquals(0, prefs.getInt(StandardRewardCurrencySync.REFRESH_PREF, 0))
    }

    @Test
    fun mossMaceValidateAllowedAfterDeriveWithPulverizedTokens() {
        registerStandardArmory()
        StandardRewardDatabase.derivePulverization()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)

        assertEquals(MOSS_MULCH, EquipmentDatabase.getPulverization(MOSS_MACE))
        assertTrue(canPurchase(MOSS_MACE, prefs) { if (it == MOSS_MULCH) 1 else 0 })
        assertFalse(canPurchase(MOSS_MACE, prefs) { 0 })
    }

    private fun canPurchase(
        itemId: Int,
        prefs: Preferences,
        accessibleCount: (Int) -> Int,
    ): Boolean =
        CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
            itemId,
            CharacterState(meat = 100_000),
            prefs,
            accessibleCount = accessibleCount,
        )

    private fun registerStandardArmory() {
        registerTestItem(MOSS_MACE, "moss mace")
        registerTestItem(MOSS_MULCH, "moss mulch")
        StandardRewardDatabase.loadFromText(SAMPLE_REWARDS, SAMPLE_PULVERIZED)
        CoinmasterDatabase.loadFromText(
            shopsText = "armory\tArmory and Leggery\n",
            coinText = "",
        )
        ArmoryAndLeggeryShopRows.rebuild()
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
        private const val MOSS_MACE = 11504
        private const val MOSS_MULCH = 11510
        private const val CREPE_BITS = 11526

        private val SAMPLE_REWARDS = """
            11504	2024	norm	SC	ROW1454	moss mace
        """.trimIndent()

        private val SAMPLE_PULVERIZED = """
            11510	2024	norm	moss mulch
            11526	2025	norm	crepe paper pared cuttings
        """.trimIndent()
    }
}
