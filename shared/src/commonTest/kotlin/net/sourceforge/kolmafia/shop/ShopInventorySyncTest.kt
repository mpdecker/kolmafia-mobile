package net.sourceforge.kolmafia.shop

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.SkillDefinition
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.SessionLogger

class ShopInventorySyncTest {

    @AfterTest
    fun cleanup() {
        CoinmasterDatabase.resetForTest()
        ShopRowDatabase.resetForTest()
        CoinmasterVisitInventory.resetForTest()
        NpcStoreVisitOverlay.resetForTest()
        ItemDatabase.resetForTest()
        SkillDefinitionDatabase.resetForTest()
    }

    @Test
    fun parseAndLearn_logsUnknownRowAndRegistersOverlay() {
        registerItems()
        CoinmasterDatabase.loadFromText(
            shopsText = "fdkol\tFDKOL Requisitions Tent\tNPCCOIN\n",
            coinText = "",
        )
        ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "fdkol\tFDKOL Requisitions Tent\n",
        )
        val prefs = Preferences(MapSettings())
        val sessionLogger = SessionLogger(prefs, GameEventBus())
        val html = """
            <tr rel="$VISIT_ITEM">
            <a onClick='javascript:descitem($VISIT_ITEM)'><b>visit-learned item</b></a>
            <span title="FDKOL commendation"><b>75</b></span>
            <form action="shop.php?action=buy&whichshop=fdkol&whichrow=1500">
            </tr>
        """.trimIndent()

        ShopInventorySync.parseAndLearn(
            html = html,
            url = "shop.php?whichshop=fdkol",
            sessionLogger = sessionLogger,
        )

