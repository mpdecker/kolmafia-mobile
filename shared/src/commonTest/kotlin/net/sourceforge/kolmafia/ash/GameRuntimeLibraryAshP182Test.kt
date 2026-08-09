package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.StandardRewardDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.ArmoryAndLeggerySync
import net.sourceforge.kolmafia.shop.ArmoryAndLeggeryShopRows
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseProbe
import net.sourceforge.kolmafia.shop.CoinmasterVisitInventory
import net.sourceforge.kolmafia.shop.StandardRewardCurrencySync

class GameRuntimeLibraryAshP182Test {

    @AfterTest
    fun cleanup() {
        CoinmasterVisitInventory.resetForTest()
        CoinmasterDatabase.resetForTest()
        StandardRewardDatabase.resetForTest()
        EquipmentDatabase.resetForTest()
    }

    @Test
    fun revision_phase195() {
        assertEquals("phase350", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visitLearnUnknownRow_validateAllowedWithPetrifiedWood() {
        registerStandardArmory()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)

        ArmoryAndLeggerySync.syncFromShopHtml(WAR_PIKE_HTML, prefs, force = true)

        val reward = StandardRewardDatabase.findStandardReward(WAR_PIKE)
        assertNotNull(reward)
        assertEquals("1700", reward.row)
        assertTrue(canPurchase(WAR_PIKE, prefs) { if (it == PETRIFIED_WOOD) 1 else 0 })
    }

    @Test
    fun currencyGain_refreshDerivesPulverizationAndKeepsProbe() {
        registerStandardArmory()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)

        StandardRewardCurrencySync.onInventoryDelta(
            before = emptyMap(),
            after = mapOf(MOSS_MULCH to 1),
            prefs = prefs,
        )

        assertEquals(1, prefs.getInt(StandardRewardCurrencySync.REFRESH_PREF, 0))
        assertEquals(MOSS_MULCH, EquipmentDatabase.getPulverization(MOSS_MACE))
        assertTrue(canPurchase(MOSS_MACE, prefs) { if (it == MOSS_MULCH) 1 else 0 })
    }

    @Test
    fun adobeArsecover_hardTokenGate() {
        registerStandardArmory()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        StandardRewardDatabase.derivePulverization()

        assertFalse(canPurchase(ADOBE_ARSECOVER, prefs) { if (it == MOSS_MULCH) 5 else 0 })
        assertTrue(canPurchase(ADOBE_ARSECOVER, prefs) { if (it == ADOBE_ASSORTMENT) 1 else 0 })
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
        registerTestItem(ADOBE_ARSECOVER, "adobe arsecover")
        registerTestItem(WAR_PIKE, "petrified wood war pike")
        registerTestItem(MOSS_MULCH, "moss mulch")
        registerTestItem(ADOBE_ASSORTMENT, "adobe assortment")
        registerTestItem(CREPE_BITS, "crepe paper pared cuttings")
        registerTestItem(PETRIFIED_WOOD, "petrified wood waste parts")
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
        private const val ADOBE_ARSECOVER = 11512
        private const val WAR_PIKE = 11528
        private const val MOSS_MULCH = 11510
        private const val ADOBE_ASSORTMENT = 11518
        private const val CREPE_BITS = 11526
        private const val PETRIFIED_WOOD = 11534

        private val SAMPLE_REWARDS = """
            11504	2024	norm	SC	ROW1454	moss mace
            11512	2024	hard	SC	ROW1460	adobe arsecover
            11528	2025	hard	SC	UNKNOWN	petrified wood war pike
        """.trimIndent()

        private val SAMPLE_PULVERIZED = """
            11510	2024	norm	moss mulch
            11518	2024	hard	adobe assortment
            11526	2025	norm	crepe paper pared cuttings
            11534	2025	hard	petrified wood waste parts
        """.trimIndent()

        private val WAR_PIKE_HTML = """
            <tr rel="11528">
            <a onClick='javascript:descitem(11528)'><b>petrified wood war pike</b></a>
            <span title="petrified wood waste parts"><b>1</b></span>
            <form action="shop.php?action=buy&whichshop=armory&whichrow=1700">
            </tr>
        """.trimIndent()
    }
}
