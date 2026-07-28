package net.sourceforge.kolmafia.shop

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterShopSync

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
    fun shore_toasterBlockedAfterVisitSync() {
        val p = prefs()
        p.setBoolean("autoSatisfyWithCoinmasters", true)
        CoinmasterShopSync.apply(
            html = "<html></html>",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=shore",
            prefs = p,
        )
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                shoreMaster(),
                637,
                CharacterState(level = 6),
                p,
                accessibleCount = { 100 },
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
    fun arcade_lockedItemBlockedWhenPrefSet() {
        val p = prefs()
        p.setBoolean("lockedItem4637", true)
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                CoinmasterData(
                    masterName = "Arcade Ticket Counter",
                    nickname = "arcade",
                    token = "ticket",
                    shopId = "arcade",
                    buyItems = emptyList(),
                    sellItems = emptyList(),
                ),
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
                CoinmasterData(
                    masterName = "Arcade Ticket Counter",
                    nickname = "arcade",
                    token = "ticket",
                    shopId = "arcade",
                    buyItems = emptyList(),
                    sellItems = emptyList(),
                ),
                4637,
                CharacterState(),
                prefs(),
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun mystic_psychosisPixelBlockedUntilUnlocked() {
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                mysticMaster(),
                5906,
                CharacterState(),
                prefs(),
                accessibleCount = { 0 },
            ),
        )
        val p = prefs()
        p.setBoolean(CoinmasterShopSync.MYSTIC_PSYCHOSIS_ITEMS_UNLOCKED, true)
        assertTrue(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                mysticMaster(),
                5906,
                CharacterState(),
                p,
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun bacon_viralVideoOneTimePref() {
        val p = prefs()
        p.setBoolean("_internetViralVideoBought", true)
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                CoinmasterData(
                    masterName = "Internet Meme Shop",
                    nickname = "bacon",
                    token = "BACON",
                    shopId = "bacon",
                    buyItems = emptyList(),
                    sellItems = emptyList(),
                ),
                9017,
                CharacterState(),
                p,
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun fixodent_dentadentRequiresMonodentEquipped() {
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                CoinmasterData(
                    masterName = "Craft with Teeth",
                    nickname = "fixodent",
                    token = null,
                    shopId = "fixodent",
                    buyItems = emptyList(),
                    sellItems = emptyList(),
                ),
                11977,
                CharacterState(),
                prefs(),
                accessibleCount = { 0 },
            ),
        )
    }

    private fun swaggerMaster(): CoinmasterData =
        CoinmasterData(
            masterName = "The Swagger Shop",
            nickname = "swagger",
            token = "swagger",
            shopId = "swagger",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )

    private fun jarlMaster(): CoinmasterData =
        CoinmasterData(
            masterName = "Jarlsberg's Cosmic Kitchen",
            nickname = "jarl",
            token = null,
            shopId = "jarl",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )

    @Test
    fun swagger_blackBartsBootyBlockedUntilVisitSync() {
        val p = prefs()
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                swaggerMaster(),
                7732,
                CharacterState(),
                p,
                accessibleCount = { 0 },
            ),
        )
        CoinmasterShopSync.applySwaggerVisit(
            html = """
                <tr><td><b>Black Bart's Booty</b></td>
                <td><form><input type="hidden" name="whichitem" value="7732" />
                <input type="submit" value="Buy (1000 swagger)" /></form></td></tr>
            """.trimIndent(),
            url = "https://www.kingdomofloathing.com/peevpee.php?place=shop",
            prefs = p,
        )
        assertTrue(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                swaggerMaster(),
                7732,
                CharacterState(),
                p,
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun jarl_cosmicSixPackBlockedAfterPurchaseSync() {
        val p = prefs()
        CoinmasterShopSync.applyPurchasedItem(jarlMaster(), 6237, p)
        assertFalse(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                jarlMaster(),
                6237,
                CharacterState(challengePath = AscensionPath.AVATAR_OF_JARLSBERG.apiName),
                p,
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun replica_wrongYearBlockedAfterVisitSync() {
        val p = prefs()
        CoinmasterShopSync.apply(
            html = """<td colspan=14 align=center>&mdash; <b>2023</b> &mdash;</td>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=mrreplica",
            prefs = p,
        )
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
}
