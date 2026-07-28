package net.sourceforge.kolmafia.shop

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences

class CoinmasterPurchaseAccessibilityTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    private fun shoreMaster(): CoinmasterData =
        CoinmasterData(
            masterName = "The Shore, Inc. Gift Shop",
            nickname = "shore",
            token = "Shore Inc. Ship Trip Scrip",
            shopId = "shore",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )

    private fun replicaMaster(): CoinmasterData =
        CoinmasterData(
            masterName = "Replica Mr. Store",
            nickname = "mrreplica",
            token = "replica Mr. Accessory",
            shopId = "mrreplica",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )

    private fun mysticMaster(): CoinmasterData =
        CoinmasterData(
            masterName = "The Crackpot Mystic's Shed",
            nickname = "mystic",
            token = null,
            shopId = "mystic",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )

    private fun crimbo20FoodMaster(): CoinmasterData =
        CoinmasterData(
            masterName = "Elf Food Drive",
            nickname = "crimbo20food",
            token = "donated food",
            shopId = "crimbo20food",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )

    @Test
    fun shore_toasterBlockedAfterPurchasePref() {
        val p = prefs()
        p.setBoolean("itemBoughtPerAscension637", true)
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                shoreMaster(),
                637,
                CharacterState(level = 5),
                p,
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun shore_compassBlockedWhenOwned() {
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                shoreMaster(),
                6729,
                CharacterState(level = 5),
                prefs(),
                accessibleCount = { if (it == 6729) 1 else 0 },
            ),
        )
    }

    @Test
    fun crimbo20_buttonBlockedWhenOwned() {
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                crimbo20FoodMaster(),
                10691,
                CharacterState(),
                prefs(),
                accessibleCount = { if (it == 10691) 1 else 0 },
            ),
        )
    }

    @Test
    fun replica_currentYearItemAvailable() {
        val p = prefs()
        p.setInt("currentReplicaStoreYear", 2023)
        assertTrue(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                replicaMaster(),
                11325,
                CharacterState(challengePath = AscensionPath.LEGACY_OF_LOATHING.apiName),
                p,
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun replica_wrongYearItemBlocked() {
        val p = prefs()
        p.setInt("currentReplicaStoreYear", 2005)
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                replicaMaster(),
                11190,
                CharacterState(challengePath = AscensionPath.LEGACY_OF_LOATHING.apiName),
                p,
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun pixel_yellowSubmarineOnlyWhenBeachLocked() {
        val p = prefs()
        assertTrue(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                mysticMaster(),
                8376,
                CharacterState(level = 6, ascensionNumber = 5),
                p,
                accessibleCount = { 0 },
            ),
        )
        p.setInt("lastDesertUnlock", 5)
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                mysticMaster(),
                8376,
                CharacterState(level = 6, ascensionNumber = 5),
                p,
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun starchart_starShirtBlockedWithoutTorso() {
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                CoinmasterData(
                    masterName = "Star Chart",
                    nickname = "starchart",
                    token = "star chart",
                    shopId = "starchart",
                    buyItems = emptyList(),
                    sellItems = emptyList(),
                ),
                1133,
                CharacterState(level = 10),
                prefs(),
                accessibleCount = { 0 },
                hasSkill = { false },
            ),
        )
    }

    @Test
    fun fiveDPrinter_unknownRecipeBlocksItem() {
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                CoinmasterData(
                    masterName = "Xiblaxian 5D printer",
                    nickname = "5dprinter",
                    token = null,
                    shopId = "5dprinter",
                    buyItems = emptyList(),
                    sellItems = emptyList(),
                ),
                7752,
                CharacterState(),
                prefs(),
                accessibleCount = { 0 },
            ),
        )
    }
}
