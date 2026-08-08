package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.NpcStoreData
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterData
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseAccessibility
import net.sourceforge.kolmafia.shop.MysticShopSync
import net.sourceforge.kolmafia.shop.NpcPurchaseAccessibility

class GameRuntimeLibraryAshP148Test {

    private fun prefs(): Preferences = Preferences(MapSettings())

    private fun mysticMaster() = CoinmasterData(
        masterName = "The Crackpot Mystic's Shed",
        nickname = "mystic",
        token = null,
        shopId = "mystic",
        buyItems = emptyList(),
        sellItems = emptyList(),
    )

    private fun wildfireStore() = NpcStoreData(
        storeKey = "wildfire",
        storeName = "FDKOL Auxiliary",
        storeType = "NPC",
    )

    private fun bugbearStore() = NpcStoreData(
        storeKey = "bugbear",
        storeName = "Bugbear Bakery",
        storeType = "NPC",
    )

    @Test
    fun revision_phase176() {
        assertEquals("phase320", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun mystic_psychosisPixelBlockedUntilUnlocked() {
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                mysticMaster(),
                5906,
                CharacterState(challengePath = AscensionPath.STANDARD.apiName),
                prefs(),
                accessibleCount = { 0 },
            ),
        )
        val p = prefs()
        p.setBoolean(MysticShopSync.MYSTIC_PSYCHOSIS_ITEMS_UNLOCKED, true)
        assertTrue(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                mysticMaster(),
                5906,
                CharacterState(challengePath = AscensionPath.STANDARD.apiName),
                p,
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun wildfire_blartBlockedAfterBoughtPref() {
        val p = prefs()
        p.setBoolean("itemBoughtPerAscension10790", true)
        assertFalse(
            NpcPurchaseAccessibility.canPurchaseIgnoringMeat(
                itemId = 10790,
                store = wildfireStore(),
                state = CharacterState(challengePath = AscensionPath.WILDFIRE.apiName),
                prefs = p,
            ),
        )
    }

    @Test
    fun bugbear_requiresCostumePieces() {
        assertFalse(
            NpcPurchaseAccessibility.canPurchaseIgnoringMeat(
                itemId = 683,
                store = bugbearStore(),
                state = CharacterState(),
                prefs = prefs(),
                accessibleCount = { 0 },
            ),
        )
        assertTrue(
            NpcPurchaseAccessibility.canPurchaseIgnoringMeat(
                itemId = 683,
                store = bugbearStore(),
                state = CharacterState(),
                prefs = prefs(),
                accessibleCount = { if (it == 169 || it == 79) 1 else 0 },
            ),
        )
    }
}
