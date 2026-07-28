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
import net.sourceforge.kolmafia.data.StandardRewardDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.ArmoryAndLeggeryShopRows
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseProbe
import net.sourceforge.kolmafia.shop.CoinmasterVisitInventory

class GameRuntimeLibraryAshP176Test {

    @AfterTest
    fun cleanup() {
        CoinmasterVisitInventory.resetForTest()
        CoinmasterDatabase.resetForTest()
        StandardRewardDatabase.resetForTest()
    }

    @Test
    fun mossMaceValidateAllowedWithPulverizedTokens() {
        registerStandardArmory()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)

        assertTrue(canPurchase(MOSS_MACE, prefs) { if (it == CREPE_BITS) 1 else 0 })
    }

    @Test
    fun mossMaceValidateBlockedWithoutPulverizedTokens() {
        registerStandardArmory()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)

        assertFalse(canPurchase(MOSS_MACE, prefs) { 0 })
    }

    @Test
    fun adobeArsecoverValidateBlockedWithoutHardPulverizedTokens() {
        registerStandardArmory()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)

        assertFalse(canPurchase(ADOBE_ARSECOVER, prefs) { if (it == CREPE_BITS) 5 else 0 })
        assertTrue(canPurchase(ADOBE_ARSECOVER, prefs) { if (it == PETRIFIED_WOOD) 1 else 0 })
    }

    @Test
    fun phrygianCapValidateBlockedWithoutAngelboneFragments() {
        registerStandardArmory()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)

        assertFalse(canPurchase(PHRYGIAN_CAP, prefs) { 0 })
        assertTrue(canPurchase(PHRYGIAN_CAP, prefs) { if (it == ANGELBONE_FRAGMENTS) 1 else 0 })
    }

    @Test
    fun unknownStandardRewardBlockedFromValidate() {
        registerStandardArmory(
            rewardsText = """
                12068	2026	norm	SC	UNKNOWN	angelbone kilt
            """.trimIndent(),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)

        assertFalse(canPurchase(12068, prefs) { if (it == ANGELBONE_FRAGMENTS) 5 else 0 })
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

    private fun registerStandardArmory(
        rewardsText: String = SAMPLE_REWARDS,
        pulverizedText: String = SAMPLE_PULVERIZED,
    ) {
        registerTestItem(MOSS_MACE, "moss mace")
        registerTestItem(ADOBE_ARSECOVER, "adobe arsecover")
        registerTestItem(PHRYGIAN_CAP, "crepe paper phrygian cap")
        registerTestItem(CREPE_BITS, "crepe paper pared cuttings")
        registerTestItem(PETRIFIED_WOOD, "petrified wood waste parts")
        registerTestItem(ANGELBONE_FRAGMENTS, "angelbone fragments")
        StandardRewardDatabase.loadFromText(rewardsText, pulverizedText)
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
        private const val PHRYGIAN_CAP = 11520
        private const val CREPE_BITS = 11526
        private const val PETRIFIED_WOOD = 11534
        private const val ANGELBONE_FRAGMENTS = 12074

        private val SAMPLE_REWARDS = """
            11504	2024	norm	SC	ROW1454	moss mace
            11512	2024	hard	SC	ROW1460	adobe arsecover
            11520	2025	norm	SC	ROW1461	crepe paper phrygian cap
        """.trimIndent()

        private val SAMPLE_PULVERIZED = """
            11526	2025	norm	crepe paper pared cuttings
            11534	2025	hard	petrified wood waste parts
            12074	2026	norm	angelbone fragments
        """.trimIndent()
    }
}
