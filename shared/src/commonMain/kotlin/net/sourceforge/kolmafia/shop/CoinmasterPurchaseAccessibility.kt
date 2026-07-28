package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.StandardRewardDatabase
import net.sourceforge.kolmafia.data.TorsoAwareness
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase.Companion.FINISHED
import net.sourceforge.kolmafia.quest.QuestDatabase.Companion.UNSTARTED

/** Desktop coinmaster per-item canBuyItem gates (expanded for high-traffic masters). */
object CoinmasterPurchaseAccessibility {

    private const val RED_ZEPPELIN_TICKET = 7185
    private const val PATCHOULI_OIL_BOMB = 2040
    private const val EXPLODING_HACKY_SACK = 2042
    private const val TEQUILA_GRENADE = 2068
    private const val MOLOTOV_COCKTAIL_COCKTAIL = 2400
    private const val WAR_HIPPY_HEADBAND = 2337
    private const val WAR_HIPPY_CORDS = 2032
    private const val WAR_HIPPY_GLASSES = 2033
    private const val WAR_FRAT_HELMET = 2069
    private const val WAR_FRAT_PANTS = 2070
    private const val WAR_FRAT_PIN = 2353

    private const val COSMIC_SIX_PACK = 6237
    private const val STAFF_OF_BREAKFAST = 6258
    private const val STAFF_OF_LIFE = 6259
    private const val STAFF_OF_LUNCH = 6260
    private const val STAFF_OF_CHEESE = 6261
    private const val STAFF_OF_DINNER = 6262
    private const val STAFF_OF_STEAK = 6263
    private const val STAFF_OF_FRUIT = 6264
    private const val STAFF_OF_CREAM = 6265

    private const val MIME_SCIENCE_VOL_1 = 9635
    private const val MIME_SCIENCE_VOL_2 = 9637
    private const val MIME_SCIENCE_VOL_3 = 9639
    private const val MIME_SCIENCE_VOL_4 = 9641
    private const val MIME_SCIENCE_VOL_5 = 9643
    private const val MIME_SCIENCE_VOL_6 = 9645

    private const val SKILL_BAKE = 14023
    private const val SKILL_BLEND = 14034
    private const val SKILL_BOIL = 14003
    private const val SKILL_CHOP = 14014
    private const val SKILL_CURDLE = 14000
    private const val SKILL_FREEZE = 14033
    private const val SKILL_FRY = 14004
    private const val SKILL_GRILL = 14024
    private const val SKILL_SLICE = 14013

    private val JARLSBERG_BAKE_ITEMS = intArrayOf(6191, 6200, 6202, 6207)
    private val JARLSBERG_BLEND_ITEMS = intArrayOf(6187, 6197, 6205, 6210)
    private val JARLSBERG_BOIL_ITEMS = intArrayOf(6185, 6188, 6194)
    private val JARLSBERG_CHOP_ITEMS = intArrayOf(6190, 6196)
    private val JARLSBERG_CURDLE_ITEMS = intArrayOf(6193, 6198, 6208, 6211, 6214)
    private val JARLSBERG_FREEZE_ITEMS = intArrayOf(6204, 6209, 6213)
    private val JARLSBERG_FRY_ITEMS = intArrayOf(6186, 6201, 6206)
    private val JARLSBERG_GRILL_ITEMS = intArrayOf(6192, 6203)
    private val JARLSBERG_SLICE_ITEMS = intArrayOf(6189, 6195, 6199, 6212)

    private val JARLSBERG_STAFF_ITEMS = intArrayOf(
        STAFF_OF_BREAKFAST,
        STAFF_OF_LIFE,
        STAFF_OF_LUNCH,
        STAFF_OF_CHEESE,
        STAFF_OF_DINNER,
        STAFF_OF_STEAK,
        STAFF_OF_FRUIT,
        STAFF_OF_CREAM,
    )

