package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.ZodiacSign
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.KolGameHolidayCalendar
import net.sourceforge.kolmafia.data.NpcStoreData
import net.sourceforge.kolmafia.data.OutfitDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase.Companion.FINISHED
import net.sourceforge.kolmafia.quest.QuestDatabase.Companion.STARTED
import net.sourceforge.kolmafia.quest.QuestDatabase.Companion.UNSTARTED
import net.sourceforge.kolmafia.quest.QuestDatabase.Companion.stepOrdinal
import net.sourceforge.kolmafia.request.StandardRequest
import net.sourceforge.kolmafia.data.RestrictedItemType

/** Desktop NPCStoreDatabase.canPurchase shop/item gates (meat affordability ignored). */
object NpcPurchaseAccessibility {

    private const val LAB_KEY = 339
    private const val SPARE_KIDNEY = 2718
    private const val FORGED_ID_DOCUMENTS = 2064
    private const val STRANGE_GOGGLES = 6118
    private const val SUSPICIOUS_JAR = 5898
    private const val FEDORA_MOUNTED_FOUNTAIN = 10762
    private const val PORKPIE_MOUNTED_POPPER = 10763
    private const val SOMBRERO_MOUNTED_SPARKLER = 10764
    private const val ROCKET_BOOTS = 10771
    private const val OVERSIZED_SPARKLER = 10772
    private const val DOC_VITALITY_SERUM = 8202
    private const val MAYO_CLINIC = 8260
    private const val MIRACLE_WHIP = 8266
    private const val SPHYGMAYOMANOMETER = 8267
    private const val REFLEX_HAMMER = 8268
    private const val MAYO_LANCE = 8269
    private const val DINGY_DINGHY = 141
    private const val SKIFF = 5885
    private const val YELLOW_SUBMARINE = 8376
    private const val HIPPY_HAT = 214
    private const val HIPPY_PANTS = 213
    private const val WAR_HIPPY_HEADBAND = 2337
    private const val WAR_HIPPY_CORDS = 2032
    private const val WAR_HIPPY_GLASSES = 2033
    private const val WAR_FRAT_HELMET = 2069
    private const val WAR_FRAT_PANTS = 2070
    private const val WAR_FRAT_PIN = 2353
    private const val DOWN_THE_RABBIT_HOLE_EFFECT = 725
    private const val TRICK_TOT_FAMILIAR = 206
    private const val TRICK_TOT_CANDY = 9139
    private const val TRICK_TOT_EYEBALL = 9144
    private const val TRICK_TOT_KNIGHT = 9137
    private const val TRICK_TOT_ROBOT = 9143
    private const val TRICK_TOT_UNICORN = 9138
    private const val TRICK_TOT_LIBERTY = 9145
    private const val FISHING_POLE = 9007
    private const val FISHING_HAT = 9011
    private const val PIRATE_FLEDGES = 3033
    private const val SWASHBUCKLING_GETUP = 9
    private val SWASHBUCKLING_PIECE_IDS = intArrayOf(224, 402, 403)
    private val PIRATE_EPHEMERA_REGEX = Regex("pirate (?:brochure|pamphlet|tract)", RegexOption.IGNORE_CASE)

    fun canPurchaseIgnoringMeat(
        itemId: Int,
        store: NpcStoreData,
        state: CharacterState,
        prefs: Preferences? = null,
        accessibleCount: (Int) -> Int = { 0 },
        hasActiveEffect: (Int) -> Boolean = { false },
        familiarUsable: (Int) -> Boolean = { false },
    ): Boolean {
        if (isSavageBeast(prefs)) return false
        return shopItemAvailable(
            store.storeKey,
            store.storeName,
            itemId,
            state,
            prefs,
            accessibleCount,
            hasActiveEffect,
            familiarUsable,
        )
    }

    private fun isSavageBeast(prefs: Preferences?): Boolean =
        prefs?.getString("_savageBeastMods", "")?.isNotEmpty() == true

    private fun inBadMoon(state: CharacterState): Boolean =
        ZodiacSign.find(state.zodiacSign)?.isBadMoon == true

    private fun guildStoreOpen(state: CharacterState, prefs: Preferences?): Boolean {
        if (state.inNuclearAutumn || state.inPokefam) return false
        return prefs?.getInt("lastGuildStoreOpen", -1) == state.ascensionNumber
    }

    private fun dispensaryOpen(state: CharacterState, prefs: Preferences?, accessibleCount: (Int) -> Int): Boolean {
        if (prefs?.getInt("lastDispensaryOpen", -1) != state.ascensionNumber) return false
        return accessibleCount(LAB_KEY) > 0
    }

