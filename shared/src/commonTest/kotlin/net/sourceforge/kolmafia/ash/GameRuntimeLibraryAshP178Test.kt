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

class GameRuntimeLibraryAshP178Test {

    @AfterTest
    fun cleanup() {
        CoinmasterVisitInventory.resetForTest()
        CoinmasterDatabase.resetForTest()
    }

    @Test
    fun revision_phase195() {
        assertEquals("phase210", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun trickCoinValidateBlockedWhenFoundryClosed() {
        registerElfFactoryShop()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setString("crimbo23FoundryControl", "none")
        prefs.setInt(Crimbo23ShopSync.AVAILABLE_ELF_MPC_PREF, 20)

        assertFalse(canPurchase(TRICK_COIN, prefs))
    }

    @Test
    fun trickCoinValidateBlockedWhenPiratesControlFoundry() {
        registerElfFactoryShop()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setString("crimbo23FoundryControl", "pirate")
        prefs.setInt(Crimbo23ShopSync.AVAILABLE_ELF_MPC_PREF, 20)

        assertFalse(canPurchase(TRICK_COIN, prefs))
    }

    @Test
    fun trickCoinValidateAllowedWithElfControlAndMpcPref() {
        registerElfFactoryShop()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setString("crimbo23FoundryControl", "elf")
        prefs.setInt(Crimbo23ShopSync.AVAILABLE_ELF_MPC_PREF, 20)

        assertTrue(canPurchase(TRICK_COIN, prefs))
    }

    @Test
    fun prankCrimboCardValidateBlockedWhenFoundryContested() {
        registerPirateFactoryShop()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setString("crimbo23FoundryControl", "contested")
        prefs.setInt(Crimbo23ShopSync.AVAILABLE_PIECE_OF_12_PREF, 20)

        assertFalse(canPurchase(PRANK_CRIMBO_CARD, prefs))
    }

    @Test
    fun prankCrimboCardValidateAllowedWithPirateControlAndPieceOf12Pref() {
        registerPirateFactoryShop()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        prefs.setString("crimbo23FoundryControl", "pirate")
        prefs.setInt(Crimbo23ShopSync.AVAILABLE_PIECE_OF_12_PREF, 20)

        assertTrue(canPurchase(PRANK_CRIMBO_CARD, prefs))
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

    private fun registerElfFactoryShop() {
        registerTestItem(TRICK_COIN, "trick coin")
        registerTestItem(Crimbo23ShopSync.ELF_MPC, "Elf Guard MPC")
        CoinmasterDatabase.loadFromText(
            shopsText = "crimbo23_elf_factory\tElf Guard Toy and Munitions Factory\n",
            coinText = "Elf Guard Toy and Munitions Factory\tROW1424\ttrick coin\tElf Guard MPC (10)\n",
        )
    }

    private fun registerPirateFactoryShop() {
        registerTestItem(PRANK_CRIMBO_CARD, "prank Crimbo card")
        registerTestItem(Crimbo23ShopSync.PIECE_OF_12, "Crimbuccaneer piece of 12")
        CoinmasterDatabase.loadFromText(
            shopsText = "crimbo23_pirate_factory\tCrimbuccaneer Foundry\n",
            coinText = "Crimbuccaneer Foundry\tROW1431\tprank Crimbo card\tCrimbuccaneer piece of 12 (10)\n",
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
        private const val TRICK_COIN = 11480
        private const val PRANK_CRIMBO_CARD = 11487
    }
}
