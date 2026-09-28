package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.campground.CampgroundInventorySync
import net.sourceforge.kolmafia.campground.GardenCropAvailability
import net.sourceforge.kolmafia.campground.GardenCropIds
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DesertVisitSync
import net.sourceforge.kolmafia.quest.ProtonicGhostSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.DreadScrollManager

/**
 * Desktop [UseItemRequest.parseConsumption] remainder after the 7871–7930 slice
 * (Phases 7931–7990). Legion unscrew, jacking fruit, and the island kill-count
 * follow-up come from [UseItemRequestState].
 */
object UseItemSideEffectTail {

    private const val SCREWDRIVER = 4926
    private const val DOUGH = 159
    private const val FLAT_DOUGH = 301
    private const val ROLLING_PIN = 873
    private const val UNROLLING_PIN = 874
    private const val PLUS_SIGN = 818
    private const val RANGE = 157
    private const val OVEN = 4707
    private const val EXPRESS_CARD = 1687
    private const val JACKING_MAP = 4560
    private const val CHRONER = 7567
    private const val MERKIN_CHEATSHEET = 4204
    private const val MERKIN_LOCKKEY = 3810
    private const val SPARE_CHOCOLATE_PARTS = 9240
    private const val PROTON_ACCELERATOR = 9082
    private const val TALL_GRASS = GardenCropIds.TALL_GRASS_SEEDS

    private val LEGION = 4908..4928
    private val TRIVIA = 5511..5514
    private val GIFTS = setOf(1167, 1168, 1169, 1170, 1171, 1172, 1173, 1174, 1175, 1176, 1177, 1460, 1534, 2683, 3430)
    private val SAUCE = setOf(3561, 3562, 3563, 3693, 3694)
    private val CUPCAKES = 1624..1628
    private val LOVE_SONGS = 3754..3759
    private val PUNCTUATION = 4552..4558
    private val MARKS = 6434..6439
    private val LINTS = 6877..6880
    private val BEDDING = 6886..6889
    private val XIBLAXIAN = 7743..7749
    private val CRIMBOT = 7865..7904
    private val MAYO = mapOf(
        8261 to "Mayonex",
        8262 to "Mayodiol",
        8263 to "Mayostat",
        8264 to "Mayozapine",
        8265 to "Mayoflex",
    )
    private val STEAM = listOf(
        intArrayOf(7406, 7407, 7408),
        intArrayOf(7409, 7410, 7411),
        intArrayOf(7412, 7413, 7414),
        intArrayOf(7415, 7416, 7417),
        intArrayOf(7418, 7419, 7420),
    )
    private val STACK_CHIPS = mapOf(9040 to "sourceTerminalPram", 9041 to "sourceTerminalGram", 9042 to "sourceTerminalSpam")
    private val NAMED_CHIPS = mapOf(
        9043 to "CRAM",
        9044 to "DRAM",
        9045 to "TRAM",
        9046 to "INGRAM",
        9047 to "DIAGRAM",
        9048 to "ASHRAM",
        9049 to "SCRAM",
        9050 to "TRIGRAM",
    )
    private val TERMINAL_FILES = mapOf(
        9051 to "substats.enh",
        9052 to "damage.enh",
        9053 to "critical.enh",
        9054 to "protect.enq",
        9055 to "stats.enq",
        9056 to "compress.edu",
        9057 to "duplicate.edu",
        9058 to "portscan.edu",
        9059 to "turbo.edu",
        9060 to "familiar.ext",
        9061 to "pram.ext",
        9062 to "gram.ext",
        9063 to "spam.ext",
        9064 to "cram.ext",
        9065 to "dram.ext",
        9066 to "tram.ext",
    )

    private val WORDQUIZ = Regex("""Your Mer-kin vocabulary mastery is now at <b>(\d*?)%</b>""")
    private val CHIP_COUNT = Regex("""You have (\d+) so far""")
    private val RHYTHM = Regex("""You join the (.*?) other people""")
    private val FLUENCY = Regex("""Fluency is now (\d+)%""")

