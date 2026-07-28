package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState
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
            else -> true
        }
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
        val prefKey = SWAGGER_SEASON_ITEMS[itemId] ?: return true
        return prefs?.getBoolean(prefKey, false) == true
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