    private fun shopItemAvailable(
        storeKey: String,
        storeName: String,
        itemId: Int,
        state: CharacterState,
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
        hasActiveEffect: (Int) -> Boolean,
        familiarUsable: (Int) -> Boolean,
    ): Boolean {
        return when (storeKey.lowercase()) {
            "guildstore1" -> {
                state.characterClassEnum.isMoxieBased && guildStoreOpen(state, prefs)
            }
            "guildstore2" -> {
                (state.characterClassEnum.isMysticality ||
                    (state.characterClassEnum == CharacterClass.ACCORDION_THIEF && state.level >= 9)) &&
                    guildStoreOpen(state, prefs)
            }
            "guildstore3" -> {
                ((state.characterClassEnum.isMuscleBased && !state.isAxecore) ||
                    (state.characterClassEnum == CharacterClass.ACCORDION_THIEF && state.level >= 9)) &&
                    guildStoreOpen(state, prefs)
            }
            "hiddentavern" ->
                prefs?.getInt("hiddenTavernUnlock", -1) == state.ascensionNumber
            "jewelers" ->
                !state.inZombiecore && canadiaAvailable(state)
            "knobdisp" ->
                dispensaryOpen(state, prefs, accessibleCount)
            "doc" -> docStoreAvailable(itemId, state, prefs)
            "bugbear" -> !state.inNuclearAutumn
            "chateau" ->
                prefs?.getBoolean("chateauAvailable", false) == true
            "blackmarket" -> {
                if (!blackMarketAvailable(state, prefs)) return false
                when (itemId) {
                    SPARE_KIDNEY -> inBadMoon(state) && accessibleCount(SPARE_KIDNEY) <= 0
                    FORGED_ID_DOCUMENTS -> forgedIdDocumentsAvailable(prefs)
                    else -> true
                }
            }
            "wildfire" -> {
                if (!state.isFirecore) return false
                when (itemId) {
                    else -> true
                }
            }
            "hippy" -> hippyStoreAvailable(storeName, state, prefs, accessibleCount)
            "chinatown" -> chinatownAvailable(accessibleCount)
            "cyber_hackmarket" ->
                prefs?.getBoolean("crAlways", false) == true ||
                    prefs?.getBoolean("_crToday", false) == true
            "fwshop" -> fireworksShopAvailable(itemId, prefs)
            "generalstore" -> generalStoreItemAvailable(
                itemId, state, prefs, accessibleCount, familiarUsable,
            )
            "gnoll" -> knollAvailable(state)
            "gnomart" -> !state.inZombiecore && gnomadsAvailable(state)
            "madeline" -> isQuestFinished(prefs, Quest.ARMORER)
            "mayoclinic" -> mayoClinicItemAvailable(itemId, state, prefs)
            "meatsmith" ->
                !state.inZombiecore && !state.inNuclearAutumn && !state.isKingdomOfExploathing
            "nerve" -> inBadMoon(state)
            "sandpenny" -> state.inSeaPath
            "whitecitadel" -> whiteCitadelAvailable(prefs)
            "town_giftshop.php" -> giftShopItemAvailable(itemId, state)
            "tweedle" -> hasActiveEffect(DOWN_THE_RABBIT_HOLE_EFFECT)
            "unclep" -> !state.inZombiecore && !state.inNuclearAutumn
            "vault1" -> vault1ItemAvailable(itemId, state, prefs, familiarUsable)
            "vault2" -> vault2ItemAvailable(itemId, state, prefs, familiarUsable)
            "vault3" -> vault3ItemAvailable(itemId, state, prefs, familiarUsable)
            "armory" -> armoryItemAvailable(itemId, state, accessibleCount)
            "bartender" -> bartenderAvailable(state, prefs)
            "bartlebys" -> bartlebysItemAvailable(
                storeName, itemId, state, prefs, accessibleCount,
            )
            "fdkol" -> false
            else -> when {
                storeKey.startsWith("crimbo") -> false
                else -> true
            }
        }
    }

    private fun docStoreAvailable(itemId: Int, state: CharacterState, prefs: Preferences?): Boolean {
        if (state.inZombiecore || state.inNuclearAutumn || state.isKingdomOfExploathing) {
            return false
        }
        if (itemId == DOC_VITALITY_SERUM) {
            return isQuestFinished(prefs, Quest.DOC)
        }
        return true
    }