    private const val TOASTER = 637
    private const val UV_RESISTANT_COMPASS = 6729
    private const val STAR_SHIRT = 1133
    private const val SUGAR_SHIRT = 4191
    private const val YELLOW_SUBMARINE = 8376
    private const val PIXEL_PILL = 5906
    private const val PIXEL_ENERGY_TANK = 5907
    private const val PIXEL_GRAPPLING_HOOK = 6173

    private const val VIRAL_VIDEO = 9017
    private const val PLUS_ONE = 9020
    private const val GALLON_OF_MILK = 9021
    private const val PRINT_SCREEN = 9022
    private const val DAILY_DUNGEON_MALWARE = 9024

    private const val SINISTER_DEMON_MASK = 4637
    private const val CHAMPION_BELT = 4638
    private const val SPACE_TRIP_HEADPHONES = 4639
    private const val METEOID_ICE_BEAM = 4646
    private const val DUNGEON_FIST_GAUNTLET = 4647
    private const val FOLDER_JACKASS_PLUMBER = 6631

    private const val TALES_OF_DREAD = 6423
    private const val BRASS_DREAD_FLASK = 6428
    private const val SILVER_DREAD_FLASK = 6429
    private const val FOLDER_21 = 6638

    private const val MINI_KIWI_INTOXICATING_SPIRITS = 11602
    private const val DENTADENT = 11977
    private const val MONODENT_OF_THE_SEA = 11975

    private const val BROBERRY_BROGURT = 7455
    private const val BROCOLATE_BROGURT = 7456
    private const val FRENCH_BRONILLA_BROGURT = 7457
    private const val TACO_FISH_TACO = 7451
    private const val TACO_SAUCE = 7452
    private const val SEWING_KIT = 7300

    private val BACON_ONE_TIME_ITEMS = mapOf(
        VIRAL_VIDEO to "_internetViralVideoBought",
        PLUS_ONE to "_internetPlusOneBought",
        GALLON_OF_MILK to "_internetGallonOfMilkBought",
        PRINT_SCREEN to "_internetPrintScreenButtonBought",
        DAILY_DUNGEON_MALWARE to "_internetDailyDungeonMalwareBought",
    )

    private val ARCADE_LOCKED_ITEMS = intArrayOf(
        SINISTER_DEMON_MASK,
        CHAMPION_BELT,
        SPACE_TRIP_HEADPHONES,
        METEOID_ICE_BEAM,
        DUNGEON_FIST_GAUNTLET,
    )

    private const val FOOD_DRIVE_BUTTON = 10691
    private const val BOOZE_DRIVE_BUTTON = 10692
    private const val CANDY_DRIVE_BUTTON = 10693
    private const val FOOD_MAILING_LIST = 10694
    private const val BOOZE_MAILING_LIST = 10695
    private const val CANDY_MAILING_LIST = 10696

    private val CRIMBO20_ONE_TIME_ITEMS = intArrayOf(
        FOOD_DRIVE_BUTTON,
        BOOZE_DRIVE_BUTTON,
        CANDY_DRIVE_BUTTON,
        FOOD_MAILING_LIST,
        BOOZE_MAILING_LIST,
        CANDY_MAILING_LIST,
    )

    private val SWAGGER_SEASON_ITEMS = mapOf(
        7732 to "blackBartsBootyAvailable",
        4810 to "holidayHalsBookAvailable",
        4812 to "antagonisticSnowmanKitAvailable",
        8182 to "mapToKokomoAvailable",
        8277 to "essenceOfBearAvailable",
        8488 to "manualOfNumberologyAvailable",
        8800 to "ROMOfOptimalityAvailable",
        9123 to "schoolOfHardKnocksDiplomaAvailable",
        9921 to "guideToSafariAvailable",
        10207 to "glitchItemAvailable",
        10325 to "lawOfAveragesAvailable",
        10640 to "universalSeasoningAvailable",
        11867 to "bookOfIronyAvailable",
        4804 to "essenceOfAnnoyanceAvailable",
    )

