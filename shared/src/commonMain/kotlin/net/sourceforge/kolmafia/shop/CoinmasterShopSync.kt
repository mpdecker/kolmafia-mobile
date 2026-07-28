package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase.Companion.FINISHED
import net.sourceforge.kolmafia.quest.QuestDatabase.Companion.UNSTARTED

/** Desktop coinmaster visitShop / purchasedItem pref sync (AshP147+). */
object CoinmasterShopSync {

    const val MYSTIC_PSYCHOSIS_ITEMS_UNLOCKED = "_mysticPsychosisItemsUnlocked"

    private const val COSMIC_SIX_PACK = 6237

    private const val VIRAL_VIDEO = 9017
    private const val PLUS_ONE = 9020
    private const val GALLON_OF_MILK = 9021
    private const val PRINT_SCREEN = 9022
    private const val DAILY_DUNGEON_MALWARE = 9024
    private const val MINI_KIWI_INTOXICATING_SPIRITS = 11602

    private const val CHEAP_TOASTER = 637
    private const val TALES_OF_DREAD = 6423
    private const val BRASS_DREAD_FLASK = 6428
    private const val SILVER_DREAD_FLASK = 6429

    private const val PIXEL_PILL = 5906
    private const val PIXEL_ENERGY_TANK = 5907
    private const val PIXEL_GRAPPLING_HOOK = 6173

    private val PSYCHOSIS_PIXEL_ITEMS = intArrayOf(
        PIXEL_PILL,
        PIXEL_ENERGY_TANK,
        PIXEL_GRAPPLING_HOOK,
    )

    private val ARCADE_UNLOCKABLE_ITEMS = intArrayOf(
        4637, 4638, 4639, 4646, 4647,
    )