    private fun generalStoreItemAvailable(
        itemId: Int,
        state: CharacterState,
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
        familiarUsable: (Int) -> Boolean,
    ): Boolean {
        if (state.inNuclearAutumn) return false
        val holiday = KolGameHolidayCalendar.getHoliday()
        return when (itemId) {
            3128 -> holiday.contains("Yuletide") // marshmallow
            1080 -> holiday.contains("Oyster Egg Day")
            2945 -> holiday.contains("Festival of Jarlsberg")
            2681, 2680, 2679 -> holiday.contains("Dependence Day")
            9827 -> holiday.contains("Dependence Day") && holiday.contains("St. Sneaky Pete's Day")
            3235, 3233, 3234 -> holiday.contains("Generic Summer Holiday")
            4770 -> !desertBeachAccessible(prefs, state, accessibleCount)
            6618, 6619, 6620 -> folderHolderAvailable(state, accessibleCount)
            7711, 7645, 7643, 7644 -> state.isRaincore
            9010 -> accessibleCount(9007) > 0 // fishing line needs pole
            TRICK_TOT_UNICORN, TRICK_TOT_CANDY -> familiarUsable(TRICK_TOT_FAMILIAR)
            3582 -> state.inSeaPath
            else -> true
        }
    }

    private fun giftShopItemAvailable(itemId: Int, state: CharacterState): Boolean {
        if (inBadMoon(state) || state.isKingdomOfExploathing) return false
        val holiday = KolGameHolidayCalendar.getHoliday()
        val asc = state.ascensionNumber
        return when (itemId) {
            1026, 2330, 2331, 2332, 2333 ->
                holiday.contains("Valentine's Day")
            1180, 1202 -> asc >= 1
            1191, 1213 -> asc >= 2
            1181, 1203 -> asc >= 4
            1192, 1214 -> asc >= 5
            1182, 1204 -> asc >= 7
            1193, 1215 -> asc >= 8
            1183, 1205 -> asc >= 10
            1194, 1216 -> asc >= 11
            1184, 1206 -> asc >= 13
            1195, 1217 -> asc >= 14
            1185, 1207 -> asc >= 16
            1196, 1218 -> asc >= 17
            1186, 1208 -> asc >= 19
            1197, 1219 -> asc >= 20
            1187, 1209 -> asc >= 22
            1198, 1220 -> asc >= 23
            1188, 1210 -> asc >= 25
            1199, 1221 -> asc >= 26
            else -> true
        }
    }

    private fun vault1ItemAvailable(
        itemId: Int,
        state: CharacterState,
        prefs: Preferences?,
        familiarUsable: (Int) -> Boolean,
    ): Boolean {
        if (!state.inNuclearAutumn) return false
        if ((prefs?.getInt("falloutShelterLevel", 0) ?: 0) < 2) return false
        return when (itemId) {
            TRICK_TOT_CANDY, TRICK_TOT_EYEBALL -> familiarUsable(TRICK_TOT_FAMILIAR)
            else -> true
        }
    }

    private fun vault2ItemAvailable(
        itemId: Int,
        state: CharacterState,
        prefs: Preferences?,
        familiarUsable: (Int) -> Boolean,
    ): Boolean {
        if (!state.inNuclearAutumn) return false
        if ((prefs?.getInt("falloutShelterLevel", 0) ?: 0) < 4) return false
        return when (itemId) {
            TRICK_TOT_KNIGHT, TRICK_TOT_ROBOT -> familiarUsable(TRICK_TOT_FAMILIAR)
            else -> true
        }
    }

    private fun vault3ItemAvailable(
        itemId: Int,
        state: CharacterState,
        prefs: Preferences?,
        familiarUsable: (Int) -> Boolean,
    ): Boolean {
        if (!state.inNuclearAutumn) return false
        if ((prefs?.getInt("falloutShelterLevel", 0) ?: 0) < 7) return false
        return when (itemId) {
            TRICK_TOT_LIBERTY, TRICK_TOT_UNICORN -> familiarUsable(TRICK_TOT_FAMILIAR)
            else -> true
        }
    }

    private fun armoryItemAvailable(
        itemId: Int,
        state: CharacterState,
        accessibleCount: (Int) -> Int,
    ): Boolean {
        if (state.inZombiecore || state.inNuclearAutumn || state.isKingdomOfExploathing) {
            return false
        }
        if (itemId == FISHING_HAT) {
            return accessibleCount(FISHING_POLE) > 0
        }
        return true
    }

    private fun bartenderAvailable(state: CharacterState, prefs: Preferences?): Boolean {
        if (state.inZombiecore) return false
        return isQuestLaterThan(prefs, Quest.RAT, STARTED)
    }

    private fun bartlebysStoreNameMatches(state: CharacterState, storeName: String): Boolean {
        val expected = if (state.inBeecore) {
            "Barrrtleby's Barrrgain Books (Bees Hate You)"
        } else {
            "Barrrtleby's Barrrgain Books"
        }
        return storeName == expected
    }