    fun canPurchaseItem(
        master: CoinmasterData,
        itemId: Int,
        state: CharacterState,
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
        hasSkill: (Int) -> Boolean = { false },
    ): Boolean {
        if (!standardRewardItemAvailable(itemId)) return false
        if (!visitInventoryItemAvailable(master, itemId)) return false
        val nickname = master.nickname.lowercase()
        return when {
            nickname == "blackmarket" ||
                master.masterName.equals("The Black Market", ignoreCase = true) ->
                blackMarketItemAvailable(itemId, state, prefs, accessibleCount)
            nickname == "dimemaster" || nickname == "dmt" ->
                dimemasterItemAvailable(itemId, state, prefs, accessibleCount)
            nickname == "quartersmaster" ->
                quartersmasterItemAvailable(itemId, state, prefs, accessibleCount)
            nickname == "jarl" ||
                master.masterName.equals("Jarlsberg's Cosmic Kitchen", ignoreCase = true) ->
                jarlsbergItemAvailable(itemId, prefs, accessibleCount, hasSkill)
            nickname == "swagger" ||
                master.masterName.equals("The Swagger Shop", ignoreCase = true) ->
                swaggerItemAvailable(itemId, prefs)
            nickname == "crimbo17" ||
                master.masterName.equals("Cheer-o-Vend 3000", ignoreCase = true) ->
                crimbo17ItemAvailable(itemId, state)
            nickname == "crimbo20food" ||
                nickname == "crimbo20booze" ||
                nickname == "crimbo20candy" ->
                crimbo20ItemAvailable(itemId, accessibleCount)
            nickname == "shore" ||
                master.masterName.equals("The Shore, Inc. Gift Shop", ignoreCase = true) ->
                shoreItemAvailable(itemId, prefs, accessibleCount)
            nickname == "mrreplica" ||
                master.masterName.equals("Replica Mr. Store", ignoreCase = true) ->
                ReplicaMrStoreAccessibility.isItemAvailable(itemId, prefs, accessibleCount)
            nickname == "mystic" ||
                master.masterName.equals("The Crackpot Mystic's Shed", ignoreCase = true) ->
                pixelItemAvailable(itemId, state, prefs)
            nickname == "starchart" ->
                starchartItemAvailable(itemId, hasSkill)
            nickname == "sugarsheets" ->
                sugarsheetsItemAvailable(itemId, hasSkill)
            nickname == "5dprinter" ->
                FiveDPrinterAccessibility.isItemAvailable(itemId, prefs)
            nickname == "bacon" ||
                master.masterName.equals("Internet Meme Shop", ignoreCase = true) ->
                baconItemAvailable(itemId, prefs)
            nickname == "arcade" ||
                master.masterName.equals("Arcade Ticket Counter", ignoreCase = true) ->
                arcadeItemAvailable(itemId, prefs, accessibleCount)
            nickname == "dv" ||
                master.masterName.equals("The Terrified Eagle Inn", ignoreCase = true) ->
                dreadsylvaniaItemAvailable(itemId, prefs, accessibleCount)
            nickname == "kiwi" ||
                master.masterName.equals("Kiwi Kwiki Mart", ignoreCase = true) ->
                kiwiItemAvailable(itemId, prefs)
            nickname == "fixodent" ||
                master.masterName.equals("Craft with Teeth", ignoreCase = true) ->
                fixodentItemAvailable(itemId, accessibleCount)
            nickname == "piraterealm" ||
                nickname == "piraterealmfunalog" ||
                master.masterName.equals("PirateRealm Fun-a-Log", ignoreCase = true) ->
                FunALogUnlockPrefs.isItemAvailable(itemId, prefs)
            nickname == "driparmory" ||
                master.masterName.equals("Drip Institute Armory", ignoreCase = true) ->
                DripArmoryPrefs.isItemAvailable(itemId, prefs, accessibleCount)
            nickname == "sbb_brogurt" || nickname == "brogurt" ||
                master.masterName.equals("The Frozen Brogurt Stand", ignoreCase = true) ->
                sbbBrogurtItemAvailable(itemId, prefs)
            nickname == "sbb_taco" || nickname == "taco_dan" ||
                master.masterName.equals("Taco Dan's Taco Stand", ignoreCase = true) ->
                sbbTacoItemAvailable(itemId, prefs)
            nickname == "damachine" || nickname == "vendingmachine" ||
                master.masterName.equals("Vending Machine", ignoreCase = true) ->
                vendingMachineItemAvailable(itemId, accessibleCount)
            nickname == "wereprofessor_tinker" ||
                master.masterName.equals("Tinkering Bench", ignoreCase = true) ->
                tinkeringBenchItemAvailable(itemId, accessibleCount)
            nickname == "fdkol" ||
                master.masterName.equals("FDKOL Requisitions Tent", ignoreCase = true) ->
                fdkolItemAvailable(itemId, prefs)
            else -> true
        }
    }

