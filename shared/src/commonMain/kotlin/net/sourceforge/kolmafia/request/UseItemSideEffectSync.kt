package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.campground.CampgroundAvailability
import net.sourceforge.kolmafia.campground.CampgroundInventorySync
import net.sourceforge.kolmafia.campground.DwellingSync
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.modifiers.StringModifier
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.ElVibratoSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.BugbearManager
import net.sourceforge.kolmafia.session.CryptManager
import net.sourceforge.kolmafia.session.EquipmentManager
import net.sourceforge.kolmafia.session.SkillLearnFromResponse
import net.sourceforge.kolmafia.session.TurnCounter
import net.sourceforge.kolmafia.skill.SkillLearner
import net.sourceforge.kolmafia.skill.SkillManager

/**
 * Desktop [UseItemRequest.parseConsumption] item-id side effects that run
 * after organ routing returns (Phases 7871–7930). The switch tail lives in
 * [UseItemSideEffectTail] (Phases 7931–7990).
 *
 * [Outcome.KEEP] matches a desktop `return` (item stays). [Outcome.CONSUME]
 * matches `break` (caller removes the item). [Outcome.ABORT] matches an error
 * return that must not look like a successful use.
 */
object UseItemSideEffectSync {

    enum class Outcome { UNHANDLED, CONSUME, KEEP, ABORT }

    data class Result(
        val outcome: Outcome,
        val message: String = "",
        val extraConsumes: List<Pair<Int, Int>> = emptyList(),
    )

    fun apply(
        responseText: String,
        itemId: Int,
        count: Int,
        preferences: Preferences?,
        character: KoLCharacter?,
        inventory: InventoryManager?,
        equipmentManager: EquipmentManager? = null,
        skillManager: SkillManager? = null,
        familiarManager: FamiliarManager? = null,
    ): Result {
        val ctx = Ctx(
            responseText, itemId, count, preferences, character, inventory,
            equipmentManager, skillManager, familiarManager,
        )
        ctx.dispatch()
        return Result(ctx.outcome, ctx.message, ctx.extraConsumes.toList())
    }