    private fun bartlebysItemAvailable(
        storeName: String,
        itemId: Int,
        state: CharacterState,
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
    ): Boolean {
        if (!bartlebysStoreNameMatches(state, storeName)) return false
        if (pirateEphemeraBlocked(itemId, prefs, state)) return false
        return hasOutfitPieces(SWASHBUCKLING_GETUP, accessibleCount) ||
            accessibleCount(PIRATE_FLEDGES) > 0
    }

    private fun pirateEphemeraBlocked(
        itemId: Int,
        prefs: Preferences?,
        state: CharacterState,
    ): Boolean {
        val itemName = ItemDatabase.getById(itemId)?.name ?: return false
        if (!PIRATE_EPHEMERA_REGEX.containsMatchIn(itemName)) return false
        val reset = prefs?.getInt("lastPirateEphemeraReset", -1) ?: -1
        if (reset != state.ascensionNumber) return false
        val last = prefs?.getString("lastPirateEphemera", "") ?: ""
        return last.isNotEmpty() && !last.equals(itemName, ignoreCase = true)
    }

    private fun hasOutfitPieces(outfitId: Int, accessibleCount: (Int) -> Int): Boolean {
        val outfit = OutfitDatabase.getById(outfitId)
        if (outfit != null) {
            return outfit.equipment.all { pieceName ->
                val pieceId = ItemDatabase.getByName(pieceName)?.id ?: return false
                accessibleCount(pieceId) > 0
            }
        }
        if (outfitId == SWASHBUCKLING_GETUP) {
            return SWASHBUCKLING_PIECE_IDS.all { accessibleCount(it) > 0 }
        }
        return false
    }

    private fun isQuestLaterThan(prefs: Preferences?, quest: Quest, step: String): Boolean {
        val current = prefs?.getString(quest.prefKey, UNSTARTED) ?: UNSTARTED
        return stepOrdinal(current) > stepOrdinal(step)
    }

    private fun folderHolderAvailable(state: CharacterState, accessibleCount: (Int) -> Int): Boolean {
        if (accessibleCount(6617) > 0) return true
        if (state.inLegacyOfLoathing && accessibleCount(11220) > 0) return true
        return false
    }

    private fun desertBeachAccessible(
        prefs: Preferences?,
        state: CharacterState,
        accessibleCount: (Int) -> Int,
    ): Boolean {
        if (prefs?.getInt("lastDesertUnlock", -1) == state.ascensionNumber) return true
        return accessibleCount(4770) > 0
    }

    private fun mayoClinicItemAvailable(itemId: Int, state: CharacterState, prefs: Preferences?): Boolean {
        if (!CampgroundItemSync.hasWorkshedItem(prefs, MAYO_CLINIC)) return false
        if (!StandardRequest.isAllowed(RestrictedItemType.ITEMS, "portable Mayo Clinic", state)) {
            return false
        }
        if (itemId == MIRACLE_WHIP) {
            return prefs?.getBoolean("_mayoDeviceRented", false) != true &&
                prefs?.getBoolean("itemBoughtPerAscension8266", false) != true
        }
        if (itemId == SPHYGMAYOMANOMETER || itemId == REFLEX_HAMMER || itemId == MAYO_LANCE) {
            return prefs?.getBoolean("_mayoDeviceRented", false) != true
        }
        return true
    }

    private fun hippyStoreAvailable(
        storeName: String,
        state: CharacterState,
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
    ): Boolean {
        if (state.isKingdomOfExploathing) return false
        val ascension = state.ascensionNumber
        val lastFilthClearance = prefs?.getInt("lastFilthClearance", -1) ?: -1

        if (storeName.contains("Pre-War", ignoreCase = true)) {
            if (lastFilthClearance == ascension) return false
            if (!mysteriousIslandAccessible(state, prefs, accessibleCount)) return false
            if (!hasHippyOutfit(accessibleCount)) return false
            if (state.level < 12) return true
            return isHippyStoreAvailable(prefs)
        }

        if (lastFilthClearance != ascension) return false
        val currentStore = prefs?.getString("currentHippyStore", "none") ?: "none"
        val storeOpen = isHippyStoreAvailable(prefs)
        return when {
            storeName.contains("(Fratboy)", ignoreCase = true) ->
                currentStore.equals("fratboy", ignoreCase = true) &&
                    (storeOpen || hasWarFratOutfit(accessibleCount))
            storeName.contains("(Hippy)", ignoreCase = true) ->
                currentStore.equals("hippy", ignoreCase = true) &&
                    (storeOpen || hasWarHippyOutfit(accessibleCount))
            else -> false
        }
    }