    /** AshP192/AshP194 — visit-learned inventory gates for coinmasters with runtime overlay rows. */
    internal fun visitInventoryItemAvailable(master: CoinmasterData, itemId: Int): Boolean {
        val shopId = master.shopId?.lowercase() ?: return true
        if (!CoinmasterVisitInventory.hasVisited(shopId)) return true
        if (CoinmasterVisitInventory.isDynamicShop(shopId) ||
            CoinmasterVisitInventory.hasVisitOverlay(shopId)
        ) {
            return CoinmasterVisitInventory.containsItem(shopId, itemId)
        }
        return true
    }

    private fun fdkolItemAvailable(itemId: Int, prefs: Preferences?): Boolean {
        if (CoinmasterVisitInventory.hasVisited("fdkol")) {
            return CoinmasterVisitInventory.containsItem("fdkol", itemId)
        }
        return true
    }

    fun standardRewardItemAvailable(itemId: Int): Boolean {
        val reward = StandardRewardDatabase.findStandardReward(itemId) ?: return true
        if (reward.row.equals("UNKNOWN", ignoreCase = true)) return false
        if (StandardRewardDatabase.findPulverization(reward) == -1) return false
        return true
    }

    private fun sbbBrogurtItemAvailable(itemId: Int, prefs: Preferences?): Boolean =
        when (itemId) {
            BROBERRY_BROGURT, BROCOLATE_BROGURT, FRENCH_BRONILLA_BROGURT ->
                prefs?.getString("questESlBacteria", UNSTARTED) == FINISHED
            else -> true
        }

    private fun sbbTacoItemAvailable(itemId: Int, prefs: Preferences?): Boolean =
        when (itemId) {
            TACO_FISH_TACO -> prefs?.getString("questESlFish", UNSTARTED) == FINISHED
            TACO_SAUCE -> prefs?.getString("questESlSprinkles", UNSTARTED) == FINISHED
            else -> true
        }

    private fun vendingMachineItemAvailable(itemId: Int, accessibleCount: (Int) -> Int): Boolean =
        when (itemId) {
            SEWING_KIT -> accessibleCount(SEWING_KIT) <= 0
            else -> true
        }

    private fun tinkeringBenchItemAvailable(itemId: Int, accessibleCount: (Int) -> Int): Boolean =
        TinkeringBenchGates.canMakeItem(itemId, accessibleCount)

    private fun baconItemAvailable(itemId: Int, prefs: Preferences?): Boolean {
        val prefKey = BACON_ONE_TIME_ITEMS[itemId] ?: return true
        return prefs?.getBoolean(prefKey, false) != true
    }

    private fun arcadeItemAvailable(
        itemId: Int,
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
    ): Boolean = when (itemId) {
        FOLDER_JACKASS_PLUMBER ->
            FolderHolderAccessibility.hasFolderHolder(accessibleCount)
        in ARCADE_LOCKED_ITEMS ->
            prefs?.getBoolean("lockedItem$itemId", true) != true
        else -> true
    }

    private fun dreadsylvaniaItemAvailable(
        itemId: Int,
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
    ): Boolean = when (itemId) {
        TALES_OF_DREAD -> prefs?.getBoolean("itemBoughtPerCharacter6423", false) != true
        BRASS_DREAD_FLASK -> prefs?.getBoolean("itemBoughtPerCharacter6428", false) != true
        SILVER_DREAD_FLASK -> prefs?.getBoolean("itemBoughtPerCharacter6429", false) != true
        FOLDER_21 -> FolderHolderAccessibility.hasFolderHolder(accessibleCount)
        else -> true
    }