    private class Ctx(
        val html: String,
        val itemId: Int,
        val count: Int,
        val preferences: Preferences?,
        val character: KoLCharacter?,
        val inventory: InventoryManager?,
        val equipmentManager: EquipmentManager?,
        val skillManager: SkillManager?,
        val familiarManager: FamiliarManager?,
    ) {
        var outcome: Outcome = Outcome.UNHANDLED
        var message: String = ""
        val extraConsumes = mutableListOf<Pair<Int, Int>>()

        fun consume() { outcome = Outcome.CONSUME }
        fun keep() { outcome = Outcome.KEEP }
        fun abort(text: String) {
            outcome = Outcome.ABORT
            message = text
        }
        fun take(id: Int, qty: Int = 1) {
            if (qty > 0) extraConsumes += id to qty
        }
        fun has(id: Int): Boolean = (inventory?.getCount(id) ?: 0) > 0
        fun prefBool(key: String, value: Boolean = true) { preferences?.setBoolean(key, value) }
        fun prefInt(key: String, value: Int) { preferences?.setInt(key, value) }
        fun prefStr(key: String, value: String) { preferences?.setString(key, value) }
        fun bump(key: String, delta: Int = 1, max: Int = 0) { preferences?.increment(key, delta, max) }
        fun contains(text: String): Boolean = html.contains(text, ignoreCase = true)
        fun quests(): QuestDatabase? = preferences?.let { QuestDatabase(it) }

        fun dispatch() {
            when (itemId) {
                LEGENDARY_BEAT -> { prefBool("_legendaryBeat"); keep() }
                PORTABLE_STEAM_UNIT -> { prefBool("_portableSteamUnitUsed"); keep() }
                TOASTER -> { prefBool("_toastSummoned"); keep() }
                ICE_SCULPTURE -> { prefBool("_iceSculptureUsed"); keep() }
                SHAKING_CAMERA -> { prefBool("_cameraUsed"); keep() }
                SHAKING_CRAPPY_CAMERA -> { prefBool("_crappyCameraUsed"); keep() }
                LYNYRD_SNARE -> { prefInt("_lynyrdSnareUses", 3); keep() }
                BAG_O_TRICKS -> { prefBool("_bagOTricksUsed"); keep() }
                ALL_YEAR_SUCKER -> { prefBool("_allYearSucker"); keep() }
                DARK_CHOCOLATE_HEART -> { prefBool("_darkChocolateHeart"); keep() }
                JACKASS_PLUMBER_GAME -> { prefBool("_jackassPlumberGame"); keep() }
                TRIVIAL_AVOCATIONS_GAME -> { prefBool("_trivialAvocationsGame"); keep() }
                CREEPY_VOODOO_DOLL -> { prefBool("_creepyVoodooDollUsed"); keep() }
                HOBBY_HORSE -> { prefBool("_hobbyHorseUsed"); keep() }
                BALL_IN_A_CUP -> { prefBool("_ballInACupUsed"); keep() }
                SET_OF_JACKS -> { prefBool("_setOfJacksUsed"); keep() }
                BAG_OF_CANDY -> { prefBool("_bagOfCandyUsed"); keep() }
                EMBLEM_AKGYXOTH, IDOL_AKGYXOTH -> { prefBool("_akgyxothUsed"); keep() }
                GNOLL_EYE -> { prefBool("_gnollEyeUsed"); keep() }
                KOL_CON_SIX_PACK -> { prefBool("_kolConSixPackUsed"); keep() }
                MUS_MANUAL, MYS_MANUAL, MOX_MANUAL -> { prefBool("_guildManualUsed"); keep() }
                STYX_SPRAY -> { prefBool("_styxSprayUsed"); keep() }
                STABONIC_SCROLL -> { prefBool("_stabonicScrollUsed"); keep() }
                COAL_PAPERWEIGHT -> { prefBool("_coalPaperweightUsed"); keep() }
                JINGLE_BELL -> { prefBool("_jingleBellUsed"); keep() }
                CURSED_KEG -> { prefBool("_cursedKegUsed"); keep() }
                CURSED_MICROWAVE -> { prefBool("_cursedMicrowaveUsed"); keep() }
                TACO_FLIER -> { prefBool("_tacoFlierUsed"); keep() }
                FISHY_PIPE -> { prefBool("_fishyPipeUsed"); keep() }
                DOLPHIN_WHISTLE, RUSTY_HEDGE_TRIMMERS, CARONCH_MAP, FRATHOUSE_BLUEPRINTS -> keep()
                in BRICKO_FIGHTS -> bricko()
                in FOSSIL_SKULLS -> keep()
                in RECORDINGS -> recording()
                SPARKLER, SNAKE, M282, GREEN_ROCKET -> fireworks()
                GONG -> gong()
                D20 -> d20()
                BGE_TATTOO -> if (contains("You've already got one of those tattoos on")) keep() else consume()
                HERMIT_SCRIPT -> { prefBool("hermitHax0red"); consume() }
                ENCHANTED_BEAN -> bean()
                LIBRARY_CARD -> libraryCard()
                HEY_DEZE_MAP -> if (contains("pleased me greatly")) consume() else abort("Your music was inadequate.")
                GIANT_CASTLE_MAP ->
                    if (contains("Sorceress is in another castle")) consume()
                    else abort("You couldn't make it all the way to the back door.")
                DRASTIC_HEALING -> if (contains("crumble")) consume() else keep()
                ANTIDOTE -> if (contains("don't waste the anti")) keep() else consume()
                TBONE_KEY -> tbone()
                KETCHUP_HOUND -> ketchup()
                DOLPHIN_KING_MAP ->
                    if (contains("find his glorious treasure")) consume()
                    else abort("You don't have everything you need.")
                SLUG_LORD_MAP ->
                    if (contains("deepest part of the tank")) consume()
                    else abort("You don't have everything you need.")
                DR_HOBO_MAP -> hoboMap()
                SHOPPING_LIST -> if (contains("throw it away")) consume() else keep()
                COBBS_KNOB_MAP -> cobbsKnob()
                SPOOKY_MAP -> spookyMap()
                DINGHY_PLANS -> dinghy()
                MORTAR_DISSOLVING_RECIPE -> mortar()
                FENG_SHUI -> fengShui()
                GATES_SCROLL -> gatesScroll()
                ELITE_SCROLL -> consume()
                in SNOWCONES -> if (contains("still cold")) abort("Your mouth is too cold.") else consume()
                GREEN_THUMB -> { prefBool("ownsFloristFriar"); consume() }
                REPLICA_GREEN_THUMB -> { prefBool("ownsReplicaFloristFriar"); consume() }
                CHATEAU_ROOM_KEY -> chateau()
                REPLICA_CHATEAU_ROOM_KEY -> { prefBool("replicaChateauAvailable"); consume() }
                GINGERBREAD_CITY -> gingerbread()
                COUNTERFEIT_CITY -> counterfeitCity()
                TELEGRAPH_OFFICE_DEED -> telegraph()
                INFLATABLE_TELEGRAPH_OFFICE -> inflatableTelegraph()
                HEART_SHAPED_CRATE -> loveTunnel()
                LOVE_ENTRANCE_PASS -> if (contains("You follow the directions")) { prefBool("_loveTunnelToday"); consume() } else keep()
                BEAUTIFUL_RAINBOW -> rainbow()
                THUNDER_THIGH, AQUA_BRAIN, LIGHTNING_MILK ->
                    if (contains("you just throw it away")) consume() else keep()
                OLFACTION_BOOK -> olfaction()
                TEACHINGS_OF_THE_FIST -> fist()
                BOOKE_OF_VAMPYRIC_KNOWLEDGE -> vampyreBook()
                SLIME_SOAKED_HYPOPHYSIS, SLIME_SOAKED_BRAIN, SLIME_SOAKED_SWEAT_GLAND -> slimeGland()
                WESTERN_SLANG_VOL_1, WESTERN_SLANG_VOL_2, WESTERN_SLANG_VOL_3 -> westernSlang()
                in UseItemSkillBooks.IDS -> skillBook()
                SPICE_MELANGE -> spiceMelange()
                ULTRA_MEGA_SOUR_BALL -> sourBall()
                ALIEN_ANIMAL_MILK -> alienMilk()
                ALIEN_PLANT_POD -> alienPod()
                SYNTHETIC_DOG_HAIR_PILL -> dogHair()
                DISTENTION_PILL -> distention()
                MILK_OF_MAGNESIUM -> milk()
                BORROWED_TIME -> borrowedTime()
                MOVEABLE_FEAST -> feast()
                RESOLUTION_ADVENTUROUS -> resolution()
                ESSENTIAL_TOFU -> { prefBool("_essentialTofuUsed"); consume() }
                CHOCOLATE_CIGAR -> chocolateCigar()
                VITACHOC_CAPSULE -> vitachoc()
                CHOCOLATE_SCULPTURE -> sculpture()
                in FANCY_CHOCOLATES -> { bump("_chocolatesUsed"); consume() }
                LOVE_CHOCOLATE -> { bump("_loveChocolatesUsed"); consume() }
                EXTRA_TIME -> { bump("_extraTimeUsed"); consume() }
                BOX_OF_HAMMERS -> { prefBool("_boxOfHammersUsed"); consume() }
                TEMPURA_AIR -> { prefBool("_tempuraAirUsed"); consume() }
                PRESSURIZED_PNEUMATICITY ->
                    if (contains("You pop the cork")) { prefBool("_pneumaticityPotionUsed"); consume() } else consume()
                HYPERINFLATED_SEAL_LUNG -> { prefBool("_hyperinflatedSealLungUsed"); consume() }
                BALLAST_TURTLE -> { prefBool("_ballastTurtleUsed"); consume() }
                STUFFED_POCKETWATCH ->
                    if (contains("You play with the stuffed watch")) prefBool("_stuffedPocketwatchUsed").also { keep() } else keep()
                LEFT_BEAR_ARM -> bearArm()
                in CRIMBO_TOYS -> crimboToys()
                in PSYCHO_JARS -> psychoJar()
                SONAR -> sonar()
                SPRING_BEACH_CHARTER -> charter("sleazeAirportAlways")
                SPRING_BEACH_TICKET -> dayPass("_sleazeAirportToday")
                SPRING_BEACH_TATTOO_COUPON ->
                    if (contains("stagger into the back room")) consume() else keep()
                ANTI_FUNGAL_SPRAY -> { preferences?.resetToDefault("funGuyMansionKills"); consume() }
                CONSPIRACY_ISLAND_CHARTER -> charter("spookyAirportAlways")
                CONSPIRACY_ISLAND_TICKET -> dayPass("_spookyAirportToday")
                DINSEY_CHARTER -> charter("stenchAirportAlways")
                DINSEY_TICKET -> dayPass("_stenchAirportToday")
                VOLCANO_CHARTER -> charter("hotAirportAlways")
                VOLCANO_TICKET -> dayPass("_hotAirportToday")
                SHAWARMA_KEYCARD -> { prefBool("SHAWARMAInitiativeUnlocked"); consume() }
                BOTTLE_OPENER_KEYCARD -> { prefBool("canteenUnlocked"); consume() }
                ARMORY_KEYCARD -> { prefBool("armoryUnlocked"); consume() }
                JUICY_GARBAGE -> { bump("juicyGarbageUsed"); consume() }
                PEN_PAL_KIT -> if (contains("already got a pen pal")) keep() else consume()
                NEW_YOU_CLUB_MEMBERSHIP_FORM ->
                    if (contains("instructions on the back of the card")) consume() else keep()
                SEAL_IRON_INGOT -> if (contains("formidable club")) consume() else keep()
                in SEAL_FIGHTS -> keep()
                DEPLETED_URANIUM_SEAL -> uraniumSeal()
                EVIL_EYE -> evilEye()
                EVILOMETER -> { CryptManager.examineEvilometer(html, preferences); keep() }
                QUASIRELGIOUS_SCULPTURE, SOLID_GOLD_ROSARY -> cyrptSafer()
                KEYOTRON -> keyotron()
                SINISTER_ANCIENT_TABLET -> tablet()
                in LAMINATED -> laminated()
                in DWARVISH -> dwarvish()
                BOOZEHOUND_TOKEN -> if (contains("don't know where any bars are")) keep() else consume()
                CLANCY_SACKBUT -> { prefStr("clancyInstrument", "sackbut"); keep() }
                CLANCY_CRUMHORN -> { prefStr("clancyInstrument", "crumhorn"); keep() }
                CLANCY_LUTE -> { prefStr("clancyInstrument", "lute"); keep() }
                in SEEDS -> seeds()
                STAFF_GUIDE -> staffGuide()
                in COSTUMES -> if (contains("You've already got a sexy costume on")) abort("You've already got a sexy costume on.") else consume()
                BLACK_PAINT -> blackPaint()
                BURROWGRUB_HIVE -> burrowgrub()
                TEARS -> { /* Beaten Up removal via UneffectRemovableMaps */ consume() }
                TELESCOPE -> telescope()
                WORKYTIME_TEA ->
                    if (contains("not quite bored enough")) {
                        abort("You're not bored enough to drink that much tea.")
                    } else {
                        consume()
                    }
                WARM_SUBJECT -> warmSubject()
                MINING_OIL, TAINTED_MINING_OIL -> miningOil()
                DUSTY_ANIMAL_SKULL -> dustySkull()
                ANCIENT_CURSED_FOOTLOCKER -> cursedChest(SIMPLE_CURSED_KEY)
                ORNATE_CURSED_CHEST -> cursedChest(ORNATE_CURSED_KEY)
                GILDED_CURSED_CHEST -> cursedChest(GILDED_CURSED_KEY)
                STUFFED_CHEST -> cursedChest(STUFFED_KEY)
                GENERAL_ASSEMBLY_MODULE -> generalAssembly()
                in BANG_POTIONS -> bangPotion()
                in SLIME_VIALS -> slimeVial()
                OUTRAGEOUS_SOMBRERO -> { prefBool("outrageousSombreroUsed"); keep() }
                NEVERENDING_SODA -> { prefBool("oscusSodaUsed"); keep() }
                AUGMENTED_DRONE -> augmentedDrone()
                TRAPEZOID -> trapezoid()
                PERSONAL_MASSAGER ->
                    if (contains("don't really need a massage")) keep() else consume()
                GRUB, MOTH, FIRE_ANT, ICE_ANT, STINKBUG, DEATH_WATCH_BEETLE, LOUSE ->
                    if (contains("filled with revulsion")) keep() else consume()
                HONEYPOT -> honeypot()
                MAID, CLOCKWORK_MAID, MEAT_BUTLER, PORTABLE_HOUSEKEEPING_ROBOT -> maid()
                SCARECROW, MEAT_GOLEM, BLACK_BLUE_LIGHT, LOUDMOUTH_LARRY, PLASMA_BALL,
                MEAT_GLOBE, LED_CLOCK, BONSAI_TREE,
                -> campFurniture()
                in DWELLINGS -> dwelling()
                else -> dispatchTail()
            }
        }

        private fun telescope() {
            preferences?.setInt("lastTelescopeReset", -1)
            preferences?.setInt("telescopeUpgrades", preferences.getInt("telescopeUpgrades", 0).coerceAtLeast(1))
            character?.setCampground(telescopeUpgrades = preferences?.getInt("telescopeUpgrades", 1) ?: 1)
            consume()
        }

        private fun warmSubject() {
            // Desktop multi-use: first ironical shirt consumes only one.
            if (contains("ironically") && count > 1) {
                take(itemId, 1)
                keep()
            } else {
                consume()
            }
        }

        private fun miningOil() {
            if (contains("Limiting to 100") && count > 100) {
                take(itemId, 100)
                keep()
            } else {
                consume()
            }
        }

        private fun dustySkull() {
            if (!contains("Graaangh?")) {
                abort("You're missing some parts.")
                return
            }
            for (id in 1802 until 1900) take(id)
            consume()
        }

        private fun cursedChest(keyId: Int) {
            if (!has(keyId)) {
                keep()
                return
            }
            take(keyId)
            consume()
        }

        private fun generalAssembly() {
            if (contains("INSUFFICIENT RESOURCES LOCATED")) {
                keep()
                return
            }
            when {
                contains("carrying the  laser cannon") -> {
                    take(LASER_CANON); take(LASER_TARGETING_CHIP); take(UNOBTAINIUM_STRAPS)
                    consume()
                }
                contains("carrying the  polymorphic fastening apparatus") -> {
                    take(FASTENING_APPARATUS); take(LEG_ARMOR); take(GLUTEAL_SHIELD)
                    consume()
                }
                contains("carrying the carbonite visor") -> {
                    take(CARBONITE_VISOR); take(CHIN_STRAP); take(KEVLATEFLOCITE_HELMET)
                    consume()
                }
                else -> consume()
            }
        }

        private fun bangPotion() {
            BangPotionElimination.identifyBangPotion(html, itemId, preferences)
            if (contains("You decide not to drink it")) keep() else consume()
        }

        private fun slimeVial() {
            BangPotionElimination.identifySlimeVial(html, itemId, preferences)
            consume()
        }

        private fun maid() {
            if (contains("You've already got")) {
                keep()
                return
            }
            val prefs = preferences
            if (prefs != null) {
                for (id in MAIDS) CampgroundInventorySync.setItem(prefs, id, 0)
                CampgroundInventorySync.setItem(prefs, itemId, 1)
            }
            consume()
        }

        private fun campFurniture() {
            if (contains("You've already got")) {
                keep()
                return
            }
            preferences?.let { CampgroundInventorySync.setItem(it, itemId, 1) }
            consume()
        }

        private fun dwelling() {
            if (contains("You've already got")) {
                keep()
                return
            }
            DwellingSync.setCurrentDwelling(preferences, itemId)
            consume()
        }

        private fun augmentedDrone() {
            if (contains("You put an overcharged sphere in the cavity")) {
                take(OVERCHARGED_POWER_SPHERE)
            }
            consume()
        }

        private fun trapezoid() {
            if (!contains("you put it on the ground at your campsite")) {
                keep()
                return
            }
            preferences?.setInt("currentPortalEnergy", 20)
            preferences?.let { ElVibratoSync.updatePortalTrapezoid(it) }
            consume()
        }

        private fun cyrptSafer() {
            if (contains("entire Cyrpt feels safer")) {
                UseItemRequestState.markEvilometerRefresh()
            }
            consume()
        }

        private fun honeypot() {
            preferences?.let {
                TurnCounter.stopCounting(it, "Bee window begin")
                TurnCounter.stopCounting(it, "Bee window end")
            }
            consume()
        }

        private fun dispatchTail() {
            var tail = UseItemSideEffectTail.apply(html, itemId, count, preferences, character, inventory)
            if (tail.outcome == Outcome.UNHANDLED) {
                tail = UseItemSideEffectTail2.apply(
                    html, itemId, count, preferences, character, inventory, skillManager, familiarManager,
                )
            }
            if (tail.outcome == Outcome.UNHANDLED) return
            outcome = tail.outcome
            message = tail.message
            extraConsumes += tail.extraConsumes
        }

        private fun bricko() {
            if (contains("You're sick of playing with BRICKOs today")) {
                prefInt("_brickoFights", 10)
                abort("You're sick of playing with BRICKOs today")
            } else {
                keep()
            }
        }

        private fun recording() {
            if (contains("too many songs stuck in your head")) abort("You have the maximum number of AT buffs already.")
            else consume()
        }

        private fun fireworks() {
            if (contains("back to work") || contains("fireworks are illegal")) keep()
            else { prefBool("_fireworkUsed"); consume() }
        }

        private fun gong() {
            when {
                contains("sobered up a little") || contains("don't have time to bang") ->
                    abort("Insufficient adventures or sobriety to use a gong.")
                contains("middle of a journey of reincarnation") ->
                    abort("You're still under a gong effect.")
                else -> keep()
            }
        }

        private fun d20() {
            when {
                contains("You already rolled for initiative") -> abort("You already rolled for initiative")
                contains("Maybe you should've paid less attention in gym class") ->
                    abort("Rolling that many d20s doesn't do anything interesting.")
                else -> consume()
            }
        }

        private fun bean() {
            if (!contains("grows into an enormous beanstalk")) keep()
            else {
                quests()?.setQuestIfBetter(Quest.GARBAGE, "step1")
                consume()
            }
        }

        private fun libraryCard() {
            prefBool("libraryCardUsed")
            if (contains("feeling kind of dirty")) consume() else keep()
        }

        private fun tbone() {
            if (!has(LOCKED_LOCKER)) keep()
            else { take(LOCKED_LOCKER); consume() }
        }

        private fun ketchup() {
            if (contains("pagoda")) {
                take(HEY_DEZE_NUTS)
                take(PAGODA_PLANS)
                CampgroundInventorySync.setItem(preferences, PAGODA_PLANS, 1)
            }
            keep()
        }

        private fun hoboMap() {
            if (!contains("exact same moment")) abort("You don't have everything you need.")
            else { take(ASPARAGUS_KNIFE); consume() }
        }

        private fun cobbsKnob() {
            if (!contains("memorize the location")) abort("You don't have everything you need.")
            else {
                take(ENCRYPTION_KEY)
                quests()?.setQuestIfBetter(Quest.GOBLIN, "step1")
                consume()
            }
        }

        private fun spookyMap() {
            if (!has(SPOOKY_SAPLING) || !has(SPOOKY_FERTILIZER)) {
                abort("You don't have everything you need.")
                return
            }
            val asc = character?.state?.value?.ascensionNumber ?: preferences?.getInt("knownAscensions", 0) ?: 0
            val quests = quests()
            if (quests != null && preferences != null &&
                asc != preferences.getInt("lastTempleUnlock", -1)
            ) {
                quests.setProgress(Quest.TEMPLE, QuestDatabase.FINISHED)
                preferences.setInt("lastTempleUnlock", asc)
                if (quests.isQuestStarted(Quest.WORSHIP)) {
                    quests.setQuestIfBetter(Quest.WORSHIP, "step1")
                }
            }
            take(SPOOKY_SAPLING)
            take(SPOOKY_FERTILIZER)
            consume()
        }

        private fun dinghy() {
            if (!has(DINGY_PLANKS)) abort("You need some dingy planks.")
            else { take(DINGY_PLANKS); consume() }
        }

        private fun mortar() {
            if (contains("Screw this scavenger hunt crap")) prefStr("spookyravenRecipeUsed", "with_glasses")
            else if (preferences?.getString("spookyravenRecipeUsed", "") != "with_glasses") {
                prefStr("spookyravenRecipeUsed", "no_glasses")
            }
            consume()
        }

        private fun fengShui() {
            if (has(FOUNTAIN) && has(WINDCHIMES)) {
                take(FOUNTAIN)
                take(WINDCHIMES)
                CampgroundInventorySync.setItem(preferences, FENG_SHUI, 1)
            }
            consume()
        }

        private fun gatesScroll() {
            if (!contains("you're flattered")) {
                keep()
                return
            }
            take(DICTIONARY)
            quests()?.setProgress(Quest.TOPPING, QuestDatabase.FINISHED)
            quests()?.setProgress(Quest.LOL, QuestDatabase.FINISHED)
            consume()
        }

        private fun chateau() {
            prefBool("chateauAvailable")
            if (contains("you find the one it unlocks")) consume()
            else abort("You've already have a room at the Chateau Mantegna.")
        }

        private fun gingerbread() {
            prefBool("gingerbreadCityAvailable")
            if (contains("build a gingerbread city")) consume() else keep()
        }

        private fun counterfeitCity() {
            if (contains("already a gingerbread city")) keep()
            else { prefBool("_gingerbreadCityToday"); consume() }
        }

        private fun telegraph() {
            prefBool("telegraphOfficeAvailable")
            if (contains("You find a vacant lot")) consume()
            else abort("You've already opened a telegraph office.")
        }

        private fun inflatableTelegraph() {
            if (contains("You blow up the replica")) { prefBool("_telegraphOfficeToday"); consume() } else keep()
        }

        private fun loveTunnel() {
            prefBool("loveTunnelAvailable")
            if (contains("You wander")) consume() else abort("You've already opened a Tunnel of L.O.V.E.")
        }

        private fun rainbow() {
            if (!contains("eaten the entire thing")) {
                prefInt("skillLevel117", 11)
                abort("You've already maxed out Belch The Rainbow.")
                return
            }
            learnNamed("Belch The Rainbow")
            consume()
        }

        private fun olfaction() {
            if (!contains("smell has been elevated to a superhuman level")) {
                abort("You can't learn that skill.")
                return
            }
            learnNamed("Transcendent Olfaction")
            consume()
        }

        private fun fist() {
            bump("fistSkillsKnown")
            learnFromHtml()
            consume()
        }

        private fun vampyreBook() {
            when {
                contains("already learned all the darke secrettes") ->
                    abort("You've already learned the blood skill for your class.")
                contains("bunch of gibberish") ->
                    abort("That book has nothing to teach your class.")
                else -> { learnFromHtml(); consume() }
            }
        }

        private fun slimeGland() {
            if (contains("You gain a skill")) learnItemSkill()
            consume()
        }

        private fun westernSlang() {
            if (!contains("skills have been unlocked")) abort("You've already read that book.")
            else consume()
        }

        private fun skillBook() {
            val learned = contains("You acquire a skill") ||
                contains("place the Grimoire on the bookshelf") ||
                contains("absorb the knowledge of optimality") ||
                contains("feel beary") ||
                contains("knew how to maximize") ||
                contains("additional carrot") ||
                contains("larynx become even more pirate") ||
                contains("become even more of an expert") ||
                contains("reread the tale and really remember") ||
                contains("spirit of Kokomo to sink") ||
                contains("Beleven") ||
                contains("absorb the residual paste into your soul") ||
                contains("Don't you think?")
            if (!learned) {
                abort("You can't learn that skill.")
                return
            }
            learnItemSkill()
            consume()
        }

        private fun spiceMelange() {
            if (contains("too scared to eat any more of that stuff today")) {
                prefBool("spiceMelangeUsed")
                keep()
                return
            }
            if (!contains("You pop the spice melange into your mouth and chew it up")) {
                keep()
                return
            }
            adjustOrgans(fullnessDelta = -3, inebrietyDelta = -3)
            prefBool("spiceMelangeUsed")
            consume()
        }

        private fun sourBall() {
            if (contains("too scared to eat any more of that candy today")) {
                prefBool("_ultraMegaSourBallUsed")
                keep()
                return
            }
            if (!contains("absorbs almost all of the moisture")) {
                keep()
                return
            }
            adjustOrgans(fullnessDelta = -3, inebrietyDelta = -3)
            prefBool("_ultraMegaSourBallUsed")
            consume()
        }

        private fun alienMilk() {
            prefBool("_alienAnimalMilkUsed")
            adjustOrgans(fullnessDelta = -3, inebrietyDelta = 0)
            consume()
        }

        private fun alienPod() {
            prefBool("_alienPlantPodUsed")
            adjustOrgans(fullnessDelta = 0, inebrietyDelta = -3)
            consume()
        }

        private fun dogHair() {
            if (contains("liver can't take any more abuse")) { prefBool("_syntheticDogHairPillUsed"); keep(); return }
            if (!contains("quivers")) { keep(); return }
            adjustOrgans(fullnessDelta = 0, inebrietyDelta = -1)
            prefBool("_syntheticDogHairPillUsed")
            consume()
        }

        private fun distention() {
            if (contains("stomach can't take any more abuse")) { prefBool("_distentionPillUsed"); keep(); return }
            if (!contains("stomach feels rather stretched")) { keep(); return }
            prefBool("_distentionPillUsed")
            consume()
        }

        private fun milk() {
            if (contains("hard on the old gullet")) { prefBool("_milkOfMagnesiumUsed"); keep(); return }
            if (!contains("stomach immediately begins to churn")) { keep(); return }
            prefBool("_milkOfMagnesiumUsed")
            prefBool("milkOfMagnesiumActive")
            consume()
        }

        private fun borrowedTime() {
            prefBool("_borrowedTimeUsed")
            if (contains("already borrowed some time today")) keep() else consume()
        }

        private fun feast() {
            when {
                contains("wait until tomorrow") -> prefInt("_feastUsed", 5)
                contains("chows down") -> {
                    bump("_feastUsed")
                    val familiar = character?.state?.value?.familiarName.orEmpty()
                    val old = preferences?.getString("_feastedFamiliars", "").orEmpty()
                    val next = if (old.isEmpty()) familiar else "$old;$familiar"
                    prefStr("_feastedFamiliars", next)
                }
            }
            keep()
        }

        private fun resolution() {
            if (contains("already feeling adventurous enough")) {
                val extra = 10 - (preferences?.getInt("_resolutionAdv", 0) ?: 0)
                if (extra > 0) bump("_resolutionAdv", extra)
                keep()
                return
            }
            var used = 1
            used += Regex("resolve to do it again", RegexOption.IGNORE_CASE).findAll(html).count()
            bump("_resolutionAdv", 2 * used)
            consume()
        }

        private fun chocolateCigar() {
            prefInt(
                "_chocolateCigarsUsed",
                when {
                    contains("You light the end") -> 1
                    contains("This one doesn't taste") -> 2
                    else -> 3
                },
            )
            consume()
        }

        private fun vitachoc() {
            prefInt(
                "_vitachocCapsulesUsed",
                when {
                    contains("As the nutritive nanobots") -> 1
                    contains("Your body is becoming acclimated") -> 2
                    else -> 3
                },
            )
            consume()
        }

        private fun sculpture() {
            val current = preferences?.getInt("_chocolateSculpturesUsed", 0) ?: 0
            prefInt(
                "_chocolateSculpturesUsed",
                when {
                    contains("doesn't taste as good") -> 2
                    contains("starting to get tired") -> 3
                    contains("didn't enjoy") -> maxOf(current, 4)
                    else -> 1
                },
            )
            consume()
        }

        private fun bearArm() {
            if (!contains("You find a box")) keep()
            else { take(RIGHT_BEAR_ARM); keep() }
        }

        private fun crimboToys() {
            for (id in CRIMBO_TOYS) take(id)
            keep()
        }

        private fun psychoJar() {
            if (!contains("You open the jar and peer inside.")) {
                keep()
                return
            }
            CampgroundInventorySync.setItem(preferences, itemId, 1)
            prefBool("_psychoJarUsed")
            consume()
        }

        private fun sonar() {
            val quests = quests()
            when {
                contains("rubble leading west from Guano Junction collapses in a heap") ->
                    quests?.setQuestIfBetter(Quest.BAT, "step1")
                contains("sound waves knock down the pile of rocks on the east side") ->
                    quests?.setQuestIfBetter(Quest.BAT, "step2")
                contains("high frequency noise makes short work of the rubble") ->
                    quests?.setQuestIfBetter(Quest.BAT, "step3")
            }
            consume()
        }

        private fun charter(pref: String) {
            prefBool(pref)
            if (contains("name gets added to the registry")) consume() else keep()
        }

        private fun dayPass(pref: String) {
            if (contains("already have access to that place")) keep()
            else { prefBool(pref); consume() }
        }

        private fun uraniumSeal() {
            when {
                contains("too many Infernal seals") -> {
                    val claw = INFERNAL_SEAL_CLAW
                    val maxSummons = if (has(claw) || equipmentManager?.hasEquipped(claw) == true) 10 else 5
                    prefInt("_sealsSummoned", maxSummons)
                    abort("Summoning limit reached.")
                }
                contains("Brotherhood of the Smackdown") -> abort("You need more seal-blubber candles.")
                contains("you need 1 imbued seal-blubber candle") -> abort("You need an imbued seal-blubber candle.")
                contains("Only Seal Clubbers may use this item.") -> abort("Only Seal Clubbers may use this item.")
                contains("need to be at least level") -> abort("You are not high enough level.")
                else -> keep()
            }
        }

        private fun evilEye() {
            if (contains("Evilometer emits three quick beeps") && preferences != null) {
                val evilness = minOf(preferences.getInt("cyrptNookEvilness", 0), 3 * count)
                CryptManager.decreaseEvilness(CryptManager.DEFILED_NOOK, evilness, preferences)
            }
            consume()
        }

        private fun keyotron() {
            val prefs = preferences
            if (prefs != null) {
                val match = KEYOTRON_BIODATA.find(html)
                if (match != null) {
                    for (i in 1..9) {
                        val count = match.groupValues.getOrNull(i)?.toIntOrNull() ?: continue
                        val data = BugbearManager.BUGBEAR_DATA.firstOrNull { it.id == i }
                        BugbearManager.setBiodata(data, count, prefs)
                    }
                }
            }
            keep()
        }

        private fun tablet() {
            val name = TABLET_NAME.find(html)?.groupValues?.getOrNull(1)?.trim().orEmpty()
            if (name.isNotEmpty()) prefStr("demonName9", name)
            keep()
        }

        private fun laminated() {
            val prefs = preferences
            if (prefs != null) DwarfFactoryRequest.useLaminatedItem(itemId, html, prefs)
            keep()
        }

        private fun dwarvish() {
            val prefs = preferences
            if (prefs != null) DwarfFactoryRequest.useUnlaminatedItem(itemId, html, prefs)
            keep()
        }

        private fun seeds() {
            val state = character?.state?.value
            if (state != null && !CampgroundAvailability.haveCampground(state)) keep()
            else consume()
        }

        private fun staffGuide() {
            when {
                contains("You don't have time to screw around in a haunted house") ->
                    abort("Insufficient adventures to use a staff guide.")
                contains("You aren't allowed to go to any Haunted Houses right now") ->
                    abort("You aren't allowed to go to any Haunted Houses right now.")
                contains("You don't know where any haunted sorority houses are right now.") ||
                    contains("No way. It's boring in there now that everybody is dead.") ->
                    abort("The Haunted Sorority House is unavailable.")
                else -> consume()
            }
        }

        private fun blackPaint() {
            val state = character?.state?.value
            if (state?.isFistcore == true && contains("Your teachings forbid the use of black paint.")) {
                abort("Your teachings forbid the use of black paint.")
            } else {
                consume()
            }
        }

        private fun burrowgrub() {
            if (contains("It's horrifying.")) prefInt("burrowgrubSummonsRemaining", 3)
            keep()
        }

        private fun adjustOrgans(fullnessDelta: Int, inebrietyDelta: Int) {
            val character = character ?: return
            val s = character.state.value
            character.updateConsumables(
                fullness = (s.fullness + fullnessDelta).coerceAtLeast(0),
                inebriety = (s.inebriety + inebrietyDelta).coerceAtLeast(0),
                spleenUsed = s.spleenUsed,
            )
        }

        private fun learnNamed(name: String) {
            val prefs = preferences ?: return
            val skillId = SkillDefinitionDatabase.getByName(name)?.id ?: return
            SkillLearner.learnSkill(skillId, prefs, skillManager, inventory)
        }

        private fun learnFromHtml() {
            val prefs = preferences ?: return
            SkillLearnFromResponse.learnSkillFromResponse(html, prefs, skillManager, inventory)
        }

        private fun learnItemSkill() {
            val prefs = preferences ?: return
            val name = ItemDatabase.getItemName(itemId)
            if (name.isBlank()) return
            val skill = ModifierDatabase.getStringModifier(name, StringModifier.SKILL)
            if (skill.isBlank()) {
                learnFromHtml()
                return
            }
            learnNamed(skill)
        }
    }

