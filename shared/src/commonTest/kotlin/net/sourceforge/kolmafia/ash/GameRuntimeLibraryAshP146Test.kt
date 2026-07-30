package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterData
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseAccessibility
import net.sourceforge.kolmafia.shop.FolderHolderAccessibility

class GameRuntimeLibraryAshP146Test {

    private fun prefs(): Preferences = Preferences(MapSettings())

    private fun baconMaster() = CoinmasterData(
        masterName = "Internet Meme Shop",
        nickname = "bacon",
        token = "BACON",
        shopId = "bacon",
        buyItems = emptyList(),
        sellItems = emptyList(),
    )

    private fun arcadeMaster() = CoinmasterData(
        masterName = "Arcade Ticket Counter",
        nickname = "arcade",
        token = "ticket",
        shopId = "arcade",
        buyItems = emptyList(),
        sellItems = emptyList(),
    )

    private fun dvMaster() = CoinmasterData(
        masterName = "The Terrified Eagle Inn",
        nickname = "dv",
        token = "Freddy Kruegerand",
        shopId = "dv",
        buyItems = emptyList(),
        sellItems = emptyList(),
    )

    private fun kiwiMaster() = CoinmasterData(
        masterName = "Kiwi Kwiki Mart",
        nickname = "kiwi",
        token = null,
        shopId = "kiwi",
        buyItems = emptyList(),
        sellItems = emptyList(),
    )

    private fun fixodentMaster() = CoinmasterData(
        masterName = "Craft with Teeth",
        nickname = "fixodent",
        token = null,
        shopId = "fixodent",
        buyItems = emptyList(),
        sellItems = emptyList(),
    )

    @Test
    fun revision_phase176() {
        assertEquals("phase249", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun bacon_oneTimeItemBlockedAfterPref() {
        val p = prefs()
        p.setBoolean("_internetViralVideoBought", true)
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                baconMaster(),
                9017,
                CharacterState(),
                p,
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun arcade_lockedItemBlockedWhenPrefSet() {
        val p = prefs()
        p.setBoolean("lockedItem4637", true)
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                arcadeMaster(),
                4637,
                CharacterState(),
                p,
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun arcade_lockedItemBlockedByDefault() {
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                arcadeMaster(),
                4637,
                CharacterState(),
                prefs(),
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun arcade_lockedItemAllowedAfterUnlockPref() {
        val p = prefs()
        p.setBoolean("lockedItem4637", false)
        assertTrue(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                arcadeMaster(),
                4637,
                CharacterState(),
                p,
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun arcade_folderRequiresHolder() {
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                arcadeMaster(),
                6631,
                CharacterState(),
                prefs(),
                accessibleCount = { 0 },
            ),
        )
        assertTrue(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                arcadeMaster(),
                6631,
                CharacterState(),
                prefs(),
                accessibleCount = { if (it == FolderHolderAccessibility.FOLDER_HOLDER) 1 else 0 },
            ),
        )
    }

    @Test
    fun dv_talesOfDreadOneTime() {
        val p = prefs()
        p.setBoolean("itemBoughtPerCharacter6423", true)
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                dvMaster(),
                6423,
                CharacterState(),
                p,
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun kiwi_spiritsOneTime() {
        val p = prefs()
        p.setBoolean("_miniKiwiIntoxicatingSpiritsBought", true)
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                kiwiMaster(),
                11602,
                CharacterState(),
                p,
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun fixodent_dentadentRequiresMonodent() {
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                fixodentMaster(),
                11977,
                CharacterState(),
                prefs(),
                accessibleCount = { 0 },
            ),
        )
        assertTrue(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                fixodentMaster(),
                11977,
                CharacterState(),
                prefs(),
                accessibleCount = { if (it == 11975) 1 else 0 },
            ),
        )
    }
}