    private fun mysteriousIslandAccessible(
        state: CharacterState,
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
    ): Boolean {
        if (prefs?.getInt("lastIslandUnlock", -1) == state.ascensionNumber) return true
        if (accessibleCount(DINGY_DINGHY) > 0) return true
        if (accessibleCount(SKIFF) > 0) return true
        if (accessibleCount(YELLOW_SUBMARINE) > 0) return true
        if (isQuestFinished(prefs, Quest.HIPPY)) return true
        return false
    }

    private fun hasHippyOutfit(accessibleCount: (Int) -> Int): Boolean =
        accessibleCount(HIPPY_HAT) > 0 && accessibleCount(HIPPY_PANTS) > 0

    private fun hasWarHippyOutfit(accessibleCount: (Int) -> Int): Boolean =
        accessibleCount(WAR_HIPPY_HEADBAND) > 0 &&
            accessibleCount(WAR_HIPPY_CORDS) > 0 &&
            accessibleCount(WAR_HIPPY_GLASSES) > 0

    private fun hasWarFratOutfit(accessibleCount: (Int) -> Int): Boolean =
        accessibleCount(WAR_FRAT_HELMET) > 0 &&
            accessibleCount(WAR_FRAT_PANTS) > 0 &&
            accessibleCount(WAR_FRAT_PIN) > 0

    private fun isHippyStoreAvailable(prefs: Preferences?): Boolean {
        val progress = prefs?.getString(Quest.ISLAND_WAR.prefKey, UNSTARTED) ?: UNSTARTED
        return progress != "step1" && progress != UNSTARTED
    }

    private fun knollAvailable(state: CharacterState): Boolean {
        if (state.isKingdomOfExploathing) return false
        val sign = ZodiacSign.find(state.zodiacSign) ?: return false
        return sign == ZodiacSign.MONGOOSE ||
            sign == ZodiacSign.WALLABY ||
            sign == ZodiacSign.VOLE
    }

    private fun gnomadsAvailable(state: CharacterState): Boolean {
        val sign = ZodiacSign.find(state.zodiacSign) ?: return false
        return sign == ZodiacSign.WOMBAT ||
            sign == ZodiacSign.BLENDER ||
            sign == ZodiacSign.PACKRAT
    }

    private fun whiteCitadelAvailable(prefs: Preferences?): Boolean {
        val progress = prefs?.getString(Quest.CITADEL.prefKey, UNSTARTED) ?: UNSTARTED
        return progress == FINISHED || progress == "step5" || progress == "step6"
    }

    private fun isQuestFinished(prefs: Preferences?, quest: Quest): Boolean =
        prefs?.getString(quest.prefKey, UNSTARTED) == FINISHED

    private fun chinatownAvailable(accessibleCount: (Int) -> Int): Boolean =
        accessibleCount(STRANGE_GOGGLES) > 0 && accessibleCount(SUSPICIOUS_JAR) > 0

    private fun fireworksShopAvailable(itemId: Int, prefs: Preferences?): Boolean {
        if (prefs?.getBoolean("_fireworksShop", false) != true) return false
        return when (itemId) {
            FEDORA_MOUNTED_FOUNTAIN,
            PORKPIE_MOUNTED_POPPER,
            SOMBRERO_MOUNTED_SPARKLER,
            -> prefs.getBoolean("_fireworksShopHatBought", false) != true
            ROCKET_BOOTS,
            OVERSIZED_SPARKLER,
            -> prefs.getBoolean("_fireworksShopEquipmentBought", false) != true
            else -> true
        }
    }

    private fun canadiaAvailable(state: CharacterState): Boolean {
        if (state.isKingdomOfExploathing) return false
        val sign = ZodiacSign.find(state.zodiacSign) ?: return false
        return sign == ZodiacSign.BLENDER || sign == ZodiacSign.PACKRAT || sign == ZodiacSign.VOLE
    }

    private fun blackMarketAvailable(state: CharacterState, prefs: Preferences?): Boolean {
        if (prefs?.getInt("lastWuTangDefeated", -1) == state.ascensionNumber) return false
        val progress = prefs?.getString(Quest.MACGUFFIN.prefKey, UNSTARTED) ?: UNSTARTED
        return progress == FINISHED || progress.contains("step")
    }

    private fun forgedIdDocumentsAvailable(prefs: Preferences?): Boolean {
        val progress = prefs?.getString(Quest.MACGUFFIN.prefKey, UNSTARTED) ?: UNSTARTED
        return stepOrdinal(progress) <= stepOrdinal("step1")
    }
}