    private val KEYOTRON_BIODATA = Regex(
        "Medbay:</td><td><b>(\\d)/3</b> bio-data segments collected</td></tr>" +
            "<tr><td align=right>Waste Processing:</td><td><b>(\\d)/3</b> bio-data segments collected</td></tr>" +
            "<tr><td align=right>Sonar:</td><td><b>(\\d)/3</b> bio-data segments collected</td></tr>" +
            "<tr><td align=right>Science Lab:</td><td><b>(\\d)/6</b> bio-data segments collected</td></tr>" +
            "<tr><td align=right>Morgue:</td><td><b>(\\d)/6</b> bio-data segments collected</td></tr>" +
            "<tr><td align=right>Special Ops:</td><td><b>(\\d)/6</b> bio-data segments collected</td></tr>" +
            "<tr><td align=right>Engineering:</td><td><b>(\\d)/9</b> bio-data segments collected</td></tr>" +
            "<tr><td align=right>Navigation:</td><td><b>(\\d)/9</b> bio-data segments collected</td></tr>" +
            "<tr><td align=right>Galley:</td><td><b>(\\d)/9</b> bio-data segments collected",
    )
    private val TABLET_NAME = Regex("<font.*?color=#cccccc>(.*?)</font>")

    const val LEGENDARY_BEAT = 4573
    const val PORTABLE_STEAM_UNIT = 11081
    const val TOASTER = 637
    const val ICE_SCULPTURE = 7080
    const val SHAKING_CAMERA = 4170
    const val SHAKING_CRAPPY_CAMERA = 7176
    const val LYNYRD_SNARE = 7204
    const val BAG_O_TRICKS = 4136
    const val ALL_YEAR_SUCKER = 5497
    const val DARK_CHOCOLATE_HEART = 5498
    const val JACKASS_PLUMBER_GAME = 5501
    const val TRIVIAL_AVOCATIONS_GAME = 5502
    const val CREEPY_VOODOO_DOLL = 5062
    const val HOBBY_HORSE = 3092
    const val BALL_IN_A_CUP = 3093
    const val SET_OF_JACKS = 3094
    const val BAG_OF_CANDY = 3261
    const val EMBLEM_AKGYXOTH = 3010
    const val IDOL_AKGYXOTH = 3009
    const val GNOLL_EYE = 3731
    const val KOL_CON_SIX_PACK = 4641
    const val MUS_MANUAL = 2280
    const val MYS_MANUAL = 2281
    const val MOX_MANUAL = 2282
    const val STYX_SPRAY = 3458
    const val STABONIC_SCROLL = 4757
    const val COAL_PAPERWEIGHT = 4905
    const val JINGLE_BELL = 4906
    const val CURSED_KEG = 5664
    const val CURSED_MICROWAVE = 5663
    const val TACO_FLIER = 6051
    const val FISHY_PIPE = 6314
    const val DOLPHIN_WHISTLE = 3997
    const val RUSTY_HEDGE_TRIMMERS = 5115
    const val CARONCH_MAP = 2950
    const val FRATHOUSE_BLUEPRINTS = 2951
    const val SPARKLER = 2679
    const val SNAKE = 2680
    const val M282 = 2681
    const val GREEN_ROCKET = 9827
    const val GONG = ItemDatabase.GONG
    const val D20 = 5290
    const val BGE_TATTOO = 4900
    const val HERMIT_SCRIPT = 567
    const val ENCHANTED_BEAN = 186
    const val LIBRARY_CARD = 2672
    const val HEY_DEZE_MAP = 516
    const val GIANT_CASTLE_MAP = 667
    const val DRASTIC_HEALING = 595
    const val ANTIDOTE = 829
    const val TBONE_KEY = 86
    const val LOCKED_LOCKER = 84
    const val KETCHUP_HOUND = 493
    const val HEY_DEZE_NUTS = 509
    const val PAGODA_PLANS = 502
    const val DOLPHIN_KING_MAP = 26
    const val SLUG_LORD_MAP = 598
    const val DR_HOBO_MAP = 601
    const val ASPARAGUS_KNIFE = 19
    const val SHOPPING_LIST = 602
    const val COBBS_KNOB_MAP = 2442
    const val ENCRYPTION_KEY = 2441
    const val SPOOKY_MAP = 74
    const val SPOOKY_SAPLING = 75
    const val SPOOKY_FERTILIZER = 76
    const val DINGHY_PLANS = 146
    const val DINGY_PLANKS = 140
    const val MORTAR_DISSOLVING_RECIPE = 7495
    const val FENG_SHUI = 210
    const val FOUNTAIN = 211
    const val WINDCHIMES = 212
    const val GATES_SCROLL = 552
    const val DICTIONARY = 536
    const val ELITE_SCROLL = 553
    const val GREEN_THUMB = 6413
    const val REPLICA_GREEN_THUMB = 11221
    const val CHATEAU_ROOM_KEY = 8019
    const val REPLICA_CHATEAU_ROOM_KEY = 11229
    const val GINGERBREAD_CITY = 9203
    const val COUNTERFEIT_CITY = 9204
    const val TELEGRAPH_OFFICE_DEED = 8836
    const val INFLATABLE_TELEGRAPH_OFFICE = 8851
    const val HEART_SHAPED_CRATE = 9316
    const val LOVE_ENTRANCE_PASS = 9330
    const val BEAUTIFUL_RAINBOW = 7712
    const val THUNDER_THIGH = 7648
    const val AQUA_BRAIN = 7647
    const val LIGHTNING_MILK = 7646
    const val OLFACTION_BOOK = 2463
    const val TEACHINGS_OF_THE_FIST = 5220
    const val BOOKE_OF_VAMPYRIC_KNOWLEDGE = 10180
    const val SLIME_SOAKED_HYPOPHYSIS = 3991
    const val SLIME_SOAKED_BRAIN = 3992
    const val SLIME_SOAKED_SWEAT_GLAND = 3993
    const val WESTERN_SLANG_VOL_1 = 8920
    const val WESTERN_SLANG_VOL_2 = 8921
    const val WESTERN_SLANG_VOL_3 = 8922
    const val SPICE_MELANGE = 3433
    const val ULTRA_MEGA_SOUR_BALL = 6852
    const val ALIEN_ANIMAL_MILK = 9429
    const val ALIEN_PLANT_POD = 9421
    const val SYNTHETIC_DOG_HAIR_PILL = 5167
    const val DISTENTION_PILL = 5168
    const val MILK_OF_MAGNESIUM = 1650
    const val BORROWED_TIME = 5232
    const val MOVEABLE_FEAST = 4135
    const val RESOLUTION_ADVENTUROUS = 5471
    const val ESSENTIAL_TOFU = 4609
    const val CHOCOLATE_CIGAR = 4851
    const val VITACHOC_CAPSULE = 3091
    const val CHOCOLATE_SCULPTURE = 9269
    const val LOVE_CHOCOLATE = 9325
    const val EXTRA_TIME = 11347
    const val BOX_OF_HAMMERS = 5233
    const val TEMPURA_AIR = 4133
    const val PRESSURIZED_PNEUMATICITY = 4134
    const val HYPERINFLATED_SEAL_LUNG = 3935
    const val BALLAST_TURTLE = 4005
    const val STUFFED_POCKETWATCH = 4545
    const val LEFT_BEAR_ARM = 5792
    const val RIGHT_BEAR_ARM = 5791
    const val SONAR = 563
    const val SPRING_BEACH_CHARTER = 7466
    const val SPRING_BEACH_TICKET = 7467
    const val SPRING_BEACH_TATTOO_COUPON = 7465
    const val ANTI_FUNGAL_SPRAY = 7461
    const val CONSPIRACY_ISLAND_CHARTER = 7767
    const val CONSPIRACY_ISLAND_TICKET = 7768
    const val DINSEY_CHARTER = 8203
    const val DINSEY_TICKET = 8204
    const val VOLCANO_CHARTER = 8487
    const val VOLCANO_TICKET = 8486
    const val SHAWARMA_KEYCARD = 7792
    const val BOTTLE_OPENER_KEYCARD = 7793
    const val ARMORY_KEYCARD = 7794
    const val JUICY_GARBAGE = 5684
    const val PEN_PAL_KIT = 5112
    const val NEW_YOU_CLUB_MEMBERSHIP_FORM = 9478
    const val SEAL_IRON_INGOT = 3932
    const val DEPLETED_URANIUM_SEAL = 4296
    const val INFERNAL_SEAL_CLAW = 4322
    const val EVIL_EYE = CryptManager.EVIL_EYE
    const val EVILOMETER = CryptManager.EVILOMETER
    const val KEYOTRON = 5653
    const val SINISTER_ANCIENT_TABLET = 4706
    const val BOOZEHOUND_TOKEN = 3739
    const val CLANCY_SACKBUT = 5547
    const val CLANCY_CRUMHORN = 5549
    const val CLANCY_LUTE = 5551
    const val STAFF_GUIDE = 5307
    const val BLACK_PAINT = 2327
    const val BURROWGRUB_HIVE = 3629
    const val AMINO_ACIDS = 4006
    const val TEARS = 869
    const val TELESCOPE = 2599
    const val WORKYTIME_TEA = 4866
    const val WARM_SUBJECT = 621
    const val MINING_OIL = 7856
    const val TAINTED_MINING_OIL = 8017
    const val DUSTY_ANIMAL_SKULL = 1799
    const val ANCIENT_CURSED_FOOTLOCKER = 3016
    const val ORNATE_CURSED_CHEST = 3017
    const val GILDED_CURSED_CHEST = 3018
    const val STUFFED_CHEST = 3949
    const val SIMPLE_CURSED_KEY = 3013
    const val ORNATE_CURSED_KEY = 3014
    const val GILDED_CURSED_KEY = 3015
    const val STUFFED_KEY = 3950
    const val GENERAL_ASSEMBLY_MODULE = 3075
    const val LASER_CANON = 3069
    const val LASER_TARGETING_CHIP = 3076
    const val UNOBTAINIUM_STRAPS = 3073
    const val FASTENING_APPARATUS = 3074
    const val LEG_ARMOR = 3077
    const val GLUTEAL_SHIELD = 3071
    const val CARBONITE_VISOR = 3072
    const val CHIN_STRAP = 3070
    const val KEVLATEFLOCITE_HELMET = 3078
    const val MAID = 1000
    const val CLOCKWORK_MAID = 1113
    const val MEAT_BUTLER = 11262
    const val PORTABLE_HOUSEKEEPING_ROBOT = 11377
    const val SCARECROW = 104
    const val MEAT_GOLEM = 101
    const val BLACK_BLUE_LIGHT = 3276
    const val LOUDMOUTH_LARRY = 3277
    const val PLASMA_BALL = 3281
    const val MEAT_GLOBE = 636
    const val LED_CLOCK = 6072
    const val BONSAI_TREE = 6120
    const val OUTRAGEOUS_SOMBRERO = 2548
    const val NEVERENDING_SODA = 3393
    const val AUGMENTED_DRONE = 3167
    const val OVERCHARGED_POWER_SPHERE = 3215
    const val TRAPEZOID = 3198
    const val PERSONAL_MASSAGER = 3279
    const val GRUB = 3356
    const val MOTH = 3357
    const val FIRE_ANT = 3358
    const val ICE_ANT = 3359
    const val STINKBUG = 3360
    const val DEATH_WATCH_BEETLE = 3361
    const val LOUSE = 3362
    const val HONEYPOT = 5145
    const val QUASIRELGIOUS_SCULPTURE = 6667
    const val SOLID_GOLD_ROSARY = 7149
    const val NEWBIESPORT_TENT = 69
    const val BARSKIN_TENT = 73
    const val COTTAGE = 143
    const val HOUSE = 526
    const val SANDCASTLE = 3127
    const val TWIG_HOUSE = 3374
    const val GINGERBREAD_HOUSE = 4347
    const val HOBO_FORTRESS = 3416
    const val BRICKO_PYRAMID = 4485
    const val GIANT_FARADAY_CAGE = 6668
    const val SNOW_FORT = 7089
    const val ELEVENT = 7295
    const val RESIDENCE_CUBE = 7758
    const val GIANT_PILGRIM_HAT = 9185
    const val HOUSE_SIZED_MUSHROOM = 10497
    const val MINI_KIWI_TIPI = 11600