    private val DESC_ITEM_PATTERN = Regex("""descitem\((\d+)\)""")
    private val ARCADE_ITEM_PATTERN = Regex("""<tr rel="(\d+)"""")
    private val SHOP_ID_PATTERN = Regex("""whichshop=([^&]+)""", RegexOption.IGNORE_CASE)
    private val REPLICA_YEAR_PATTERN = Regex("""&mdash; <b>(\d+)</b> &mdash;""")

    private data class SwaggerSeason(
        val seasonName: String,
        val itemId: Int,
        val availablePref: String,
        val costPref: String,
        val swaggerPref: String,
    )

    private val SWAGGER_SEASONS = listOf(
        SwaggerSeason("pirate", 7732, "blackBartsBootyAvailable", "blackBartsBootyCost", "pirateSwagger"),
        SwaggerSeason("holiday", 4810, "holidayHalsBookAvailable", "holidayHalsBookCost", "holidaySwagger"),
        SwaggerSeason("ice", 4812, "antagonisticSnowmanKitAvailable", "antagonisticSnowmanKitCost", "iceSwagger"),
        SwaggerSeason("drunken", 8182, "mapToKokomoAvailable", "mapToKokomoCost", "drunkenSwagger"),
        SwaggerSeason("bear", 8277, "essenceOfBearAvailable", "essenceOfBearCost", "bearSwagger"),
        SwaggerSeason("numeric", 8488, "manualOfNumberologyAvailable", "manualOfNumberologyCost", "numericSwagger"),
        SwaggerSeason("optimal", 8800, "ROMOfOptimalityAvailable", "ROMOfOptimalityCost", "optimalSwagger"),
        SwaggerSeason("school", 9123, "schoolOfHardKnocksDiplomaAvailable", "schoolOfHardKnocksDiplomaCost", "schoolSwagger"),
        SwaggerSeason("safari", 9921, "guideToSafariAvailable", "guideToSafariCost", "safariSwagger"),
        SwaggerSeason("glitch", 10207, "glitchItemAvailable", "glitchItemCost", "glitchSwagger"),
        SwaggerSeason("average", 10325, "lawOfAveragesAvailable", "lawOfAveragesCost", "averageSwagger"),
        SwaggerSeason("Seasoning", 10640, "universalSeasoningAvailable", "universalSeasoningCost", "SeasoningSwagger"),
        SwaggerSeason("ironic", 11867, "bookOfIronyAvailable", "bookOfIronyCost", "ironicSwagger"),
        SwaggerSeason("none", 4804, "essenceOfAnnoyanceAvailable", "essenceOfAnnoyanceCost", "availableSwagger"),
    )

    private val SWAGGER_ITEM_PATTERN = Regex(
        """whichitem" value="(\d+)".*?\((\d[\d,]*) swagger\)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    private val SWAGGER_SEASON_PATTERN = Regex(
        """You've earned -?([\d,]+) swagger during (?:a |an |)?(pirate|holiday|ice|drunken|bear|numeric|optimal|school|safari|glitch|average|Seasoning|ironic|none)(?: season)?""",
        RegexOption.IGNORE_CASE,
    )

    fun applySwaggerVisit(html: String, url: String?, prefs: Preferences?) {
        if (prefs == null) return
        if (url?.contains("action=buy", ignoreCase = true) == true) return

        val presentItems = mutableSetOf<Int>()
        val itemPrices = mutableMapOf<Int, Int>()
        for (match in SWAGGER_ITEM_PATTERN.findAll(html)) {
            val itemId = match.groupValues[1].toIntOrNull() ?: continue
            val price = match.groupValues[2].replace(",", "").toIntOrNull() ?: continue
            presentItems.add(itemId)
            itemPrices[itemId] = price
        }

        for (season in SWAGGER_SEASONS) {
            prefs.setBoolean(season.availablePref, season.itemId in presentItems)
            itemPrices[season.itemId]?.let { prefs.setInt(season.costPref, it) }
        }

        SWAGGER_SEASON_PATTERN.find(html)?.let { match ->
            val swagger = match.groupValues[1].replace(",", "").toIntOrNull() ?: 0
            val seasonName = match.groupValues[2]
            prefs.setString("currentPVPSeason", seasonName)
            SWAGGER_SEASONS.firstOrNull {
                it.seasonName.equals(seasonName, ignoreCase = true)
            }?.let { season ->
                prefs.setInt(season.swaggerPref, swagger)
            }
        }

        val visitRows = presentItems.map { itemId ->
            ShopRow(
                rowId = itemId,
                item = ItemStack(itemId = itemId, count = 1),
                price = itemPrices[itemId] ?: 0,
            )
        }
        CoinmasterVisitInventory.replaceBuyRows(CoinmasterVisitInventory.SWAGGER, visitRows)
    }

    fun apply(
        html: String,
        url: String?,
        prefs: Preferences?,
        state: CharacterState? = null,
        sessionLogger: SessionLogger? = null,
    ) {
        if (prefs == null) return
        val shopId = extractShopId(url) ?: return
        when (shopId.lowercase()) {
            "5dprinter" -> syncFiveDPrinter(html, prefs)
            "bacon" -> syncBacon(html, prefs)
            "arcade" -> syncArcade(html, prefs)
            "kiwi" -> syncKiwi(html, prefs)
            "mystic" -> syncMystic(html, prefs)
            "shore" -> syncShore(html, prefs)
            "mrreplica" -> syncReplicaMrStore(html, prefs)
            "blackmarket" -> syncBlackMarket(prefs, state)
            "piraterealm" -> syncPirateRealmFunALog(html, url, prefs)
            "driparmory" -> syncDripArmory(html, url, prefs)
            "conmerch" -> syncConmerch(html, url, prefs)
            in TimeTowerSync.CHRONER_SHOP_IDS -> syncTimeTowerChronerShop(html, url, prefs)
            "trapper" -> syncTrapper(html, url, prefs, state)
            "lathe" -> syncLathe(html, url, prefs)
            "september" -> syncSeptember(html, url, prefs)
            "junkmagazine" -> syncJunkMagazine(html, url, prefs)
            "flowertradein" -> syncFlowerTradein(html, url, prefs)
            "crimbo25_sammy" -> syncCrimbo25Sammy(html, url, prefs)
            "armory" -> syncArmoryAndLeggery(html, url, prefs, sessionLogger)
            else -> {
                if (shopId.startsWith("crimbo23_")) {
                    syncCrimbo23Shop(html, url, prefs)
                }
            }
        }
    }

    fun applyPurchasedItem(
        master: CoinmasterData,
        itemId: Int,
        prefs: Preferences?,
        gameDatabase: net.sourceforge.kolmafia.data.GameDatabase? = null,
    ) {
        if (prefs == null) return
        when (master.nickname.lowercase()) {
            "bacon" -> when (itemId) {
                VIRAL_VIDEO -> prefs.setBoolean("_internetViralVideoBought", true)
                PLUS_ONE -> prefs.setBoolean("_internetPlusOneBought", true)
                GALLON_OF_MILK -> prefs.setBoolean("_internetGallonOfMilkBought", true)
                PRINT_SCREEN -> prefs.setBoolean("_internetPrintScreenButtonBought", true)
                DAILY_DUNGEON_MALWARE -> prefs.setBoolean("_internetDailyDungeonMalwareBought", true)
            }
            "kiwi" -> {
                if (itemId == MINI_KIWI_INTOXICATING_SPIRITS) {
                    prefs.setBoolean("_miniKiwiIntoxicatingSpiritsBought", true)
                }
            }
            "shore" -> {
                if (itemId == CHEAP_TOASTER) {
                    prefs.setBoolean("itemBoughtPerAscension637", true)
                }
            }
            "dv" -> when (itemId) {
                TALES_OF_DREAD -> prefs.setBoolean("itemBoughtPerCharacter6423", true)
                BRASS_DREAD_FLASK -> prefs.setBoolean("itemBoughtPerCharacter6428", true)
                SILVER_DREAD_FLASK -> prefs.setBoolean("itemBoughtPerCharacter6429", true)
            }
            "jarl" -> {
                if (itemId == COSMIC_SIX_PACK) {
                    prefs.setBoolean("_cosmicSixPackConjured", true)
                }
            }
            "wereprofessor_tinker" -> TinkeringBenchPurchasedItem.apply(master, itemId, gameDatabase)
        }
    }

    private fun extractShopId(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return SHOP_ID_PATTERN.find(url)?.groupValues?.getOrNull(1)
    }

    private fun syncFiveDPrinter(html: String, prefs: Preferences) {
        for (match in DESC_ITEM_PATTERN.findAll(html)) {
            val descId = match.groupValues[1]
            val itemId = ItemDatabase.getByDescId(descId)?.id ?: continue
            if (itemId <= 0 || itemId !in FiveDPrinterAccessibility.UNKNOWN_RECIPE_ITEMS) continue
            val prefKey = "unknownRecipe$itemId"
            if (prefs.getBoolean(prefKey, true)) {
                prefs.setBoolean(prefKey, false)
            }
        }
    }

    private fun syncBacon(html: String, prefs: Preferences) {
        prefs.setBoolean("_internetViralVideoBought", !html.contains("viral video", ignoreCase = true))
        prefs.setBoolean("_internetPlusOneBought", !html.contains("plus one", ignoreCase = true))
        prefs.setBoolean("_internetGallonOfMilkBought", !html.contains("gallon of milk", ignoreCase = true))
        prefs.setBoolean(
            "_internetPrintScreenButtonBought",
            !html.contains("print screen button", ignoreCase = true),
        )
        prefs.setBoolean(
            "_internetDailyDungeonMalwareBought",
            !html.contains("daily dungeon malware", ignoreCase = true),
        )
    }

    private fun syncArcade(html: String, prefs: Preferences) {
        for (match in ARCADE_ITEM_PATTERN.findAll(html)) {
            val id = match.groupValues[1].toIntOrNull() ?: continue
            if (id in ARCADE_UNLOCKABLE_ITEMS) {
                prefs.setBoolean("lockedItem$id", false)
            }
        }
    }

    private fun syncKiwi(html: String, prefs: Preferences) {
        prefs.setBoolean(
            "_miniKiwiIntoxicatingSpiritsBought",
            !html.contains("mini kiwi intoxicating spirits", ignoreCase = true),
        )
    }

    private fun syncMystic(html: String, prefs: Preferences) {
        val unlocked = PSYCHOSIS_PIXEL_ITEMS.any { itemId ->
            html.contains("""<tr rel="$itemId"""", ignoreCase = true) ||
                when (itemId) {
                    PIXEL_PILL -> html.contains("pixel pill", ignoreCase = true)
                    PIXEL_ENERGY_TANK -> html.contains("pixel energy tank", ignoreCase = true)
                    PIXEL_GRAPPLING_HOOK -> html.contains("pixel grappling hook", ignoreCase = true)
                    else -> false
                }
        }
        if (unlocked) {
            prefs.setBoolean(MYSTIC_PSYCHOSIS_ITEMS_UNLOCKED, true)
        }
    }

    private fun syncShore(html: String, prefs: Preferences) {
        prefs.setBoolean(
            "itemBoughtPerAscension637",
            !html.contains("cheap toaster", ignoreCase = true),
        )
    }

    private fun syncReplicaMrStore(html: String, prefs: Preferences) {
        REPLICA_YEAR_PATTERN.find(html)?.let { match ->
            val year = match.groupValues[1].toIntOrNull() ?: return
            prefs.setInt("currentReplicaStoreYear", year)
        }
    }

    private fun syncBlackMarket(prefs: Preferences, state: CharacterState?) {
        val charState = state ?: CharacterState()
        if (prefs.getInt("lastWuTangDefeated", -1) == charState.ascensionNumber) return
        if (charState.inNuclearAutumn) return
        val progress = prefs.getString(Quest.MACGUFFIN.prefKey, UNSTARTED)
        if (progress == FINISHED || progress.contains("step")) return
        prefs.setString(Quest.MACGUFFIN.prefKey, "step1")
    }

    private fun syncPirateRealmFunALog(html: String, url: String?, prefs: Preferences) {
        if (url?.contains("action=buy", ignoreCase = true) == true) return
        FunALogUnlockPrefs.syncFromShopHtml(html, prefs)
    }

    private fun syncDripArmory(html: String, url: String?, prefs: Preferences) {
        if (url?.contains("action=buy", ignoreCase = true) == true) return
        DripArmoryPrefs.syncFromShopHtml(html, prefs)
    }

    private fun syncTimeTowerChronerShop(html: String, url: String?, prefs: Preferences) {
        if (url?.contains("action=buy", ignoreCase = true) == true) return
        TimeTowerSync.syncFromChronerShopHtml(html, prefs)
    }

    private fun syncConmerch(html: String, url: String?, prefs: Preferences) {
        if (url?.contains("action=buy", ignoreCase = true) == true) return
        MerchTableSync.syncFromShopHtml(html, prefs)
    }

    private fun syncFlowerTradein(html: String, url: String?, prefs: Preferences) {
        if (url?.contains("action=buy", ignoreCase = true) == true) return
        FlowerTradeinSync.syncFromShopHtml(html, prefs)
    }

    private fun syncCrimbo25Sammy(html: String, url: String?, prefs: Preferences) {
        if (url?.contains("action=buy", ignoreCase = true) == true) return
        Crimbo25SammySync.syncFromShopHtml(html, prefs)
    }

    private fun syncArmoryAndLeggery(
        html: String,
        url: String?,
        prefs: Preferences,
        sessionLogger: SessionLogger?,
    ) {
        if (url?.contains("action=buy", ignoreCase = true) == true) return
        ArmoryAndLeggerySync.syncFromShopHtml(html, prefs, sessionLogger = sessionLogger)
    }

    private fun syncCrimbo23Shop(html: String, url: String?, prefs: Preferences) {
        if (url?.contains("action=buy", ignoreCase = true) == true) return
        val shopId = extractShopId(url) ?: return
        Crimbo23ShopSync.syncFromShopHtml(html, shopId, prefs)
    }

    private fun syncTrapper(html: String, url: String?, prefs: Preferences, state: CharacterState?) {
        if (url?.contains("action=buy", ignoreCase = true) == true) return
        val ascension = state?.ascensionNumber ?: 0
        TrapperSync.syncFromShopHtml(html, prefs, ascension)
    }

    private fun syncLathe(html: String, url: String?, prefs: Preferences) {
        if (url?.contains("action=buy", ignoreCase = true) == true) return
        SpinMasterLatheSync.syncFromShopHtml(prefs)
    }

    private fun syncSeptember(html: String, url: String?, prefs: Preferences) {
        if (url?.contains("action=buy", ignoreCase = true) == true) return
        SeptEmberSync.syncFromShopHtml(html, prefs)
    }

    private fun syncJunkMagazine(html: String, url: String?, prefs: Preferences) {
        if (url?.contains("action=buy", ignoreCase = true) == true) return
        JunkMagazineSync.syncFromShopHtml(prefs)
    }
}
