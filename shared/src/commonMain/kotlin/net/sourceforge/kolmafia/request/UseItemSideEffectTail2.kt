package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.campground.CampgroundInventorySync
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.character.ZodiacSign
import net.sourceforge.kolmafia.data.TCRSDatabase
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.modifiers.StringModifier
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.SkillLearnFromResponse
import net.sourceforge.kolmafia.skill.SkillLearner
import net.sourceforge.kolmafia.skill.SkillManager

/**
 * Desktop [UseItemRequest.parseConsumption] remainder after fantasy-realm guest
 * through the end of the item switch, plus workshed and bedding installs
 * (Phases 7991–8050). Asdon fuel refresh and wardrobe description HTTP stay out.
 */
object UseItemSideEffectTail2 {

    private val FR_MAPS = mapOf(
        9873 to "frMountainsUnlocked",
        9874 to "frWoodUnlocked",
        9875 to "frSwampUnlocked",
        9876 to "frVillageUnlocked",
        9877 to "frCemetaryUnlocked",
    )
    private val MYSTERY_BOXES = 9739..9742
    private val BATTERIES = 10742..10744
    private val SOUPS = mapOf(
        11621 to "mp",
        11622 to "damage",
        11623 to "act",
        11624 to "hp",
        11625 to "stats",
    )
    private val BEANS = setOf(8866, 8867, 8868, 8869, 8870, 8871, 8872, 8873, 8875)
    private val HOLORECORDS = setOf(9109, 9110, 9111, 9112, 9113, 9114, 9115)
    private val CHESS_PIECES = setOf(10623, 10624, 10625, 10626, 10627, 10628, 10629, 10630, 10631, 10632, 10633, 10634)
    private val CHESS_TAKES = listOf(
        10623 to 1, 10624 to 1, 10625 to 2, 10626 to 2, 10627 to 2, 10628 to 8,
        10629 to 1, 10630 to 1, 10631 to 2, 10632 to 2, 10633 to 2, 10634 to 8,
    )
    private val RECIPES = setOf(
        10979, 10980, 10981, 10982, 10983, 10984, 10985, 10986, 10987,
        10993, 10994, 10995, 10996, 10997, 10999,
        3535, 3536, 3537, 3538, 3539, 3540, 3541,
        5657, 5658, 3745, 3747,
    )
    private val WORKSHEDS = setOf(
        6964, 6965, 6966, 6967, 7036, 7037, 7082, 7140, 7382, 8260, 9508, 10335, 10815, 11045, 11687,
    )
    private val BEDDING = setOf(429, 3345, 11345, 2638, 3344, 5888, 6338, 3348, 4842, 6890, 3347, 3346)

    private val BIRD = Regex("""Today's bird is the (.*?)!""")
    private val MEAT = Regex("""You gain ([\d,]+) Meat""")
    private const val SEEK_BIRD = 7323
    private const val CRIMBO_FIRST = 207
    private const val CRIMBO_LAST = 217

    fun apply(
        responseText: String,
        itemId: Int,
        count: Int,
        preferences: Preferences?,
        character: KoLCharacter?,
        inventory: InventoryManager?,
        skillManager: SkillManager? = null,
        familiarManager: FamiliarManager? = null,
    ): UseItemSideEffectSync.Result {
        val ctx = Tail(
            responseText, itemId, count, preferences, character, inventory, skillManager, familiarManager,
        )
        ctx.dispatch()
        return UseItemSideEffectSync.Result(ctx.outcome, ctx.message, ctx.extraConsumes.toList())
    }

