package net.sourceforge.kolmafia.shop

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences

class CoinmasterShopSyncTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    @Test
    fun fiveDPrinter_clearsUnknownRecipeOnDescitemVisit() {
        val p = prefs()
        p.setBoolean("unknownRecipe7752", true)
        ItemDatabase.registerForTest(
            ItemData(
                id = 7752,
                name = "Xiblaxian xenogoggles",
                descId = "147449485",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = emptySet(),
                autosellPrice = 0,
                plural = null,
            ),
        )
        CoinmasterShopSync.apply(
            html = """<a onclick="descitem(147449485);">goggles</a>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=5dprinter",
            prefs = p,
        )
        assertFalse(p.getBoolean("unknownRecipe7752", true))
    }

    @Test
    fun bacon_visitSyncSetsBoughtPrefsFromMissingRows() {
        val p = prefs()
        CoinmasterShopSync.apply(
            html = "<html></html>",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=bacon",
            prefs = p,
        )
        assertTrue(p.getBoolean("_internetViralVideoBought", false))
        assertTrue(p.getBoolean("_internetPlusOneBought", false))
    }

    @Test
    fun arcade_visitUnlocksLockedItems() {
        val p = prefs()
        p.setBoolean("lockedItem4637", true)
        CoinmasterShopSync.apply(
            html = """<tr rel="4637"><td>mask</td></tr>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=arcade",
            prefs = p,
        )
        assertFalse(p.getBoolean("lockedItem4637", true))
    }

    @Test
    fun kiwi_visitMarksSpiritsBoughtWhenMissing() {
        val p = prefs()
        CoinmasterShopSync.apply(
            html = "<html></html>",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=kiwi",
            prefs = p,
        )
        assertTrue(p.getBoolean("_miniKiwiIntoxicatingSpiritsBought", false))
    }

    @Test
    fun mystic_visitUnlocksPsychosisItems() {
        val p = prefs()
        CoinmasterShopSync.apply(
            html = """<tr rel="5906"><b>pixel pill</b></tr>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=mystic",
            prefs = p,
        )
        assertTrue(p.getBoolean(CoinmasterShopSync.MYSTIC_PSYCHOSIS_ITEMS_UNLOCKED, false))
    }

    @Test
    fun purchasedItem_setsBaconPref() {
        val p = prefs()
        val master = CoinmasterData(
            masterName = "Internet Meme Shop",
            nickname = "bacon",
            token = "BACON",
            shopId = "bacon",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )
        CoinmasterShopSync.applyPurchasedItem(master, 9017, p)
        assertTrue(p.getBoolean("_internetViralVideoBought", false))
    }

    @Test
    fun shore_visitMarksToasterBoughtWhenMissing() {
        val p = prefs()
        CoinmasterShopSync.apply(
            html = "<html></html>",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=shore",
            prefs = p,
        )
        assertTrue(p.getBoolean("itemBoughtPerAscension637", false))
    }

    @Test
    fun shore_visitLeavesToasterAvailableWhenPresent() {
        val p = prefs()
        CoinmasterShopSync.apply(
            html = """<b>cheap toaster</b>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=shore",
            prefs = p,
        )
        assertFalse(p.getBoolean("itemBoughtPerAscension637", false))
    }

    @Test
    fun purchasedItem_setsShoreToasterPref() {
        val p = prefs()
        val master = CoinmasterData(
            masterName = "The Shore, Inc. Gift Shop",
            nickname = "shore",
            token = "scrip",
            shopId = "shore",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )
        CoinmasterShopSync.applyPurchasedItem(master, 637, p)
        assertTrue(p.getBoolean("itemBoughtPerAscension637", false))
    }

    @Test
    fun purchasedItem_setsDvFlaskPref() {
        val p = prefs()
        val master = CoinmasterData(
            masterName = "The Terrified Eagle Inn",
            nickname = "dv",
            token = "1000000",
            shopId = "dv",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )
        CoinmasterShopSync.applyPurchasedItem(master, 6423, p)
        assertTrue(p.getBoolean("itemBoughtPerCharacter6423", false))
    }

    @Test
    fun swagger_visitSetsSeasonPrefs() {
        val p = prefs()
        val html = """
            You've earned 600 swagger during a pirate season, yarrr.
            <tr><td><img onclick='descitem(364177657)'></td><td><b>Black Bart's Booty</b></td>
            <td><form><input type="hidden" name="whichitem" value="7732" />
            <input type="submit" value="Buy (1000 swagger)" /></form></td></tr>
        """.trimIndent()
        CoinmasterShopSync.applySwaggerVisit(
            html = html,
            url = "https://www.kingdomofloathing.com/peevpee.php?place=shop",
            prefs = p,
        )
        assertEquals("pirate", p.getString("currentPVPSeason", ""))
        assertEquals(600, p.getInt("pirateSwagger", 0))
        assertTrue(p.getBoolean("blackBartsBootyAvailable", false))
        assertEquals(1000, p.getInt("blackBartsBootyCost", 0))
    }

    @Test
    fun swagger_skipsBuyActionUrl() {
        val p = prefs()
        CoinmasterShopSync.applySwaggerVisit(
            html = "You've earned 600 swagger during a pirate season",
            url = "https://www.kingdomofloathing.com/peevpee.php?place=shop&action=buy",
            prefs = p,
        )
        assertEquals("", p.getString("currentPVPSeason", ""))
    }

    @Test
    fun purchasedItem_setsJarlCosmicSixPackPref() {
        val p = prefs()
        val master = CoinmasterData(
            masterName = "Jarlsberg's Cosmic Kitchen",
            nickname = "jarl",
            token = null,
            shopId = "jarl",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )
        CoinmasterShopSync.applyPurchasedItem(master, 6237, p)
        assertTrue(p.getBoolean("_cosmicSixPackConjured", false))
    }

    @Test
    fun mrreplica_visitSetsCurrentYearPref() {
        val p = prefs()
        CoinmasterShopSync.apply(
            html = """<td colspan=14 align=center>&mdash; <b>2023</b> &mdash;</td>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=mrreplica",
            prefs = p,
        )
        assertEquals(2023, p.getInt("currentReplicaStoreYear", 0))
    }

    @Test
    fun blackmarket_visitUnlocksMacguffinStep1() {
        val p = prefs()
        CoinmasterShopSync.apply(
            html = "<html>Black Market</html>",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=blackmarket",
            prefs = p,
            state = net.sourceforge.kolmafia.character.CharacterState(ascensionNumber = 5),
        )
        assertEquals(
            "step1",
            p.getString(net.sourceforge.kolmafia.quest.Quest.MACGUFFIN.prefKey, ""),
        )
    }

    @Test
    fun blackmarket_visitSkipsWhenAlreadyUnlocked() {
        val p = prefs()
        p.setString(net.sourceforge.kolmafia.quest.Quest.MACGUFFIN.prefKey, "step2")
        CoinmasterShopSync.apply(
            html = "<html>Black Market</html>",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=blackmarket",
            prefs = p,
            state = net.sourceforge.kolmafia.character.CharacterState(ascensionNumber = 5),
        )
        assertEquals("step2", p.getString(net.sourceforge.kolmafia.quest.Quest.MACGUFFIN.prefKey, ""))
    }

    @Test
    fun piraterealm_visitUnlocksCrabsicleAndFunPoints() {
        val p = prefs()
        CoinmasterShopSync.apply(
            html = """
                <b>You have 500 FunPoints.</b>
                <tr rel="10199"><td>crabsicle</td></tr>
            """.trimIndent(),
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=piraterealm",
            prefs = p,
        )
        assertTrue(p.getBoolean("pirateRealmUnlockedCrabsicle", false))
        assertEquals(500, p.getInt("availableFunPoints", 0))
    }

    @Test
    fun piraterealm_visitClearsUnlockWhenRowMissing() {
        val p = prefs()
        p.setBoolean("pirateRealmUnlockedCrabsicle", true)
        CoinmasterShopSync.apply(
            html = "<b>You have 0 FunPoints.</b>",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=piraterealm",
            prefs = p,
        )
        assertFalse(p.getBoolean("pirateRealmUnlockedCrabsicle", true))
    }

    @Test
    fun piraterealm_skipsBuyActionUrl() {
        val p = prefs()
        p.setBoolean("pirateRealmUnlockedCrabsicle", false)
        CoinmasterShopSync.apply(
            html = """<tr rel="10199"><td>crabsicle</td></tr>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=piraterealm&action=buy",
            prefs = p,
        )
        assertFalse(p.getBoolean("pirateRealmUnlockedCrabsicle", false))
    }

    @Test
    fun driparmory_visitUnlocksShieldPref() {
        val p = prefs()
        CoinmasterShopSync.apply(
            html = """<b>drippy shield</b>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=driparmory",
            prefs = p,
        )
        assertTrue(p.getBoolean("drippyShieldUnlocked", false))
    }

    @Test
    fun driparmory_visitLeavesPrefFalseWhenShieldMissing() {
        val p = prefs()
        CoinmasterShopSync.apply(
            html = """<b>drippy khakis</b>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=driparmory",
            prefs = p,
        )
        assertFalse(p.getBoolean("drippyShieldUnlocked", false))
    }

    @Test
    fun driparmory_skipsBuyActionUrl() {
        val p = prefs()
        CoinmasterShopSync.apply(
            html = """<b>drippy shield</b>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=driparmory&action=buy",
            prefs = p,
        )
        assertFalse(p.getBoolean("drippyShieldUnlocked", false))
    }
}
