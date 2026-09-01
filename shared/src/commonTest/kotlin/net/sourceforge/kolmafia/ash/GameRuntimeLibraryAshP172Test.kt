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
import net.sourceforge.kolmafia.shop.CoinmasterVisitInventory
import net.sourceforge.kolmafia.shop.Crimbo23ShopAccessibility

class GameRuntimeLibraryAshP172Test {

    @AfterTest
    fun cleanup() {
        CoinmasterVisitInventory.resetForTest()
        CoinmasterDatabase.resetForTest()
    }

    @Test
    fun revision_phase189() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun kelflarVestValidateBlockedWhenCrimboTownClosed() {
        registerArmoryShops()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)

        assertFalse(canBuyKelflar(prefs))
    }

    @Test
    fun kelflarVestValidateBlockedWhenContested() {
        registerArmoryShops()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setString("crimbo23ArmoryControl", "contested")

        assertFalse(canBuyKelflar(prefs))
    }

    @Test
    fun kelflarVestValidateBlockedWhenPiratesControlArmory() {
        registerArmoryShops()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setString("crimbo23ArmoryControl", "pirate")

        assertFalse(canBuyKelflar(prefs))
    }

    @Test
    fun kelflarVestValidateAllowedWhenElvesControlArmoryWithParts() {
        registerArmoryShops()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setString("crimbo23ArmoryControl", "elf")

        assertTrue(canBuyKelflar(prefs))
    }

    @Test
    fun crimbuccaneerShirtValidateBlockedWhenElvesControlArmory() {
        registerArmoryShops()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setString("crimbo23ArmoryControl", "elf")

        assertFalse(canBuyShirt(prefs))
    }

    @Test
    fun crimbuccaneerShirtValidateAllowedWhenPiratesControlArmoryWithFlotsam() {
        registerArmoryShops()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setString("crimbo23ArmoryControl", "pirate")

        assertTrue(canBuyShirt(prefs))
    }

    private fun canBuyKelflar(prefs: Preferences): Boolean =
        CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
            Crimbo23ShopAccessibility.KELFLAR_VEST,
            CharacterState(meat = 100_000),
            prefs,
            accessibleCount = { if (it == Crimbo23ShopAccessibility.ELF_ARMY_MACHINE_PARTS) 10 else 0 },
        )

    private fun canBuyShirt(prefs: Preferences): Boolean =
        CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
            Crimbo23ShopAccessibility.CRIMBUCCANEER_SHIRT,
            CharacterState(meat = 100_000),
            prefs,
            accessibleCount = { if (it == Crimbo23ShopAccessibility.CRIMBUCCANEER_FLOTSAM) 10 else 0 },
        )

    private fun registerArmoryShops() {
        registerTestItem(Crimbo23ShopAccessibility.KELFLAR_VEST, "Kelflar vest")
        registerTestItem(Crimbo23ShopAccessibility.CRIMBUCCANEER_SHIRT, "Crimbuccaneer shirt")
        registerTestItem(Crimbo23ShopAccessibility.ELF_ARMY_MACHINE_PARTS, "Elf Army machine parts")
        registerTestItem(Crimbo23ShopAccessibility.CRIMBUCCANEER_FLOTSAM, "Crimbuccaneer flotsam")
        CoinmasterDatabase.loadFromText(
            shopsText = """
                crimbo23_elf_armory	Elf Guard Armory
                crimbo23_pirate_armory	Crimbuccaneer Junkworks
            """.trimIndent(),
            coinText = """
                Elf Guard Armory	ROW1415	Kelflar vest	Elf Army machine parts (3)
                Crimbuccaneer Junkworks	ROW1418	Crimbuccaneer shirt	Crimbuccaneer flotsam (3)
            """.trimIndent(),
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
}