    private class Tail(
        val html: String,
        val itemId: Int,
        val count: Int,
        val preferences: Preferences?,
        val character: KoLCharacter?,
        val inventory: InventoryManager?,
        val skillManager: SkillManager?,
        val familiarManager: FamiliarManager?,
    ) {
        var outcome = UseItemSideEffectSync.Outcome.UNHANDLED
        var message = ""
        val extraConsumes = mutableListOf<Pair<Int, Int>>()

        fun consume() { outcome = UseItemSideEffectSync.Outcome.CONSUME }
        fun keep() { outcome = UseItemSideEffectSync.Outcome.KEEP }
        fun take(id: Int, qty: Int = 1) { if (qty > 0) extraConsumes += id to qty }
        fun has(text: String) = html.contains(text, ignoreCase = true)
        fun prefBool(key: String, value: Boolean = true) { preferences?.setBoolean(key, value) }
        fun prefInt(key: String, value: Int) { preferences?.setInt(key, value) }
        fun prefStr(key: String, value: String) { preferences?.setString(key, value) }
        fun bump(key: String, delta: Int = 1, max: Int = 0) { preferences?.increment(key, delta, max) }
        fun owned(id: Int) = inventory?.getCount(id) ?: 0
        fun ascensions(): Int =
            character?.state?.value?.ascensionNumber ?: preferences?.getInt("knownAscensions", 0) ?: 0

        fun dispatch() {
            val fr = FR_MAPS[itemId]
            val soup = SOUPS[itemId]
            when {
                fr != null -> { prefBool(fr); consume() }
                itemId == 9937 -> if (has("You pick a cheese!")) consume() else keep()
                itemId == 9942 -> { prefBool("neverendingPartyAlways"); consume() }
                itemId == 11237 -> { prefBool("replicaNeverendingPartyAlways"); consume() }
                itemId == 9943 -> if (has("You're already invited to that party.")) keep() else { prefBool("_neverendingPartyToday"); consume() }
                itemId == 9961 -> highTops()
                itemId == 9989 -> { prefBool("voteAlways"); consume() }
                itemId == 9991 -> voterBallot()
                itemId == 10049 -> { prefBool("daycareOpen"); consume() }
                itemId == 10056 -> if (has("You already have access to the Boxing Daycare")) keep() else { prefBool("_daycareToday"); consume() }
                itemId == 7100 -> { bump("_jerksHealthMagazinesUsed", count); consume() }
                itemId == 10265 -> { prefBool("_etchedHourglassUsed"); consume() }
                itemId == 7188 -> handfulOfTips()
                itemId == 10187 -> { prefBool("prAlways"); if (has("escape to a realm of swashbuckling adventure")) consume() else keep() }
                itemId == 10188 -> if (has("You've already got access to PirateRealm.")) keep() else { prefBool("_prToday"); consume() }
                itemId == 10281 -> if (has("You don't need another beach comb")) keep() else consume()
                itemId == 10254 || itemId == 11242 -> moonSpoon()
                itemId == 10292 -> { prefBool("getawayCampsiteUnlocked"); if (has("you find a nice place to camp")) consume() else keep() }
                itemId == 7300 -> { prefBool("_sewingKitUsed"); consume() }
                itemId == 10434 -> birdCalendar()
                itemId == 10207 -> glitch()
                itemId in MYSTERY_BOXES -> if (has("This box can't be opened today")) keep() else consume()
                itemId == 4006 -> if (has("you ate some delicious, delicious amino acids")) { bump("aminoAcidsUsed", 1, 3); consume() } else keep()
                itemId == 10450 -> if (has("stomp it into goo")) consume() else keep()
                itemId == 10622 -> fancyChess()
                itemId in CHESS_PIECES -> chessPieces()
                itemId == 10640 -> seasoning()
                itemId == 10652 -> { prefBool("_cocoaDispenserUsed"); keep() }
                itemId == 10670 -> { if (has("You reach into the basket") || has("If you take anything else")) prefBool("_overflowingGiftBasketUsed"); keep() }
                itemId in BATTERIES -> { if (has("Your tongue crackles with electricity")) bump("shockingLickCharges", count); consume() }
                itemId == 10794 -> { if (has("You smear the caulk")) prefBool("wildfireBarrelCaulked"); consume() }
                itemId == 10795 -> { if (has("You smear the grease")) prefBool("wildfirePumpGreased"); consume() }
                itemId == 10878 -> { if (has("You put your hand under the spout") || has("day's allottment of free meatballs")) prefBool("_meatballMachineUsed"); keep() }
                itemId == 10879 -> { if (has("collect your fried air") || has("fryer needs to cool down")) prefBool("_airFryerUsed"); keep() }
                itemId == 10881 -> if (has("You're already feeling lucky, punk.")) keep() else consume()
                itemId == 10917 -> bookOfSkills()
                itemId == 8199 -> { prefBool("madnessBakeryAvailable"); consume() }
                itemId == 8187 -> { prefBool("overgrownLotAvailable"); consume() }
                itemId == 8158 -> { prefBool("skeletonStoreAvailable"); consume() }
                itemId == 11001 -> { prefBool("ownsSpeakeasy"); if (has("sign the deed")) consume() else keep() }
                itemId == 11003 -> { prefBool("_governmentPerDiemUsed"); if (has("collect your pay")) consume() else keep() }
                itemId in RECIPES -> recipe()
                itemId == 3580 -> { preferences?.let { QuestDatabase(it).setProgress(Quest.SEA_MONKEES, QuestDatabase.STARTED) }; consume() }
                itemId == 11046 -> crimboManual()
                itemId == 11068 -> { if (has("track down its owner")) bump("elfGratitude", count); consume() }
                itemId == 11061 -> trainbot()
                itemId == 11104 -> milestone()
                itemId == 11103 -> { prefBool("_lodestoneUsed"); keep() }
                itemId == 11106 -> { prefBool("_molehillMountainUsed"); keep() }
                itemId == 11109 -> { prefBool("_strangeStalagmiteUsed"); keep() }
                itemId == 11110 -> pingPong()
                itemId == 11116 -> { prefBool("_sitCourseCompleted"); keep() }
                itemId == 11197 -> { if (has("You open the Tome") || has("already read this book")) prefBool("_replicaSnowconeTomeUsed"); keep() }
                itemId == 11213 -> { if (has("You make a bunch of resolutions") || has("already made enough resolutions")) prefBool("_replicaResolutionLibramUsed"); keep() }
                itemId == 11219 -> { if (has("You read from The Smith") || has("smithed enough for today")) prefBool("_replicaSmithsTomeUsed"); keep() }
                itemId == 11231 -> { CampgroundInventorySync.setItem(preferences, 9033, 1); consume() }
                itemId == 11233 -> { prefBool("replicaWitchessSetAvailable"); consume() }
                itemId == 11257 || itemId == 11280 -> keep()
                itemId == 11268 -> { CampgroundInventorySync.setItem(preferences, 11268, 1); consume() }
                itemId == 11337 -> { prefBool("_mapToACandyRichBlockUsed"); consume() }
                itemId == 9967 -> partyFair("food")
                itemId == 9957 -> partyFair("booze")
                itemId == 11352 -> { prefBool("_tiedUpFlamingLeafletFought"); keep() }
                itemId == 11353 -> { prefBool("_tiedUpFlamingMonsteraFought"); keep() }
                itemId == 11354 -> { prefBool("_tiedUpLeaviathanFought"); keep() }
                itemId == 11390 -> keep()
                itemId == 11485 -> { prefBool("_snowballFactoryUsed"); consume() }
                itemId == 10325 -> lawOfAverages()
                itemId == 11582 -> { prefBool("_yamBatteryUsed"); consume() }
                itemId == 11616 -> flagellate()
                soup != null -> feedSoup(soup)
                itemId == 11644 -> { prefBool("_emberingHulkFought"); keep() }
                itemId == 11647 -> { prefBool("_structuralEmberUsed"); consume() }
                itemId == 11705 -> { prefBool("_pirateDinghyUsed"); prefInt("lastIslandUnlock", ascensions()); consume() }
                itemId == 11746 -> { prefBool("pumpkinSpiceWhorlUsed"); if (has("You can't add any more")) keep() else consume() }
                itemId == 11807 -> { prefBool("crAlways"); if (has("cut you a new key")) consume() else keep() }
                itemId == 11809 -> if (has("You've already got access to the server room.")) keep() else { prefBool("_crToday"); consume() }
                itemId == 11866 -> { bump("craftingPlansCharges"); consume() }
                itemId == 11956 -> clock()
                itemId == 11951 -> stockCertificate()
                itemId == 12192 -> { prefBool("_porkElfToiletriesKitUsed"); keep() }
                itemId == 12200 -> { prefBool("_giantGnawingBoneUsed"); keep() }
                itemId == 12205 -> { prefBool("_porkElfNetiPotUsed"); keep() }
                itemId == 12210 -> { prefBool("_fleekMascaraUsed"); keep() }
                itemId in BEANS -> { if (has("You acquire")) take(itemId); keep() }
                itemId in HOLORECORDS -> if (has("already a record")) keep() else consume()
                itemId in WORKSHEDS -> workshed()
                itemId in BEDDING -> bedding()
                else -> Unit
            }
        }

        private fun highTops() {
            when {
                has("pump up the high-tops") -> { bump("_highTopPumps"); bump("highTopPumped") }
                has("already pumped up") -> prefInt("_highTopPumps", 3)
            }
            consume()
        }

        private fun voterBallot() {
            if (has("You're already registered.") || has("You can't vote again today!")) keep()
            else { prefBool("_voteToday"); consume() }
        }

        private fun handfulOfTips() {
            if (has("a goon from the IRS")) {
                prefInt("handfulOfTipsMeat", 0)
            } else {
                val meat = MEAT.find(html)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()
                if (meat != null) bump("handfulOfTipsMeat", meat)
            }
            consume()
        }

        private fun moonSpoon() {
            if (has("You twist the spoon around") || has("You can't figure out the angle")) {
                prefBool("moonTuned")
            }
            val sign = UseItemRequestState.signFromLastUrl()?.let { ZodiacSign.find(it) }
            if (sign != null && !sign.isBadMoon) {
                val previous = ZodiacSign.find(character?.state?.value?.zodiacSign ?: "")
                val zoneChanged = previous == null ||
                    previous.isMuscle != sign.isMuscle ||
                    previous.isMysticality != sign.isMysticality ||
                    previous.isMoxie != sign.isMoxie
                character?.setZodiacSign(sign.signName)
                if (zoneChanged) {
                    preferences?.resetToDefault("_dailySpecial")
                    preferences?.resetToDefault("_dailySpecialPrice")
                }
                val state = character?.state?.value
                val prefs = preferences
                if (state != null && prefs != null && state.inTwoCrazyRandomSummer) {
                    TCRSDatabase.loadFromPreferences(state.className, sign.signName, prefs)
                    TCRSDatabase.resetModifiers(prefs, state.level)
                    TCRSDatabase.applyModifiers(state.level)
                }
            }
            keep()
        }

        private fun birdCalendar() {
            BIRD.find(html)?.groupValues?.getOrNull(1)?.let { prefStr("_birdOfTheDay", it) }
            prefBool("_canSeekBirds")
            val prefs = preferences
            if (prefs != null && prefs.getInt("skillLevel$SEEK_BIRD", 0) <= 0) {
                SkillLearner.learnSkill(SEEK_BIRD, prefs, skillManager, inventory, firstLearnOnly = true)
            }
            consume()
        }

        private fun glitch() {
            val (level, minimum) = when {
                has("needs even more than a ton more implementation") -> 6 to 69
                has("needs a ton more implementation") -> 5 to 37
                has("needs a lot more implementation") -> 4 to 11
                has("needs some more implementation") -> 3 to 4
                has("needs more implementation") -> 2 to 2
                has("needs implementation") -> 1 to 1
                has("Whoa") -> 7 to 111
                else -> 0 to 0
            }
            val prefs = preferences
            val counted = if (prefs?.getBoolean("_glitchItemImplemented", false) != true) {
                prefBool("_glitchItemImplemented")
                prefs?.increment("glitchItemImplementationCount") ?: 0
            } else {
                prefs.getInt("glitchItemImplementationCount", 0)
            }
            if (counted < minimum) prefInt("glitchItemImplementationCount", minimum)
            if ((prefs?.getInt("glitchItemImplementationLevel", 0) ?: 0) != level) {
                prefInt("glitchItemImplementationLevel", level)
            }
            keep()
        }

        private fun fancyChess() {
            if (has("You don't have time for a game of chess right now")) {
                keep()
                return
            }
            if (has("You sit down at the chessboard") || has("Your poor heart can't handle")) {
                prefBool("_fancyChessSetUsed")
            }
            keep()
        }

        private fun chessPieces() {
            if (has("assemble a complete chess set")) {
                for ((id, qty) in CHESS_TAKES) take(id, qty)
            }
            keep()
        }

        private fun seasoning() {
            if (has("You rip open your packet") || has("You can't seem to rip the packet open")) {
                prefBool("universalSeasoningActive")
                bump("_universalSeasoningsUsed")
            }
            keep()
        }

        private fun bookOfSkills() {
            prefBool("_bookOfEverySkillUsed")
            val prefs = preferences
            if (prefs != null && has("turn to a random page")) {
                SkillLearnFromResponse.learnSkillFromResponse(html, prefs, skillManager, inventory)
            }
            keep()
        }

        private fun recipe() {
            if (!has("You learn to craft a new item")) {
                learnNamedRecipe()
                keep()
            } else {
                consume()
            }
        }

        private fun learnNamedRecipe() {
            val itemName = ItemDatabase.getItemName(itemId)
            if (itemName.isBlank()) return
            val recipeName = ModifierDatabase.getStringModifier(itemName, StringModifier.RECIPE).trim()
            if (recipeName.isBlank()) return
            val recipeId = ItemDatabase.getByName(recipeName)?.id ?: return
            if (preferences?.getBoolean("unknownRecipe$recipeId", false) == true) {
                prefBool("unknownRecipe$recipeId", false)
                ConcoctionDatabase.markRefreshNeeded()
            }
        }

        private fun crimboManual() {
            val prefs = preferences
            if (has("You acquire a skill")) {
                if (prefs != null) {
                    val skillId = SkillLearnFromResponse.learnSkillFromResponse(html, prefs, skillManager, inventory)
                    if (skillId in CRIMBO_FIRST..CRIMBO_LAST) {
                        prefInt("crimboTrainingSkill", skillId - CRIMBO_FIRST + 1)
                    }
                }
                prefBool("_crimboTraining", false)
                keep()
                return
            }
            prefBool("_crimboTraining", has("You've already trained somebody today"))
            keep()
        }

        private fun trainbot() {
            if (has("It must need some raw materials")) {
                keep()
                return
            }
            if (has("reassembles your pile of Trainbot slag")) take(11076)
            consume()
        }

        private fun milestone() {
            if (has("You don't know what desert this milestone is for")) {
                keep()
                return
            }
            if (has("you quickly explore part of the desert")) bump("desertExploration", 5, 100)
            consume()
        }

        private fun pingPong() {
            if (has("You've tempted fate")) prefInt("_chocolateCoveredPingPongBallsUsed", 3)
            else bump("_chocolateCoveredPingPongBallsUsed", 1, 3)
            consume()
        }

        private fun partyFair(quest: String) {
            val prefs = preferences
            if (prefs != null &&
                prefs.getString("_questPartyFairQuest", "") == quest &&
                prefs.getString("_questPartyFairProgress", "").isNotEmpty()
            ) {
                bump("_questPartyFairItemsOpened", 1, 11)
            }
            consume()
        }

        private fun lawOfAverages() {
            if (has("You already feel pretty average")) {
                val current = preferences?.getInt("_lawOfAveragesUsed", 0) ?: 0
                prefInt("_lawOfAveragesUsed", maxOf(current, owned(itemId)))
            } else {
                bump("_lawOfAveragesUsed")
            }
            keep()
        }

        private fun flagellate() {
            if (has("You get your flagellate flagon ready for your next drink.")) bump("flagellateFlagonsActive")
            prefBool("_flagellateFlagonUsed")
            keep()
        }

        private fun feedSoup(attribute: String) {
            val familiarId = character?.state?.value?.familiarId ?: 0
            familiarManager?.incrementSoup(familiarId, attribute)
            consume()
        }

        private fun clock() {
            if (has("You don't have time")) {
                prefInt("_clocksUsed", 2)
                keep()
            } else {
                bump("_clocksUsed", 1, 2)
                consume()
            }
        }

        private fun stockCertificate() {
            val turns = preferences?.getString("stockCertificateTurns", "") ?: ""
            val comma = turns.indexOf(',')
            if (comma >= 0) prefStr("stockCertificateTurns", turns.substring(comma + 1))
            consume()
        }

        private fun workshed() {
            if (CampgroundItemSync.currentWorkshedItemId(preferences) > 0) {
                prefBool("_workshedItemUsed")
            }
            if (has("already rearranged your workshed")) {
                keep()
                return
            }
            CampgroundItemSync.setCurrentWorkshedItem(preferences, itemId)
            CampgroundInventorySync.setItem(preferences, itemId, 1)
            if (itemId == CampgroundItemSync.ASDON_MARTIN_ID) {
                UseItemRequestState.markWorkshedRefresh()
            }
            consume()
        }

        private fun bedding() {
            if (has("You've already got") || has("You don't have")) {
                keep()
                return
            }
            CampgroundItemSync.setCurrentBed(preferences, itemId)
            consume()
        }
    }
}