    private fun kiwiItemAvailable(itemId: Int, prefs: Preferences?): Boolean =
        when (itemId) {
            MINI_KIWI_INTOXICATING_SPIRITS ->
                prefs?.getBoolean("_miniKiwiIntoxicatingSpiritsBought", false) != true
            else -> true
        }

    private fun fixodentItemAvailable(itemId: Int, accessibleCount: (Int) -> Int): Boolean =
        when (itemId) {
            DENTADENT -> accessibleCount(MONODENT_OF_THE_SEA) > 0
            else -> true
        }

    private fun crimbo20ItemAvailable(itemId: Int, accessibleCount: (Int) -> Int): Boolean {
        if (itemId in CRIMBO20_ONE_TIME_ITEMS) {
            return accessibleCount(itemId) <= 0
        }
        return true
    }

    private fun shoreItemAvailable(
        itemId: Int,
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
    ): Boolean = when (itemId) {
        TOASTER -> prefs?.getBoolean("itemBoughtPerAscension637", false) != true
        UV_RESISTANT_COMPASS -> accessibleCount(UV_RESISTANT_COMPASS) <= 0
        else -> true
    }

    private fun pixelItemAvailable(
        itemId: Int,
        state: CharacterState,
        prefs: Preferences?,
    ): Boolean = when (itemId) {
        YELLOW_SUBMARINE -> !DesertBeachAccessibility.isAvailable(state, prefs)
        PIXEL_PILL, PIXEL_ENERGY_TANK, PIXEL_GRAPPLING_HOOK ->
            prefs?.getBoolean(CoinmasterShopSync.MYSTIC_PSYCHOSIS_ITEMS_UNLOCKED, false) == true
        else -> true
    }

    private fun starchartItemAvailable(itemId: Int, hasSkill: (Int) -> Boolean): Boolean =
        when (itemId) {
            STAR_SHIRT -> TorsoAwareness.hasTorsoAwareness(hasSkill)
            else -> true
        }

    private fun sugarsheetsItemAvailable(itemId: Int, hasSkill: (Int) -> Boolean): Boolean =
        when (itemId) {
            SUGAR_SHIRT -> TorsoAwareness.hasTorsoAwareness(hasSkill)
            else -> true
        }

    private fun jarlsbergItemAvailable(
        itemId: Int,
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
        hasSkill: (Int) -> Boolean,
    ): Boolean {
        when {
            itemId in JARLSBERG_BAKE_ITEMS -> if (!hasSkill(SKILL_BAKE)) return false
            itemId in JARLSBERG_BLEND_ITEMS -> if (!hasSkill(SKILL_BLEND)) return false
            itemId in JARLSBERG_BOIL_ITEMS -> if (!hasSkill(SKILL_BOIL)) return false
            itemId in JARLSBERG_CHOP_ITEMS -> if (!hasSkill(SKILL_CHOP)) return false
            itemId in JARLSBERG_CURDLE_ITEMS -> if (!hasSkill(SKILL_CURDLE)) return false
            itemId in JARLSBERG_FREEZE_ITEMS -> if (!hasSkill(SKILL_FREEZE)) return false
            itemId in JARLSBERG_FRY_ITEMS -> if (!hasSkill(SKILL_FRY)) return false
            itemId in JARLSBERG_GRILL_ITEMS -> if (!hasSkill(SKILL_GRILL)) return false
            itemId in JARLSBERG_SLICE_ITEMS -> if (!hasSkill(SKILL_SLICE)) return false
            itemId == COSMIC_SIX_PACK ->
                if (prefs?.getBoolean("_cosmicSixPackConjured", false) == true) return false
            itemId in JARLSBERG_STAFF_ITEMS ->
                if (accessibleCount(itemId) > 0) return false
        }
        return true
    }

