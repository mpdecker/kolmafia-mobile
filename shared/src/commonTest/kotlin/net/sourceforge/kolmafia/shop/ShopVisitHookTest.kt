package net.sourceforge.kolmafia.shop

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences

class ShopVisitHookTest {

    @AfterTest
    fun cleanup() {
        CoinmasterDatabase.resetForTest()
    }

    private fun prefs(): Preferences = Preferences(MapSettings())

    private fun applyVisitShopHook(
        html: String,
        url: String,
        prefs: Preferences,
        shopsText: String,
        coinText: String,
        state: CharacterState? = null,
    ) {
        CoinmasterDatabase.loadFromText(shopsText = shopsText, coinText = coinText)
        ShopInventorySync.parseAndLearn(
            html = html,
            url = url,
            prefs = prefs,
            state = state,
        )
    }

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
        applyVisitShopHook(
            html = """<a onclick="descitem(147449485);">goggles</a>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=5dprinter",
            prefs = p,
            shopsText = "5dprinter\tXiblaxian 5D printer\n",
            coinText = "Xiblaxian 5D printer\tbuy\t1\tgoggles\tROW1\n",
        )
        assertFalse(p.getBoolean("unknownRecipe7752", true))
    }

    @Test
    fun bacon_visitSyncSetsBoughtPrefsFromMissingRows() {
        val p = prefs()
        applyVisitShopHook(
            html = "<html></html>",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=bacon",
            prefs = p,
            shopsText = "bacon\tInternet Meme Shop\n",
            coinText = "Internet Meme Shop\tbuy\t1\tviral video\tROW1\n",
        )
        assertTrue(p.getBoolean("_internetViralVideoBought", false))
        assertTrue(p.getBoolean("_internetPlusOneBought", false))
    }

    @Test
    fun arcade_visitUnlocksLockedItems() {
        val p = prefs()
        p.setBoolean("lockedItem4637", true)
        applyVisitShopHook(
            html = """<tr rel="4637"><td>mask</td></tr>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=arcade",
            prefs = p,
            shopsText = "arcade\tArcade Ticket Counter\n",
            coinText = "Arcade Ticket Counter\tbuy\t1\tmask\tROW1\n",
        )
        assertFalse(p.getBoolean("lockedItem4637", true))
    }

    @Test
    fun kiwi_visitMarksSpiritsBoughtWhenMissing() {
        val p = prefs()
        applyVisitShopHook(
            html = "<html></html>",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=kiwi",
            prefs = p,
            shopsText = "kiwi\tKiwi Kwiki Mart\n",
            coinText = "Kiwi Kwiki Mart\tbuy\t1\tspirits\tROW1\n",
        )
        assertTrue(p.getBoolean("_miniKiwiIntoxicatingSpiritsBought", false))
    }

    @Test
    fun mystic_visitUnlocksPsychosisItems() {
        val p = prefs()
        applyVisitShopHook(
            html = """<tr rel="5906"><b>pixel pill</b></tr>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=mystic",
            prefs = p,
            shopsText = "mystic\tThe Crackpot Mystic's Shed\n",
            coinText = "The Crackpot Mystic's Shed\tROW39\tpixel pill\tred pixel (20)\n",
        )
        assertTrue(p.getBoolean(MysticShopSync.MYSTIC_PSYCHOSIS_ITEMS_UNLOCKED, false))
    }

    @Test
    fun shore_visitMarksToasterBoughtWhenMissing() {
        val p = prefs()
        applyVisitShopHook(
            html = "<html></html>",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=shore",
            prefs = p,
            shopsText = "shore\tThe Shore, Inc. Gift Shop\n",
            coinText = "The Shore, Inc. Gift Shop\tROW637\tcheap toaster\tShore Inc. Ship Trip Scrip (20)\n",
        )
        assertTrue(p.getBoolean("itemBoughtPerAscension637", false))
    }

    @Test
    fun shore_visitLeavesToasterAvailableWhenPresent() {
        val p = prefs()
        applyVisitShopHook(
            html = """<b>cheap toaster</b>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=shore",
            prefs = p,
            shopsText = "shore\tThe Shore, Inc. Gift Shop\n",
            coinText = "The Shore, Inc. Gift Shop\tROW637\tcheap toaster\tShore Inc. Ship Trip Scrip (20)\n",
        )
        assertFalse(p.getBoolean("itemBoughtPerAscension637", false))
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
        SwaggerShopSync.applyVisitShop(
            html = html,
            url = "https://www.kingdomofloathing.com/peevpee.php?place=shop",
            prefs = p,
            sessionLogger = null,
            state = null,
        )
        assertEquals("pirate", p.getString("currentPVPSeason", ""))
        assertEquals(600, p.getInt("pirateSwagger", 0))
        assertTrue(p.getBoolean("blackBartsBootyAvailable", false))
        assertEquals(1000, p.getInt("blackBartsBootyCost", 0))
    }

    @Test
    fun swagger_skipsBuyActionUrl() {
        val p = prefs()
        SwaggerShopSync.applyVisitShop(
            html = "You've earned 600 swagger during a pirate season",
            url = "https://www.kingdomofloathing.com/peevpee.php?place=shop&action=buy",
            prefs = p,
            sessionLogger = null,
            state = null,
        )
        assertEquals("", p.getString("currentPVPSeason", ""))
    }

    @Test
    fun mrreplica_visitSetsCurrentYearPref() {
        val p = prefs()
        applyVisitShopHook(
            html = """<td colspan=14 align=center>&mdash; <b>2023</b> &mdash;</td>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=mrreplica",
            prefs = p,
            shopsText = "mrreplica\tReplica Mr. Store\n",
            coinText = "Replica Mr. Store\tbuy\t1\taugust scepter\tROW11325\n",
        )
        assertEquals(2023, p.getInt("currentReplicaStoreYear", 0))
    }

    @Test
    fun blackmarket_visitUnlocksMacguffinStep1() {
        val p = prefs()
        applyVisitShopHook(
            html = "<html>Black Market</html>",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=blackmarket",
            prefs = p,
            shopsText = "blackmarket\tThe Black Market\n",
            coinText = "The Black Market\tROW290\tRed Zeppelin ticket\tpriceless diamond (1)\n",
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
        applyVisitShopHook(
            html = "<html>Black Market</html>",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=blackmarket",
            prefs = p,
            shopsText = "blackmarket\tThe Black Market\n",
            coinText = "The Black Market\tROW290\tRed Zeppelin ticket\tpriceless diamond (1)\n",
            state = net.sourceforge.kolmafia.character.CharacterState(ascensionNumber = 5),
        )
        assertEquals("step2", p.getString(net.sourceforge.kolmafia.quest.Quest.MACGUFFIN.prefKey, ""))
    }

    @Test
    fun piraterealm_visitUnlocksCrabsicleAndFunPoints() {
        val p = prefs()
        applyVisitShopHook(
            html = """
                <b>You have 500 FunPoints.</b>
                <tr rel="10199"><td>crabsicle</td></tr>
            """.trimIndent(),
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=piraterealm",
            prefs = p,
            shopsText = "piraterealm\tPirateRealm Fun-a-Log\n",
            coinText = "PirateRealm Fun-a-Log\tbuy\t100\tcrabsicle\tROW1053\n",
        )
        assertTrue(p.getBoolean("pirateRealmUnlockedCrabsicle", false))
        assertEquals(500, p.getInt("availableFunPoints", 0))
    }

    @Test
    fun piraterealm_visitClearsUnlockWhenRowMissing() {
        val p = prefs()
        p.setBoolean("pirateRealmUnlockedCrabsicle", true)
        applyVisitShopHook(
            html = "<b>You have 0 FunPoints.</b>",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=piraterealm",
            prefs = p,
            shopsText = "piraterealm\tPirateRealm Fun-a-Log\n",
            coinText = "PirateRealm Fun-a-Log\tbuy\t100\tcrabsicle\tROW1053\n",
        )
        assertFalse(p.getBoolean("pirateRealmUnlockedCrabsicle", true))
    }

    @Test
    fun piraterealm_skipsBuyActionUrl() {
        val p = prefs()
        p.setBoolean("pirateRealmUnlockedCrabsicle", false)
        applyVisitShopHook(
            html = """<tr rel="10199"><td>crabsicle</td></tr>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=piraterealm&action=buy",
            prefs = p,
            shopsText = "piraterealm\tPirateRealm Fun-a-Log\n",
            coinText = "PirateRealm Fun-a-Log\tbuy\t100\tcrabsicle\tROW1053\n",
        )
        assertFalse(p.getBoolean("pirateRealmUnlockedCrabsicle", false))
    }

    @Test
    fun driparmory_visitUnlocksShieldPref() {
        val p = prefs()
        applyVisitShopHook(
            html = """<b>drippy shield</b>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=driparmory",
            prefs = p,
            shopsText = "driparmory\tDrip Institute Armory\n",
            coinText = "Drip Institute Armory\tbuy\t50\tdrippy shield\tROW1132\n",
        )
        assertTrue(p.getBoolean("drippyShieldUnlocked", false))
    }

    @Test
    fun driparmory_visitLeavesPrefFalseWhenShieldMissing() {
        val p = prefs()
        applyVisitShopHook(
            html = """<b>drippy khakis</b>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=driparmory",
            prefs = p,
            shopsText = "driparmory\tDrip Institute Armory\n",
            coinText = "Drip Institute Armory\tbuy\t50\tdrippy shield\tROW1132\n",
        )
        assertFalse(p.getBoolean("drippyShieldUnlocked", false))
    }

    @Test
    fun driparmory_skipsBuyActionUrl() {
        val p = prefs()
        applyVisitShopHook(
            html = """<b>drippy shield</b>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=driparmory&action=buy",
            prefs = p,
            shopsText = "driparmory\tDrip Institute Armory\n",
            coinText = "Drip Institute Armory\tbuy\t50\tdrippy shield\tROW1132\n",
        )
        assertFalse(p.getBoolean("drippyShieldUnlocked", false))
    }

    @Test
    fun chronerShop_visitSetsTimeTowerAvailable() {
        val p = prefs()
        applyVisitShopHook(
            html = """<b>flak shield</b> Chroner (20)""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=twitch_alliedhq",
            prefs = p,
            shopsText = "twitch_alliedhq\tAllied HQ\n",
            coinText = "Allied HQ\tROW1599\tflak shield\tChroner (20)\n",
        )
        assertTrue(p.getBoolean("timeTowerAvailable", false))
    }

    @Test
    fun chronerShop_visitClearsTimeTowerWhenShopGone() {
        val p = prefs()
        p.setBoolean("timeTowerAvailable", true)
        applyVisitShopHook(
            html = """That store isn't there anymore.""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=shakeshop",
            prefs = p,
            shopsText = "shakeshop\tShake Shop\n",
            coinText = "Shake Shop\tbuy\t1\tshake\tROW1\n",
        )
        assertFalse(p.getBoolean("timeTowerAvailable", true))
    }

    @Test
    fun chronerShop_skipsBuyActionUrl() {
        val p = prefs()
        p.setBoolean("timeTowerAvailable", false)
        applyVisitShopHook(
            html = """<b>flak shield</b>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=twitch_alliedhq&action=buy",
            prefs = p,
            shopsText = "twitch_alliedhq\tAllied HQ\n",
            coinText = "Allied HQ\tROW1599\tflak shield\tChroner (20)\n",
        )
        assertFalse(p.getBoolean("timeTowerAvailable", false))
    }

    @Test
    fun twitchPlace_setsTimeTowerAvailable() {
        val p = prefs()
        net.sourceforge.kolmafia.shop.TimeTowerSync.syncFromTwitchPlaceHtml(
            html = """<b>Time-Twitching Tower</b> town_tower""",
            prefs = p,
        )
        assertTrue(p.getBoolean("timeTowerAvailable", false))
    }

    @Test
    fun twitchPlace_clearsTimeTowerWhenTemporalEther() {
        val p = prefs()
        p.setBoolean("timeTowerAvailable", true)
        net.sourceforge.kolmafia.shop.TimeTowerSync.syncFromTwitchPlaceHtml(
            html = """You drift through the temporal ether.""",
            prefs = p,
        )
        assertFalse(p.getBoolean("timeTowerAvailable", true))
    }

    @Test
    fun trapper_visitSetsQuestPrefsWhenYetiFursPresent() {
        val p = prefs()
        applyVisitShopHook(
            html = """I'm plumb stocked up on everythin' 'cept yeti furs, Adventurer.""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=trapper",
            prefs = p,
            shopsText = "trapper\tThe Trapper\n",
            coinText = "The Trapper\tbuy\t1\tyak skin\tROW14\n",
            state = CharacterState(ascensionNumber = 9),
        )
        assertEquals(9, p.getInt("lastTr4pz0rQuest", -1))
        assertEquals(
            net.sourceforge.kolmafia.quest.QuestDatabase.FINISHED,
            p.getString(net.sourceforge.kolmafia.quest.Quest.TRAPPER.prefKey, "unstarted"),
        )
    }

    @Test
    fun lathe_visitSetsSpinmasterLatheVisited() {
        val p = prefs()
        applyVisitShopHook(
            html = """<b>SpinMaster lathe</b>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=lathe",
            prefs = p,
            shopsText = "lathe\tYour SpinMaster lathe\n",
            coinText = "Your SpinMaster lathe\tbuy\t1\tlathe item\tROW1\n",
        )
        assertTrue(p.getBoolean("_spinmasterLatheVisited", false))
    }

    @Test
    fun september_firstVisitParsesEmberBalance() {
        val p = prefs()
        applyVisitShopHook(
            html = """<b>You have 1,234 Embers.</b>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=september",
            prefs = p,
            shopsText = "september\tSept-Ember Censer\n",
            coinText = "Sept-Ember Censer\tbuy\t1\tember item\tROW1\n",
        )
        assertTrue(p.getBoolean("_septEmberBalanceChecked", false))
        assertEquals(1234, p.getInt("availableSeptEmbers", 0))
    }

    @Test
    fun junkmagazine_visitAdvancesHippyQuestFromUnstarted() {
        val p = prefs()
        applyVisitShopHook(
            html = """<b>Worse Homes and Gardens</b>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=junkmagazine",
            prefs = p,
            shopsText = "junkmagazine\tWorse Homes and Gardens\n",
            coinText = "Worse Homes and Gardens\tbuy\t1\tjunk item\tROW1\n",
        )
        assertEquals("step2", p.getString(net.sourceforge.kolmafia.quest.Quest.HIPPY.prefKey, "unstarted"))
    }

    @Test
    fun september_skipsBuyActionUrl() {
        val p = prefs()
        applyVisitShopHook(
            html = """<b>You have 5 Embers.</b>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=september&action=buy",
            prefs = p,
            shopsText = "september\tSept-Ember Censer\n",
            coinText = "Sept-Ember Censer\tbuy\t1\tember item\tROW1\n",
        )
        assertFalse(p.getBoolean("_septEmberBalanceChecked", false))
    }
}