        assertTrue(CoinmasterVisitInventory.hasVisited("fdkol"))
        assertTrue(CoinmasterVisitInventory.containsItem("fdkol", VISIT_ITEM))
        assertNotNull(ShopRowDatabase.getShopRow(1500))
        val log = prefs.getString(SessionLogger.SESSION_LOG_KEY, "")
        assertTrue(log.contains("1500\tfdkol\tvisit-learned item\tFDKOL commendation (75)"))
        assertTrue(log.contains("FDKOL Requisitions Tent\tROW1500\tvisit-learned item"))
    }

    @Test
    fun parseAndLearn_persistsLearnedRowToPrefs() {
        registerItems()
        CoinmasterDatabase.loadFromText(
            shopsText = "fdkol\tFDKOL Requisitions Tent\tNPCCOIN\n",
            coinText = "",
        )
        ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "fdkol\tFDKOL Requisitions Tent\n",
        )
        val prefs = Preferences(MapSettings())
        ShopInventorySync.parseAndLearn(
            html = visitHtml(),
            url = "shop.php?whichshop=fdkol",
            sessionLogger = SessionLogger(prefs, GameEventBus()),
            prefs = prefs,
        )
        val stored = prefs.getString(ShopRowDatabase.LEARNED_SHOPROWS_KEY, "")
        assertTrue(stored.contains("1500\tfdkol\tvisit-learned item\tFDKOL commendation (75)"))
    }

    @Test
    fun parseAndLearn_skipsAjaxAndUhOh() {
        registerItems()
        ShopInventorySync.parseAndLearn(
            html = visitHtml(),
            url = "shop.php?whichshop=fdkol&ajax=1",
            sessionLogger = SessionLogger(Preferences(MapSettings()), GameEventBus()),
        )
        assertFalse(CoinmasterVisitInventory.hasVisited("fdkol"))

        ShopInventorySync.parseAndLearn(
            html = "<b style=\"color: white\">Uh Oh!</b>",
            url = "shop.php?whichshop=fdkol",
            sessionLogger = SessionLogger(Preferences(MapSettings()), GameEventBus()),
        )
        assertFalse(CoinmasterVisitInventory.hasVisited("fdkol"))
    }

    @Test
    fun parseAndLearn_skipsKnownBundledRow() {
        registerItems()
        ShopRowDatabase.loadFromText(
            shopRowsText = "1500\tfdkol\tvisit-learned item\tFDKOL commendation (75)\n",
            shopsText = "fdkol\tFDKOL Requisitions Tent\n",
        )
        val prefs = Preferences(MapSettings())
        val sessionLogger = SessionLogger(prefs, GameEventBus())
        ShopInventorySync.parseAndLearn(
            html = visitHtml(),
            url = "shop.php?whichshop=fdkol",
            sessionLogger = sessionLogger,
        )
        assertEquals("", prefs.getString(SessionLogger.SESSION_LOG_KEY, ""))
    }

    @Test
    fun parseAndLearn_logsConcoctionFormatForConcShop() {
        registerStillItems()
        ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "still\tNash Crosby's Still\tCONC\tSTILL\n",
        )
        val prefs = Preferences(MapSettings())
        val sessionLogger = SessionLogger(prefs, GameEventBus())
        val html = """
            <tr rel="$STILL_RESULT">
            <a onClick='javascript:descitem($STILL_RESULT)'><b>bottle of gin</b></a>
            <span title="bottle of vodka"><b>1</b></span>
            <form action="shop.php?action=buy&whichshop=still&whichrow=500">
            </tr>
        """.trimIndent()

        ShopInventorySync.parseAndLearn(
            html = html,
            url = "shop.php?whichshop=still",
            sessionLogger = sessionLogger,
        )

        assertFalse(CoinmasterVisitInventory.hasVisited("still"))
        val log = prefs.getString(SessionLogger.SESSION_LOG_KEY, "")
        assertTrue(log.contains("bottle of gin\tSTILL, ROW500\tbottle of vodka"))
        assertFalse(log.contains("Nash Crosby's Still\tROW500"))
    }

    @Test
    fun parseAndLearn_registersDynamicSkillNameInSessionLog() {
        registerDynamicSkillShopItems()
        CoinmasterDatabase.loadFromText(
            shopsText = "skillshop\tCorpus Skill Shop\tCOIN\n",
            coinText = """
                Corpus Skill Shop	buy	1	Other Skill	ROW2099
            """.trimIndent(),
        )
        ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "skillshop\tCorpus Skill Shop\n",
        )
        val prefs = Preferences(MapSettings())
        val sessionLogger = SessionLogger(prefs, GameEventBus())
        val html = """
            <tr rel="99999">
            <td></td>
            <td><img src="itemimages/skillbook.gif" onclick="javascript:poop('desc_skill.php?whichskill=$SKILL_ID&amp;self=true','skill',350,300)"></td>
            <td><b>Visit Learned Skill</b></td>
            <td><img src="itemimages/token.gif" onclick="javascript:descitem($TOKEN_ITEM)"></td>
            <td><b>5</b></td>
            <td><a href="shop.php?action=buyitem&whichshop=skillshop&whichrow=2100">Buy</a></td>
            </tr>
        """.trimIndent()

        ShopInventorySync.parseAndLearn(
            html = html,
            url = "shop.php?whichshop=skillshop",
            sessionLogger = sessionLogger,
        )

        assertEquals("Visit Learned Skill", SkillDefinitionDatabase.getById(SKILL_ID)?.name)
        val log = prefs.getString(SessionLogger.SESSION_LOG_KEY, "")
        assertTrue(log.contains("2100\tskillshop\tVisit Learned Skill\tshop token (5)"))
        assertFalse(log.contains("skill $SKILL_ID"))
    }

    @Test
    fun parseAndLearn_promotesShopTypeForCoinmasterVisit() {
        registerItems()
        CoinmasterDatabase.registerForTest(
            CoinmasterData(
                masterName = "FDKOL Requisitions Tent",
                nickname = "fdkol",
                shopId = "fdkol",
                token = "FDKOL commendation",
                buyItems = emptyList(),
                sellItems = emptyList(),
            ),
        )
        ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "fdkol\tFDKOL Requisitions Tent\n",
        )
        ShopInventorySync.parseAndLearn(
            html = visitHtml(),
            url = "shop.php?whichshop=fdkol",
            sessionLogger = null,
        )
        assertEquals(ShopType.COIN, ShopRowDatabase.shopType("fdkol"))
    }

    @Test
    fun parseAndLearn_logsVisitingOnCoinmasterShopVisit() {
        registerItems()
        CoinmasterDatabase.registerForTest(
            CoinmasterData(
                masterName = "FDKOL Requisitions Tent",
                nickname = "fdkol",
                shopId = "fdkol",
                token = "FDKOL commendation",
                buyItems = emptyList(),
                sellItems = emptyList(),
            ),
        )
        ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "fdkol\tFDKOL Requisitions Tent\n",
        )
        val prefs = Preferences(MapSettings())
        val sessionLogger = SessionLogger(prefs, GameEventBus())
        ShopInventorySync.parseAndLearn(
            html = visitHtml(),
            url = "shop.php?whichshop=fdkol",
            sessionLogger = sessionLogger,
        )
        val log = prefs.getString(SessionLogger.SESSION_LOG_KEY, "")
        assertTrue(log.contains("Visiting FDKOL Requisitions Tent"))
    }

    @Test
    fun parseAndLearn_disabledCoinmasterRelearnsBundledRow() {
        registerItems()
        CoinmasterDatabase.registerForTest(
            CoinmasterData(
                masterName = "FDKOL Requisitions Tent",
                nickname = "fdkol",
                shopId = "fdkol",
                token = "FDKOL commendation",
                buyItems = emptyList(),
                sellItems = emptyList(),
            ),
        )
        CoinmasterDatabase.findByShopId("fdkol")?.setDisabledForTest(true)
        ShopRowDatabase.loadFromText(
            shopRowsText = "1500\tfdkol\tvisit-learned item\tFDKOL commendation (75)\n",
            shopsText = "fdkol\tFDKOL Requisitions Tent\n",
        )
        val prefs = Preferences(MapSettings())
        val sessionLogger = SessionLogger(prefs, GameEventBus())
        ShopInventorySync.parseAndLearn(
            html = visitHtml(),
            url = "shop.php?whichshop=fdkol",
            sessionLogger = sessionLogger,
        )
        assertTrue(CoinmasterVisitInventory.containsItem("fdkol", VISIT_ITEM))
        val log = prefs.getString(SessionLogger.SESSION_LOG_KEY, "")
        assertTrue(log.contains("1500\tfdkol\tvisit-learned item\tFDKOL commendation (75)"))
    }

    @Test
    fun parseAndLearn_flowertradeinVisitShopRowsUpdatesOverlay() {
        registerFlowerTradeinItems()
        CoinmasterDatabase.loadFromText(
            shopsText = "flowertradein\tThe Central Loathing Floral Mercantile Exchange\n",
            coinText = """
                The Central Loathing Floral Mercantile Exchange	buy	1	Chroner	ROW759
            """.trimIndent(),
        )
        ShopInventorySync.parseAndLearn(
            html = flowerTradeinVisitHtml(),
            url = "shop.php?whichshop=flowertradein",
            sessionLogger = null,
        )
        assertTrue(CoinmasterVisitInventory.hasVisited("flowertradein"))
        assertNotNull(CoinmasterVisitInventory.findBuyRow("flowertradein", FLOWER_CHRONER))
    }

    @Test
    fun parseAndLearn_conmerchVisitShopRowsUpdatesOverlay() {
        registerConmerchItems()
        CoinmasterDatabase.loadFromText(
            shopsText = "conmerch\tKoL Con 13 Merch Table\n",
            coinText = """
                KoL Con 13 Merch Table	buy	1	Twitching Television Tattoo	ROW895
            """.trimIndent(),
        )
        ShopInventorySync.parseAndLearn(
            html = conmerchVisitHtml(),
            url = "shop.php?whichshop=conmerch",
            sessionLogger = null,
        )
        assertTrue(CoinmasterVisitInventory.hasVisited("conmerch"))
        assertNotNull(CoinmasterVisitInventory.findBuyRow("conmerch", CONMERCH_TATTOO))
    }

    @Test
    fun parseAndLearn_conmerchVisitShopSetsTokenPrefs() {
        registerConmerchItems()
        CoinmasterDatabase.loadFromText(
            shopsText = "conmerch\tKoL Con 13 Merch Table\n",
            coinText = """
                KoL Con 13 Merch Table	buy	1	Twitching Television Tattoo	ROW895
            """.trimIndent(),
        )
        val prefs = Preferences(MapSettings())
        ShopInventorySync.parseAndLearn(
            html = """
                You have 12 Mr. Accessories to trade.
                You have 2,000 Mr. Chroner to trade.
                ${conmerchVisitHtml()}
            """.trimIndent(),
            url = "shop.php?whichshop=conmerch",
            sessionLogger = null,
            prefs = prefs,
        )
        assertEquals(12, prefs.getInt(MerchTableSync.AVAILABLE_MR_A_PREF, 0))
        assertEquals(2000, prefs.getInt(MerchTableSync.AVAILABLE_CHRONERS_PREF, 0))
    }

    @Test
    fun parseAndLearn_driparmoryVisitShopSetsShieldPref() {
        CoinmasterDatabase.loadFromText(
            shopsText = "driparmory\tDrip Institute Armory\n",
            coinText = "Drip Institute Armory\tbuy\t50\tdrippy shield\tROW1132\n",
        )
        val prefs = Preferences(MapSettings())
        ShopInventorySync.parseAndLearn(
            html = """<b>drippy shield</b><td>Driplet (50)</td>""",
            url = "shop.php?whichshop=driparmory",
            sessionLogger = null,
            prefs = prefs,
        )
        assertTrue(prefs.getBoolean(DripArmoryPrefs.SHIELD_UNLOCK_PREF, false))
    }

    @Test
    fun parseAndLearn_trapperVisitShopSetsQuestPrefs() {
        CoinmasterDatabase.loadFromText(
            shopsText = "trapper\tThe Trapper\n",
            coinText = "The Trapper\tbuy\t1\tyak skin\tROW14\n",
        )
        val prefs = Preferences(MapSettings())
        ShopInventorySync.parseAndLearn(
            html = """I'm plumb stocked up on everythin' 'cept yeti furs, Adventurer.""",
            url = "shop.php?whichshop=trapper",
            sessionLogger = null,
            prefs = prefs,
            state = CharacterState(ascensionNumber = 5),
        )
        assertEquals(5, prefs.getInt("lastTr4pz0rQuest", -1))
        assertEquals(QuestDatabase.FINISHED, prefs.getString(Quest.TRAPPER.prefKey, QuestDatabase.UNSTARTED))
    }

    @Test
    fun parseAndLearn_septemberVisitShopParsesEmberBalance() {
        CoinmasterDatabase.loadFromText(
            shopsText = "september\tSept-Ember Censer\n",
            coinText = "Sept-Ember Censer\tbuy\t1\tember item\tROW1\n",
        )
        val prefs = Preferences(MapSettings())
        ShopInventorySync.parseAndLearn(
            html = """<b>You have 42 Embers.</b>""",
            url = "shop.php?whichshop=september",
            sessionLogger = null,
            prefs = prefs,
        )
        assertTrue(prefs.getBoolean(SeptEmberSync.BALANCE_CHECKED_PREF, false))
        assertEquals(42, prefs.getInt(SeptEmberSync.AVAILABLE_EMBERS_PREF, 0))
    }

    @Test
    fun parseAndLearn_mysticVisitShopSetsPsychosisUnlocked() {
        CoinmasterDatabase.loadFromText(
            shopsText = "mystic\tThe Crackpot Mystic's Shed\n",
            coinText = "The Crackpot Mystic's Shed\tROW39\tpixel pill\tred pixel (20)\n",
        )
        val prefs = Preferences(MapSettings())
        ShopInventorySync.parseAndLearn(
            html = """<tr rel="5906"><b>pixel pill</b></tr>""",
            url = "shop.php?whichshop=mystic",
            sessionLogger = null,
            prefs = prefs,
        )
        assertTrue(prefs.getBoolean(MysticShopSync.MYSTIC_PSYCHOSIS_ITEMS_UNLOCKED, false))
    }

    @Test
    fun parseAndLearn_shoreVisitShopClearsToasterBoughtPref() {
        CoinmasterDatabase.loadFromText(
            shopsText = "shore\tThe Shore, Inc. Gift Shop\n",
            coinText = "The Shore, Inc. Gift Shop\tROW637\tcheap toaster\tShore Inc. Ship Trip Scrip (20)\n",
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean(ShoreShopSync.CHEAP_TOASTER_BOUGHT_PREF, true)
        ShopInventorySync.parseAndLearn(
            html = """<b>cheap toaster</b>""",
            url = "shop.php?whichshop=shore",
            sessionLogger = null,
            prefs = prefs,
        )
        assertFalse(prefs.getBoolean(ShoreShopSync.CHEAP_TOASTER_BOUGHT_PREF, true))
    }

    @Test
    fun parseAndLearn_baconVisitShopSetsBoughtPrefs() {
        CoinmasterDatabase.loadFromText(
            shopsText = "bacon\tInternet Meme Shop\n",
            coinText = "Internet Meme Shop\tbuy\t1\tviral video\tROW1\n",
        )
        val prefs = Preferences(MapSettings())
        ShopInventorySync.parseAndLearn(
            html = "<html></html>",
            url = "shop.php?whichshop=bacon",
            sessionLogger = null,
            prefs = prefs,
        )
        assertTrue(prefs.getBoolean("_internetViralVideoBought", false))
        assertTrue(prefs.getBoolean("_internetPlusOneBought", false))
    }

    @Test
    fun parseAndLearn_mrreplicaVisitShopSetsYearPref() {
        CoinmasterDatabase.loadFromText(
            shopsText = "mrreplica\tReplica Mr. Store\n",
            coinText = "Replica Mr. Store\tbuy\t1\taugust scepter\tROW11325\n",
        )
        val prefs = Preferences(MapSettings())
        ShopInventorySync.parseAndLearn(
            html = """<td colspan=14 align=center>&mdash; <b>2023</b> &mdash;</td>""",
            url = "shop.php?whichshop=mrreplica",
            prefs = prefs,
        )
        assertEquals(2023, prefs.getInt("currentReplicaStoreYear", 0))
    }

    @Test
    fun parseAndLearn_blackmarketVisitShopUnlocksMacguffin() {
        CoinmasterDatabase.loadFromText(
            shopsText = "blackmarket\tThe Black Market\n",
            coinText = "The Black Market\tROW290\tRed Zeppelin ticket\tpriceless diamond (1)\n",
        )
        val prefs = Preferences(MapSettings())
        ShopInventorySync.parseAndLearn(
            html = "<html>The Black Market</html>",
            url = "shop.php?whichshop=blackmarket",
            prefs = prefs,
            state = CharacterState(ascensionNumber = 5),
        )
        assertEquals("step1", prefs.getString(Quest.MACGUFFIN.prefKey, QuestDatabase.UNSTARTED))
    }

    @Test
    fun parseAndLearn_piraterealmVisitShopSetsCrabsicleUnlock() {
        CoinmasterDatabase.loadFromText(
            shopsText = "piraterealm\tPirateRealm Fun-a-Log\n",
            coinText = "PirateRealm Fun-a-Log\tbuy\t100\tcrabsicle\tROW1053\n",
        )
        val prefs = Preferences(MapSettings())
        ShopInventorySync.parseAndLearn(
            html = """<tr rel="10199"><td>crabsicle</td></tr>""",
            url = "shop.php?whichshop=piraterealm",
            prefs = prefs,
        )
        assertTrue(prefs.getBoolean("pirateRealmUnlockedCrabsicle", false))
    }

    @Test
    fun parseAndLearn_chronerVisitShopSetsTimeTowerAvailable() {
        CoinmasterDatabase.loadFromText(
            shopsText = "twitch_alliedhq\tAllied HQ\n",
            coinText = "Allied HQ\tROW1599\tflak shield\tChroner (20)\n",
        )
        val prefs = Preferences(MapSettings())
        ShopInventorySync.parseAndLearn(
            html = """<b>flak shield</b> Chroner (20)""",
            url = "shop.php?whichshop=twitch_alliedhq",
            prefs = prefs,
        )
        assertTrue(prefs.getBoolean("timeTowerAvailable", false))
    }

    @Test
    fun parseAndLearn_crimbo23ElfBarVisitShopParsesMpcBalance() {
        CoinmasterDatabase.loadFromText(
            shopsText = "crimbo23_elf_bar\tElf Guard Officers' Club\n",
            coinText = "Elf Guard Officers' Club\tbuy\t5\tmulled wine\tROW1405\n",
        )
        val prefs = Preferences(MapSettings())
        ShopInventorySync.parseAndLearn(
            html = """<b>You have 42 Elf Guard MPCs.</b>""",
            url = "shop.php?whichshop=crimbo23_elf_bar",
            prefs = prefs,
        )
        assertEquals(42, prefs.getInt(Crimbo23ShopSync.AVAILABLE_ELF_MPC_PREF, 0))
    }

    @Test
    fun parseAndLearn_disabledCoinmasterSkipsLegacyBuyClassification() {
        registerLegacyShopItems()
        registerLegacyBuyMaster()
        CoinmasterDatabase.findByShopId(LEGACY_SHOP)?.setDisabledForTest(true)
        ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "$LEGACY_SHOP\tLegacy Test Shop\tCOIN\n",
        )
        val prefs = Preferences(MapSettings())
        val sessionLogger = SessionLogger(prefs, GameEventBus())
        ShopInventorySync.parseAndLearn(
            html = """
                <tr rel="$LEGACY_WIDGET">
                <a onClick='javascript:descitem($LEGACY_WIDGET)'><b>legacy widget</b></a>
                <span title="legacy token"><b>5</b></span>
                <form action="shop.php?action=buy&whichshop=$LEGACY_SHOP&whichrow=2301">
                </tr>
            """.trimIndent(),
            url = "shop.php?whichshop=$LEGACY_SHOP",
            sessionLogger = sessionLogger,
        )
        val log = prefs.getString(SessionLogger.SESSION_LOG_KEY, "")
        assertFalse(log.contains("Legacy Test Shop\tbuy\t"))
        assertTrue(log.contains("Legacy Test Shop\tROW2301"))
    }

    private fun registerDynamicSkillShopItems() {
        ItemDatabase.registerForTest(
            ItemData(
                id = TOKEN_ITEM,
                name = "shop token",
                descId = TOKEN_ITEM.toString(),
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    @Test
    fun parseAndLearn_logsSkillRowAndRegistersOverlay() {
        registerSkillShopItems()
        CoinmasterDatabase.loadFromText(
            shopsText = "skillshop\tCorpus Skill Shop\tCOIN\n",
            coinText = """
                Corpus Skill Shop	buy	1	Other Skill	ROW2099
            """.trimIndent(),
        )
        ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "skillshop\tCorpus Skill Shop\n",
        )
        val prefs = Preferences(MapSettings())
        val sessionLogger = SessionLogger(prefs, GameEventBus())
        val html = """
            <tr rel="99999">
            <td></td>
            <td><img src="itemimages/skillbook.gif" onclick="javascript:poop('desc_skill.php?whichskill=$SKILL_ID&amp;self=true','skill',350,300)"></td>
            <td><b>Corpus Skill</b></td>
            <td><img src="itemimages/token.gif" onclick="javascript:descitem($TOKEN_ITEM)"></td>
            <td><b>5</b></td>
            <td><a href="shop.php?action=buyitem&whichshop=skillshop&whichrow=2100">Buy</a></td>
            </tr>
        """.trimIndent()

        ShopInventorySync.parseAndLearn(
            html = html,
            url = "shop.php?whichshop=skillshop",
            sessionLogger = sessionLogger,
        )

        assertTrue(CoinmasterVisitInventory.hasVisited("skillshop"))
        assertTrue(CoinmasterVisitInventory.containsSkill("skillshop", SKILL_ID))
        assertFalse(CoinmasterVisitInventory.containsItem("skillshop", SKILL_ID))
        val log = prefs.getString(SessionLogger.SESSION_LOG_KEY, "")
        assertTrue(log.contains("2100\tskillshop\tCorpus Skill\tshop token (5)"))
        assertTrue(log.contains("Corpus Skill Shop\tROW2100\tCorpus Skill\tshop token (5)"))
    }

    @Test
    fun parseAndLearn_logsNewShopRegistration() {
        registerLegacyShopItems()
        val prefs = Preferences(MapSettings())
        val sessionLogger = SessionLogger(prefs, GameEventBus())
        val html = """
            <table><b style="color: white">Mystery Coin Shop</b></table>
            <tr rel="$LEGACY_WIDGET">
            <a onClick='javascript:descitem($LEGACY_WIDGET)'><b>legacy widget</b></a>
            <span title="legacy token"><b>5</b></span>
            <form action="shop.php?action=buy&whichshop=$LEGACY_SHOP&whichrow=2300">
            </tr>
        """.trimIndent()

        ShopInventorySync.parseAndLearn(
            html = html,
            url = "shop.php?whichshop=$LEGACY_SHOP",
            sessionLogger = sessionLogger,
        )

        assertEquals("Mystery Coin Shop", ShopRowDatabase.shopName(LEGACY_SHOP))
        val log = prefs.getString(SessionLogger.SESSION_LOG_KEY, "")
        assertTrue(log.contains("New shop: ($LEGACY_SHOP, \"Mystery Coin Shop\")"))
    }

    @Test
    fun parseAndLearn_logsLegacyBuyRow() {
        registerLegacyShopItems()
        registerLegacyBuyMaster()
        val prefs = Preferences(MapSettings())
        val sessionLogger = SessionLogger(prefs, GameEventBus())
        ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "$LEGACY_SHOP\tLegacy Test Shop\tCOIN\n",
        )
        val html = """
            <tr rel="$LEGACY_WIDGET">
            <a onClick='javascript:descitem($LEGACY_WIDGET)'><b>legacy widget</b></a>
            <span title="legacy token"><b>5</b></span>
            <form action="shop.php?action=buy&whichshop=$LEGACY_SHOP&whichrow=2301">
            </tr>
        """.trimIndent()

        ShopInventorySync.parseAndLearn(
            html = html,
            url = "shop.php?whichshop=$LEGACY_SHOP",
            sessionLogger = sessionLogger,
        )

        val log = prefs.getString(SessionLogger.SESSION_LOG_KEY, "")
        assertTrue(log.contains("Legacy Test Shop\tbuy\t5\tlegacy widget\tROW2301"))
        assertFalse(log.contains("Legacy Test Shop\tROW2301\tlegacy widget"))
    }

    @Test
    fun parseAndLearn_logsLegacySellRowAndRegistersSellOverlay() {
        registerLegacyShopItems()
        registerLegacySellMaster()
        ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "$LEGACY_SELL_SHOP\tLegacy Sell Shop\tCOIN\n",
        )
        val prefs = Preferences(MapSettings())
        val sessionLogger = SessionLogger(prefs, GameEventBus())
        val html = """
            <tr rel="$LEGACY_DIME">
            <a onClick='javascript:descitem($LEGACY_DIME)'><b>legacy dime</b></a>
            <span title="legacy junk"><b>3</b></span>
            <form action="shop.php?action=sell&whichshop=$LEGACY_SELL_SHOP&whichrow=2302">
            </tr>
        """.trimIndent()

        ShopInventorySync.parseAndLearn(
            html = html,
            url = "shop.php?whichshop=$LEGACY_SELL_SHOP",
            sessionLogger = sessionLogger,
        )

        assertTrue(CoinmasterVisitInventory.containsSellItem(LEGACY_SELL_SHOP, LEGACY_JUNK))
        val log = prefs.getString(SessionLogger.SESSION_LOG_KEY, "")
        assertTrue(log.contains("Legacy Sell Shop\tsell\t1\tlegacy junk (3)\tROW2302"))
    }

    private fun registerLegacyBuyMaster() {
        CoinmasterDatabase.registerForTest(
            CoinmasterData(
                masterName = "Legacy Test Shop",
                nickname = "legacytest",
                shopId = LEGACY_SHOP,
                token = "legacy token",
                buyItems = listOf(
                    ShopRow(
                        rowId = 1,
                        item = ItemStack(itemId = LEGACY_WIDGET, count = 1),
                        price = 5,
                    ),
                ),
                sellItems = emptyList(),
            ),
        )
    }

    private fun registerLegacySellMaster() {
        CoinmasterDatabase.registerForTest(
            CoinmasterData(
                masterName = "Legacy Sell Shop",
                nickname = "legacysell",
                shopId = LEGACY_SELL_SHOP,
                token = "legacy dime",
                buyItems = emptyList(),
                sellItems = listOf(
                    ShopRow(
                        rowId = 1,
                        item = ItemStack(itemId = LEGACY_JUNK, count = 1),
                        price = 3,
                    ),
                ),
            ),
        )
    }

    private fun registerLegacyShopItems() {
        listOf(
            LEGACY_WIDGET to "legacy widget",
            LEGACY_TOKEN to "legacy token",
            LEGACY_DIME to "legacy dime",
            LEGACY_JUNK to "legacy junk",
        ).forEach { (id, name) ->
            ItemDatabase.registerForTest(
                ItemData(
                    id = id,
                    name = name,
                    descId = id.toString(),
                    image = "img",
                    primaryUse = ItemPrimaryUse.USABLE,
                    secondaryUses = emptySet(),
                    access = setOf('t'),
                    autosellPrice = 1,
                    plural = null,
                ),
            )
        }
    }

    private fun registerSkillShopItems() {
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = SKILL_ID,
                name = "Corpus Skill",
                image = "skillbook",
                tags = setOf("passive"),
                mpCost = 0,
                duration = 0,
                isPassive = true,
                isCombat = false,
                isNonCombat = false,
                isSong = false,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = TOKEN_ITEM,
                name = "shop token",
                descId = TOKEN_ITEM.toString(),
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    private fun registerStillItems() {
        ItemDatabase.registerForTest(
            ItemData(
                id = STILL_RESULT,
                name = "bottle of gin",
                descId = "d$STILL_RESULT",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = STILL_INGREDIENT,
                name = "bottle of vodka",
                descId = "d$STILL_INGREDIENT",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    private fun visitHtml() = """
        <tr rel="$VISIT_ITEM">
        <a onClick='javascript:descitem($VISIT_ITEM)'><b>visit-learned item</b></a>
        <span title="FDKOL commendation"><b>75</b></span>
        <form action="shop.php?action=buy&whichshop=fdkol&whichrow=1500">
        </tr>
    """.trimIndent()

    private fun registerItems() {
        ItemDatabase.registerForTest(
            ItemData(
                id = VISIT_ITEM,
                name = "visit-learned item",
                descId = "d$VISIT_ITEM",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = FDKOL_COMMENDATION,
                name = "FDKOL commendation",
                descId = "d$FDKOL_COMMENDATION",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    private fun registerFlowerTradeinItems() {
        ItemDatabase.registerForTest(
            ItemData(
                id = FLOWER_CHRONER,
                name = "Chroner",
                descId = "d$FLOWER_CHRONER",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = FLOWER_ROSE,
                name = "rose",
                descId = "d$FLOWER_ROSE",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    private fun flowerTradeinVisitHtml() = """
        <tr rel="$FLOWER_CHRONER">
        <a onClick='javascript:descitem($FLOWER_CHRONER)'><b>Chroner</b></a>
        <span title="rose"><b>1</b></span>
        <form action="shop.php?action=buy&whichshop=flowertradein&whichrow=759">
        </tr>
    """.trimIndent()

    private fun registerConmerchItems() {
        ItemDatabase.registerForTest(
            ItemData(
                id = CONMERCH_TATTOO,
                name = "Twitching Television Tattoo",
                descId = "d$CONMERCH_TATTOO",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = CONMERCH_CHRONER,
                name = "Chroner",
                descId = "d$CONMERCH_CHRONER",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    private fun conmerchVisitHtml() = """
        <tr rel="$CONMERCH_TATTOO">
        <a onClick='javascript:descitem($CONMERCH_TATTOO)'><b>Twitching Television Tattoo</b></a>
        <span title="Chroner"><b>1111</b></span>
        <form action="shop.php?action=buy&whichshop=conmerch&whichrow=895">
        </tr>
    """.trimIndent()

    companion object {
        private const val VISIT_ITEM = 99201
        private const val FDKOL_COMMENDATION = 99202
        private const val STILL_RESULT = 99203
        private const val STILL_INGREDIENT = 99204
        private const val SKILL_ID = 6027
        private const val TOKEN_ITEM = 99205
        private const val LEGACY_SHOP = "legacytest"
        private const val LEGACY_SELL_SHOP = "legacysell"
        private const val LEGACY_WIDGET = 99206
        private const val LEGACY_TOKEN = 99207
        private const val LEGACY_DIME = 99208
        private const val LEGACY_JUNK = 99209
        private const val FLOWER_CHRONER = 7567
        private const val FLOWER_ROSE = 8668
        private const val CONMERCH_TATTOO = 9148
        private const val CONMERCH_CHRONER = 7567
    }
}