    private fun swaggerItemAvailable(itemId: Int, prefs: Preferences?): Boolean {
        SWAGGER_SEASON_ITEMS[itemId]?.let { prefKey ->
            return prefs?.getBoolean(prefKey, false) == true
        }
        if (CoinmasterVisitInventory.hasVisited(CoinmasterVisitInventory.SWAGGER)) {
            return CoinmasterVisitInventory.containsItem(CoinmasterVisitInventory.SWAGGER, itemId)
        }
        return true
    }

    private fun crimbo17ItemAvailable(itemId: Int, state: CharacterState): Boolean =
        when (itemId) {
            MIME_SCIENCE_VOL_1 -> state.characterClassEnum == CharacterClass.SEAL_CLUBBER
            MIME_SCIENCE_VOL_2 -> state.characterClassEnum == CharacterClass.TURTLE_TAMER
            MIME_SCIENCE_VOL_3 -> state.characterClassEnum == CharacterClass.PASTAMANCER
            MIME_SCIENCE_VOL_4 -> state.characterClassEnum == CharacterClass.SAUCEROR
            MIME_SCIENCE_VOL_5 -> state.characterClassEnum == CharacterClass.DISCO_BANDIT
            MIME_SCIENCE_VOL_6 -> state.characterClassEnum == CharacterClass.ACCORDION_THIEF
            else -> true
        }

    private fun blackMarketItemAvailable(
        itemId: Int,
        state: CharacterState,
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
    ): Boolean {
        if (!blackMarketAvailable(state, prefs)) return false
        if (itemId == RED_ZEPPELIN_TICKET) {
            return accessibleCount(RED_ZEPPELIN_TICKET) <= 0
        }
        return true
    }

    private fun dimemasterItemAvailable(
        itemId: Int,
        state: CharacterState,
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
    ): Boolean {
        if (!dimemasterAccessible(prefs, accessibleCount)) return false
        if (itemId == PATCHOULI_OIL_BOMB || itemId == EXPLODING_HACKY_SACK) {
            return prefs?.getString("sidequestLighthouseCompleted", "none") == "hippy"
        }
        return true
    }

    private fun quartersmasterItemAvailable(
        itemId: Int,
        state: CharacterState,
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
    ): Boolean {
        if (!quartersmasterAccessible(prefs, accessibleCount)) return false
        if (itemId == TEQUILA_GRENADE || itemId == MOLOTOV_COCKTAIL_COCKTAIL) {
            return prefs?.getString("sidequestLighthouseCompleted", "none") == "fratboy"
        }
        return true
    }

    private fun blackMarketAvailable(state: CharacterState, prefs: Preferences?): Boolean {
        if (prefs?.getInt("lastWuTangDefeated", -1) == state.ascensionNumber) return false
        if (state.inNuclearAutumn) return false
        val progress = prefs?.getString(Quest.MACGUFFIN.prefKey, UNSTARTED) ?: UNSTARTED
        return progress == FINISHED || progress.contains("step")
    }

    private fun dimemasterAccessible(prefs: Preferences?, accessibleCount: (Int) -> Int): Boolean {
        if (prefs?.getString("warProgress", "unstarted") != "started") return false
        return hasWarHippyOutfit(accessibleCount)
    }

    private fun quartersmasterAccessible(prefs: Preferences?, accessibleCount: (Int) -> Int): Boolean {
        if (prefs?.getString("warProgress", "unstarted") != "started") return false
        return hasWarFratOutfit(accessibleCount)
    }

    private fun hasWarHippyOutfit(accessibleCount: (Int) -> Int): Boolean =
        accessibleCount(WAR_HIPPY_HEADBAND) > 0 &&
            accessibleCount(WAR_HIPPY_CORDS) > 0 &&
            accessibleCount(WAR_HIPPY_GLASSES) > 0

    private fun hasWarFratOutfit(accessibleCount: (Int) -> Int): Boolean =
        accessibleCount(WAR_FRAT_HELMET) > 0 &&
            accessibleCount(WAR_FRAT_PANTS) > 0 &&
            accessibleCount(WAR_FRAT_PIN) > 0
}