    private val DWELLINGS = setOf(
        NEWBIESPORT_TENT,
        BARSKIN_TENT,
        COTTAGE,
        HOUSE,
        SANDCASTLE,
        TWIG_HOUSE,
        GINGERBREAD_HOUSE,
        HOBO_FORTRESS,
        BRICKO_PYRAMID,
        GIANT_FARADAY_CAGE,
        SNOW_FORT,
        ELEVENT,
        RESIDENCE_CUBE,
        GIANT_PILGRIM_HAT,
        HOUSE_SIZED_MUSHROOM,
        MINI_KIWI_TIPI,
    )

    val MAIDS = setOf(MAID, CLOCKWORK_MAID, MEAT_BUTLER, PORTABLE_HOUSEKEEPING_ROBOT)
    val BANG_POTIONS = (ItemDatabase.FIRST_BANG_POTION..ItemDatabase.LAST_BANG_POTION).toSet()
    val SLIME_VIALS = (ItemDatabase.FIRST_SLIME_VIAL until ItemDatabase.LAST_SLIME_VIAL).toSet()

    val BRICKO_FIGHTS = setOf(4474, 4475, 4476, 4477, 4478, 4479, 4480, 4481, 4482, 4483, 4484)
    val FOSSIL_SKULLS = setOf(4687, 4688, 4689, 4690, 4704, 4705)
    val RECORDINGS = setOf(4497, 4498, 4499, 4500, 4501, 4502, 4503)
    val SNOWCONES = setOf(1412, 1413, 1414, 1415, 1416, 1417)
    val FANCY_CHOCOLATES = setOf(1382, 2197, 4334, 4462, 4463, 4464, 4465, 4466, 4467, 5915, 5916, 5917, 7999)
    val CRIMBO_TOYS = setOf(6058, 6059, 6060, 6061, 6062)
    val PSYCHO_JARS = setOf(5898, 5899, 5900, 5901, 5902, 5903, 5905)
    val SEAL_FIGHTS = setOf(3902, 3903, 3904, 3905, 3906, 3907, 3908, 3909, 3910, 3911)
    val LAMINATED = setOf(3208, 3209, 3210, 3211)
    val DWARVISH = setOf(3212, 3213, 3214)
    val SEEDS = setOf(5404, 4760, 5880, 6751, 7070, 9186, 9760, 10482, 11100)
    val COSTUMES = setOf(5312, 5313, 5314, 5315, 5316)
}