    fun apply(
        responseText: String,
        itemId: Int,
        count: Int,
        preferences: Preferences?,
        character: KoLCharacter?,
        inventory: InventoryManager?,
    ): UseItemSideEffectSync.Result {
        val ctx = Tail(responseText, itemId, count, preferences, character, inventory)
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
    ) {
        var outcome = UseItemSideEffectSync.Outcome.UNHANDLED
        var message = ""
        val extraConsumes = mutableListOf<Pair<Int, Int>>()

        fun consume() { outcome = UseItemSideEffectSync.Outcome.CONSUME }
        fun keep() { outcome = UseItemSideEffectSync.Outcome.KEEP }
        fun abort(text: String) {
            outcome = UseItemSideEffectSync.Outcome.ABORT
            message = text
        }
        fun take(id: Int, qty: Int = 1) {
            if (qty > 0) extraConsumes += id to qty
        }
        fun note(text: String) { UseItemConsumptionSync.setLastUpdateForTest(text) }
        fun has(text: String) = html.contains(text, ignoreCase = true)
        fun prefBool(key: String, value: Boolean = true) { preferences?.setBoolean(key, value) }
        fun prefInt(key: String, value: Int) { preferences?.setInt(key, value) }
        fun prefStr(key: String, value: String) { preferences?.setString(key, value) }
        fun bump(key: String, delta: Int = 1, max: Int = 0) { preferences?.increment(key, delta, max) }
        fun owned(id: Int) = inventory?.getCount(id) ?: 0
        fun quests() = preferences?.let { QuestDatabase(it) }
        fun ascensions(): Int =
            character?.state?.value?.ascensionNumber ?: preferences?.getInt("knownAscensions", 0) ?: 0

        fun appendCsv(pref: String, token: String) {
            val known = preferences?.getString(pref, "") ?: return
            if (known.contains(token)) return
            preferences.setString(pref, if (known.isEmpty()) token else "$known,$token")
        }

        fun dispatch() {
            val steam = STEAM.firstOrNull { itemId in it }
            when {
                itemId in LEGION || itemId == SCREWDRIVER -> legion()
                itemId in TRIVIA -> if (has("Answer:")) consume() else keep()
                itemId in GIFTS -> gifts()
                itemId in SAUCE -> if (has("you'd explode")) abort("You're already under pressure.") else consume()
                itemId in CUPCAKES -> if (has("a little queasy")) abort("Your stomach is too queasy.") else consume()
                itemId in LOVE_SONGS ->
                    if (has("conflicting emotions")) abort("Your heart is already filled with emotions.") else consume()
                itemId == ROLLING_PIN -> { take(DOUGH, owned(DOUGH)); keep() }
                itemId == UNROLLING_PIN -> { take(FLAT_DOUGH, owned(FLAT_DOUGH)); keep() }
                itemId == EXPRESS_CARD -> { if (has("charged up")) prefInt("_zapCount", 0); keep() }
                itemId == PLUS_SIGN -> plusSign()
                itemId == OVEN -> campInstall("already got an oven", "hasOven", OVEN)
                itemId == RANGE -> campInstall("already got a fancy oven", "hasRange", RANGE)
                itemId == 1112 -> {
                    if (has("already got a clockwork chef-in-the-box")) keep()
                    else { CampgroundInventorySync.setItem(preferences, 438, 0); installChef(1112) }
                }
                itemId == 438 -> installChef(438)
                itemId == 4708 -> installKept("already got a cocktailcrafting kit", "hasShaker", 4708)
                itemId == 236 -> campInstall("already got a fancy cocktailcrafting kit", "hasCocktailKit", 236)
                itemId == 1111 -> {
                    if (has("already got a clockwork bartender-in-the-box")) keep()
                    else { CampgroundInventorySync.setItem(preferences, 440, 0); installBartender(1111) }
                }
                itemId == 440 -> installBartender(440)
                itemId == 3581 -> {
                    prefBool("hasSushiMat")
                    CampgroundInventorySync.setItem(preferences, 3581, 1)
                    ConcoctionDatabase.markRefreshNeeded()
                    consume()
                }
                itemId == JACKING_MAP -> jackingMap()
                itemId == 8674 -> { prefBool("coldAirportAlways"); if (has("name gets added to the registry")) consume() else keep() }
                itemId == 8675 -> if (has("already have access to that place")) keep() else { prefBool("_coldAirportToday"); consume() }
                itemId == 8134 -> { prefBool("lovebugsUnlocked"); if (has("have been permanently unlocked")) consume() else keep() }
                itemId == 7304 -> { quests()?.setQuestIfBetter(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.STARTED); consume() }
                itemId == 6100 -> robot()
                itemId == 4205 -> wordquiz()
                itemId == 6353 -> dreadscroll()
                itemId == 3808 -> { prefBool("intenseCurrents"); consume() }
                itemId == 3811 -> { take(MERKIN_LOCKKEY); consume() }
                itemId == 6357 -> { DreadScrollManager.handleKnucklebone(html, preferences, null); consume() }
                itemId == 6593 -> { prefBool("_defectiveTokenUsed"); keep() }
                itemId == 6429 -> { prefBool("_silverDreadFlaskUsed"); keep() }
                itemId == 6428 -> { prefBool("_brassDreadFlaskUsed"); keep() }
                itemId in MARKS -> if (has("You have unlocked a new tattoo")) consume() else keep()
                itemId == 6579 -> if (has("You've already learned everything")) keep() else consume()
                itemId == 6683 -> bookOfMatches()
                itemId == 6741 -> { prefBool("_eternalCarBatteryUsed"); keep() }
                itemId == 5881 -> keep()
                itemId == 6854 -> desert()
                itemId == 6669 -> if (has("You bury the claymore in the clay")) {
                    UseItemRequestState.markIslandRefresh()
                    consume()
                } else keep()
                itemId == 9164 -> if (has("hippies and frat orcs")) {
                    UseItemRequestState.markIslandRefresh()
                    consume()
                } else keep()
                itemId in LINTS -> if (has("very improbable thing happens")) { LINTS.forEach { take(it) }; keep() } else keep()
                itemId in BEDDING -> if (has("spirit bed")) { BEDDING.forEach { take(it) }; keep() } else keep()
                itemId == 6900 -> { prefBool("_pastaAdditive"); keep() }
                itemId == 7056 -> { if (!has("breakfast miracle")) prefBool("_warbearBreakfastMachineUsed"); keep() }
                itemId == 7059 -> { prefBool("_warbearSodaMachineUsed"); keep() }
                itemId == 7049 -> { prefBool("_warbearGyrocopterUsed"); consume() }
                itemId == 7060 -> { if (!has("don't have")) prefBool("_warbearBankUsed"); keep() }
                itemId == 7133 -> { prefBool("_lupineHormonesUsed"); consume() }
                itemId == 6408 -> { prefBool("_corruptedStardustUsed"); consume() }
                itemId == 4592 -> { prefBool("_pixelOrbUsed"); consume() }
                itemId == 6297 || itemId == 7252 -> skillPoint(if (itemId == 6297) "jarlsbergPoints" else "sneakyPetePoints")
                itemId == 8144 -> if (has("transform into knowledge")) { bump("edPoints"); consume() } else keep()
                itemId == 9506 -> if (has("You lean how best to grow your social capital.")) { bump("bondPoints"); consume() } else keep()
                itemId == 10185 -> { bump("darkGyfftePoints"); consume() }
                itemId == 11253 -> if (has("extra replica Mr. Accessor")) { bump("legacyPoints", count, 19); consume() } else keep()
                itemId == 11562 -> { bump("wereProfessorPoints", count, 23); consume() }
                itemId == 4804 -> if (has("You quaff")) { decrement("summonAnnoyanceCost"); consume() } else keep()
                itemId == 7481 -> { if (has("You pop the sweet") || has("You already had")) prefBool("_sweetToothUsed"); consume() }
                itemId == 8633 -> { prefBool("_voraciTeaUsed"); consume() }
                itemId == 8634 -> { prefBool("_sobrieTeaUsed"); consume() }
                itemId == 7729 -> { prefBool("_chronerTriggerUsed"); keep() }
                itemId == 7723 -> chronerCross()
                itemId == 7270 -> { quests()?.setQuestIfBetter(Quest.PALINDOME, "step2"); keep() }
                steam != null -> { if (has("rub the three trading cards together")) steam.forEach { take(it) }; keep() }
                itemId in XIBLAXIAN -> { if (preferences?.getBoolean("unknownRecipe$itemId", false) == true) prefBool("unknownRecipe$itemId", false); consume() }
                itemId in CRIMBOT -> if (has("You feed the schematic into the Crimbot assembler")) consume() else keep()
                itemId == 4874 -> if (has("the key vanishes")) consume() else keep()
                itemId == 7936 -> { prefBool("_pickyTweezersUsed"); keep() }
                itemId == 5927 -> { prefStr("_bittycar", "hotcar"); keep() }
                itemId == 5926 -> { prefStr("_bittycar", "meatcar"); keep() }
                itemId == 6046 -> { prefStr("_bittycar", "soulcar"); keep() }
                itemId == 8018 -> { prefBool("_rainStickUsed"); keep() }
                itemId == 10594 -> { prefBool("_redwoodRainStickUsed"); keep() }
                itemId == 8086 -> { prefInt("lastStillBeatingSpleen", ascensions()); if (has("assimilate")) consume() else keep() }
                itemId == 8070 || itemId == 8071 -> warehouse()
                itemId in MAYO -> mayo()
                itemId == 6453 -> { prefBool("_hungerSauceUsed"); consume() }
                itemId == 8238 -> { prefBool("_brainPreservationFluidUsed"); consume() }
                itemId == 8279 -> { if (has("turn in 100 chips at the redemption center")) take(8279, 100); keep() }
                itemId == 8283 -> { prefBool("_cocktailShakerUsed"); keep() }
                itemId == 8666 -> { prefBool("_twelveNightEnergyUsed"); consume() }
                itemId == 5739 -> { prefBool("_fireStartingKitUsed"); keep() }
                itemId == 8564 -> { prefBool("barrelShrineUnlocked"); consume() }
                itemId == 8639 -> if (has("You install the doghouse at your campsite")) { CampgroundInventorySync.setItem(preferences, 8639, 1); consume() } else keep()
                itemId == 8640 -> ghostChow()
                itemId == 8730 || itemId == 9528 -> keep()
                itemId == 8784 -> circleDrum()
                itemId == 8893 -> clara()
                itemId == 8890 -> { prefBool("_glennGoldenDiceUsed"); keep() }
                itemId == 8705 -> { prefBool("snojoAvailable"); consume() }
                itemId == 8635 -> { bump("royalty"); consume() }
                itemId == 5964 -> if (has("already in the process of investigating")) keep() else consume()
                itemId == 8971 -> { bump("awolMedicine", 3); bump("awolVenom", 3); consume() }
                itemId == 8986 -> { bump("awolDeferredPointsCowpuncher"); consume() }
                itemId == 8987 -> { bump("awolDeferredPointsBeanslinger"); consume() }
                itemId == 8988 -> { bump("awolDeferredPointsSnakeoiler"); consume() }
                itemId == 11167 -> { bump("asolDeferredPoints"); consume() }
                itemId == 9002 || itemId == 9004 || itemId == 9005 -> { prefBool("_floundryItemUsed"); consume() }
                itemId == 9025 -> { prefBool("_baconMachineUsed"); consume() }
                itemId == 9123 -> { prefBool("_hardKnocksDiplomaUsed"); consume() }
                itemId == 11451 -> { prefBool("_punchingMirrorUsed"); consume() }
                itemId == 11421 -> { prefBool("_elfGuardHangoverCureUsed"); consume() }
                itemId in STACK_CHIPS -> stackChip()
                itemId in NAMED_CHIPS -> namedChip()
                itemId in TERMINAL_FILES -> terminalFile()
                itemId == 9073 -> { prefBool("hasDetectiveSchool"); consume() }
                itemId == 9083 -> walkie()
                itemId == 9215 -> if (has("manage to repair")) { take(SPARE_CHOCOLATE_PARTS); consume() } else keep()
                itemId == 9404 -> { prefBool("spacegateAlways"); if (has("is now keyed to your genetic signature")) consume() else keep() }
                itemId == 9465 -> if (has("already have access to that place")) keep() else { prefBool("_spacegateToday"); prefInt("_spacegateTurnsLeft", 20); consume() }
                itemId == 9463 -> spaceBaby()
                itemId == 9503 -> { prefBool("_licenseToChillUsed"); consume() }
                itemId == 9489 -> { prefBool("_victorSpoilsUsed"); consume() }
                itemId == 9498 -> { bump("_villainLairProgress", 5); consume() }
                itemId == 9183 -> { bump("cornucopiasOpened", count); consume() }
                itemId == 9513 -> { bump("_meteoriteAdesUsed"); consume() }
                itemId == 9526 -> { prefBool("_perfectlyFairCoinUsed"); consume() }
                itemId == 9761 -> pokeGrow()
                itemId == 9835 -> { prefBool("frAlways"); if (has("escape to a realm of fantasy")) consume() else keep() }
                itemId == 9836 -> if (has("You've already got access to FantasyRealm.")) keep() else { prefBool("_frToday"); consume() }
                else -> Unit
            }
        }

        private fun legion() {
            if (itemId == SCREWDRIVER && has("You jam your screwdriver")) {
                val (id, qty) = UseItemRequestState.takeUntinker()
                if (id > 0) {
                    take(id, qty)
                    note("Successfully unscrewed $id")
                }
                keep()
                return
            }
            if (has("latches and clasps")) take(itemId)
            keep()
        }

        private fun jackingMap() {
            if (has("into the tube")) {
                val fruit = UseItemRequestState.takeFruit()
                if (fruit > 0) take(fruit)
            }
            keep()
        }

        private fun gifts() {
            if (has("You can't receive things")) abort("You can't open that package yet.") else consume()
        }

        private fun plusSign() {
            if (!has("you treat the plus sign as a book")) {
                abort("You don't know how to use it.")
                return
            }
            for (id in PUNCTUATION) take(id, owned(id))
            consume()
        }

        private fun installChef(campItem: Int) {
            if (has("already got a chef-in-the-box")) {
                keep()
                return
            }
            prefBool("hasChef")
            prefInt("chefTurnsUsed", 0)
            CampgroundInventorySync.setItem(preferences, campItem, 1)
            ConcoctionDatabase.markRefreshNeeded()
            consume()
        }

        private fun installBartender(campItem: Int) {
            if (has("already got a bartender-in-the-box")) {
                keep()
                return
            }
            prefBool("hasBartender")
            prefInt("bartenderTurnsUsed", 0)
            CampgroundInventorySync.setItem(preferences, campItem, 1)
            ConcoctionDatabase.markRefreshNeeded()
            consume()
        }

        private fun installKept(already: String, pref: String, campItem: Int) {
            if (has(already)) {
                keep()
                return
            }
            prefBool(pref)
            CampgroundInventorySync.setItem(preferences, campItem, 1)
            ConcoctionDatabase.markRefreshNeeded()
            keep()
        }

        private fun campInstall(already: String, pref: String, campItem: Int) {
            if (has(already)) {
                keep()
                return
            }
            prefBool(pref)
            CampgroundInventorySync.setItem(preferences, campItem, 1)
            ConcoctionDatabase.markRefreshNeeded()
            consume()
        }

        private fun robot() {
            when {
                has("emits a satisfied whirr") -> bump("homemadeRobotUpgrades", 1, 9)
                has("Your work here is done") -> prefInt("homemadeRobotUpgrades", 9)
                else -> { keep(); return }
            }
            consume()
        }

        private fun wordquiz() {
            val mastery = WORDQUIZ.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()
            if (mastery == null) {
                keep()
                return
            }
            prefInt("merkinVocabularyMastery", mastery)
            take(MERKIN_CHEATSHEET)
            consume()
        }

        private fun dreadscroll() {
            if (has("I guess you're the Mer-kin High Priest now")) {
                prefBool("isMerkinHighPriest")
                prefStr("merkinQuestPath", "scholar")
                consume()
                return
            }
            if (preferences?.getString("merkinQuestPath", "") != "done" && has("The sigil burned into your forehead")) {
                prefBool("isMerkinGladiatorChampion")
                prefStr("merkinQuestPath", "gladiator")
                prefInt("lastColosseumRoundWon", 15)
                consume()
                return
            }
            keep()
        }

        private fun bookOfMatches() {
            if (preferences != null &&
                preferences.getInt("hiddenTavernUnlock", 0) != ascensions() &&
                !has("admire")
            ) {
                prefInt("hiddenTavernUnlock", ascensions())
                ConcoctionDatabase.markRefreshNeeded()
            }
            keep()
        }

        private fun desert() {
            val prefs = preferences ?: return consume()
            DesertVisitSync.incrementExploration(prefs, QuestDatabase(prefs), 15)
            consume()
        }

        private fun skillPoint(pref: String) {
            if (!has("extra skill point")) keep() else { bump(pref); consume() }
        }

        private fun decrement(key: String) {
            val prefs = preferences ?: return
            prefs.setInt(key, (prefs.getInt(key, 0) - 1).coerceAtLeast(0))
        }

        private fun chronerCross() {
            prefBool("_chronerCrossUsed")
            if (!has("falls right through")) take(CHRONER)
            keep()
        }

        private fun warehouse() {
            if (has("compare the map")) {
                take(8070)
                take(8071)
                bump("warehouseProgress", 8)
            }
            keep()
        }

        private fun mayo() {
            if (has("mouth is already full")) {
                keep()
                return
            }
            prefStr("mayoInMouth", MAYO[itemId] ?: "")
            bump("mayoLevel")
            consume()
        }

        private fun ghostChow() {
            when {
                has("familiar doesn't seem interested") -> note("Your familiar is not interested in that item.")
                has("you can't figure out how to feed") -> note("You cannot feed Ghost Dog Chow to a Ghost of Crimbo")
                else -> { consume(); return }
            }
            keep()
        }

        private fun circleDrum() {
            val boost = RHYTHM.find(html)?.groupValues?.getOrNull(1)
                ?.replace(",", "")
                ?.toIntOrNull()
            if (boost != null) prefInt("_feelinTheRhythm", (boost - 1) / 10 + 1)
            prefBool("_circleDrumUsed")
            keep()
        }

        private fun clara() {
            if (has("your stomach drops and your ears pop")) prefBool("noncombatForcerActive")
            prefBool("_claraBellUsed")
            keep()
        }

        private fun stackChip() {
            val total = CHIP_COUNT.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 10
            STACK_CHIPS[itemId]?.let { prefInt(it, total) }
            if (has("You've already installed")) keep() else consume()
        }

        private fun namedChip() {
            NAMED_CHIPS[itemId]?.let { appendCsv("sourceTerminalChips", it) }
            if (has("You've already installed")) keep() else consume()
        }

        private fun terminalFile() {
            val fileName = TERMINAL_FILES[itemId] ?: return
            val pref = when {
                fileName.contains(".edu") -> "sourceTerminalEducateKnown"
                fileName.contains(".enq") -> "sourceTerminalEnquiryKnown"
                fileName.contains(".enh") -> "sourceTerminalEnhanceKnown"
                fileName.contains(".ext") -> "sourceTerminalExtrudeKnown"
                else -> return
            }
            appendCsv(pref, fileName)
            if (has("You've already installed a copy of")) keep() else consume()
        }

        private fun walkie() {
            if (owned(PROTON_ACCELERATOR) > 0) {
                keep()
                return
            }
            val turns = character?.state?.value?.turnsPlayed ?: 0
            val found = ProtonicGhostSync.applyFromWalkieTalkie(html, quests(), preferences, turns)
            if (found) consume() else keep()
        }

        private fun spaceBaby() {
            if (!has("learn a few words")) {
                keep()
                return
            }
            FLUENCY.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
                prefInt("spaceBabyLanguageFluency", it)
            }
            consume()
        }

        private fun pokeGrow() {
            if (!has("tall grass springs up")) {
                keep()
                return
            }
            val prefs = preferences
            val crop = GardenCropAvailability.getCrop(prefs)
            if (prefs != null && crop != null && crop.itemId == TALL_GRASS && crop.count != 8) {
                CampgroundInventorySync.setItem(prefs, TALL_GRASS, crop.count + 1)
            }
            consume()
        }
    }
}
