package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.adventure.RufusManager
import net.sourceforge.kolmafia.adventure.choice.ChoiceAdventures
import net.sourceforge.kolmafia.adventure.choice.ChoiceOption
import net.sourceforge.kolmafia.adventure.choice.ChoiceUtilities
import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ModifierParser
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.ToppingPeakNcSync
import net.sourceforge.kolmafia.request.FloristRequest
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

/**
 * Desktop [ChoiceAdventures.dynamicChoiceSpoilers] / [dynamicChoiceOptions] router
 * (Behavioral Deepen XXVIII–XXX).
 *
 * Inventory/prefs/equipment/effect providers are set from [GameRuntimeLibrary] so static
 * managers can emit inventory-aware spoiler text without DI constructors.
 */
object DynamicChoiceSpoilers {

    var preferences: Preferences? = null
    var questDatabase: QuestDatabase? = null
    var itemCount: (Int) -> Int = { 0 }
    var ascensions: () -> Int = { 0 }
    var characterClass: () -> CharacterClass? = { null }
    var hasEquipped: (Int) -> Boolean = { false }
    var hasEffect: (String) -> Boolean = { false }

    // ── Track A / C DI providers (Behavioral Deepen XXX) ──────────────────────
    /** Desktop KoLCharacter.getElementalResistanceLevels(element) — levels of resistance. */
    var elementalResistanceLevels: (String) -> Int = { 0 }
    /** Desktop KoLCharacter.elementalResistanceByLevel(levels) — resistance %. */
    var elementalResistanceByLevel: (Int) -> Double = { 0.0 }
    /** Desktop KoLCharacter.getAdjustedMuscle(). */
    var buffedMuscle: () -> Int = { 0 }
    /** Desktop KoLCharacter.getAdjustedMysticality(). */
    var buffedMyst: () -> Int = { 0 }
    /** Desktop KoLCharacter.getAdjustedMoxie(). */
    var buffedMoxie: () -> Int = { 0 }
    /** Desktop KoLCharacter.getCurrentHP(). */
    var currentHP: () -> Long = { 0L }
    /** Desktop KoLCharacter.getCurrentMP(). */
    var currentMP: () -> Long = { 0L }
    /** Desktop KoLCharacter.getInebriety(). */
    var inebriety: () -> Int = { 0 }
    /** Desktop KoLCharacter.getInitiativeAdjustment(). */
    var initiativeAdjustment: () -> Double = { 0.0 }
    /** Desktop KoLCharacter.getItemDropPercentAdjustment(). */
    var itemDropPercent: () -> Double = { 0.0 }
    /** Desktop KoLCharacter.currentNumericModifier(FOODDROP). */
    var foodDropPercent: () -> Double = { 0.0 }

    // ── Overlook Lodge familiar-exclusion DI providers (Group E) ──────────────
    /** True when an active familiar is present (desktop FamiliarData != NO_FAMILIAR). */
    var hasActiveFamiliar: () -> Boolean = { false }
    /** Familiar ITEMDROP bonus (active familiar species modifier from ModifierDatabase). */
    var activeFamiliarItemDrop: () -> Double = { 0.0 }
    /** Familiar FOODDROP bonus (active familiar species modifier). */
    var activeFamiliarFoodDrop: () -> Double = { 0.0 }
    /** Crown of Thrones (enthroned familiar) ITEMDROP bonus. */
    var enthronedItemDrop: () -> Double = { 0.0 }
    /** Buddy Bjorn (bjorned familiar) ITEMDROP bonus. */
    var bjornedItemDrop: () -> Double = { 0.0 }
    /** Florist Friar plants at Twin Peak ITEMDROP bonus. */
    var floristTwinPeakItemDrop: () -> Double = { 0.0 }
    /** Clancy lute ITEMDROP bonus (minstrel level weight-based). */
    var clancyLuteItemDrop: () -> Double = { 0.0 }
    /** Jarlsberg Eggman companion ITEMDROP bonus. */
    var eggmanItemDrop: () -> Double = { 0.0 }
    /** Ed the Undying cat servant ITEMDROP bonus. */
    var edCatServantItemDrop: () -> Double = { 0.0 }
    /** Desktop KoLCharacter.currentBonusDamage(). */
    var currentBonusDamage: () -> Int = { 0 }
    /** Desktop KoLCharacter.currentPrismaticDamage(). */
    var currentPrismaticDamage: () -> Int = { 0 }
    /** Desktop KoLCharacter.estimatedPoolSkill(). */
    var estimatedPoolSkill: () -> Int = { 0 }
    /** Desktop ChoiceManager.lastResponseText — for HTML-parsing spoilers. */
    var lastResponseText: () -> String = { ChoiceCombatAshState.lastChoiceResponseText }
    /** Desktop hasSkill(int) check. */
    var hasSkillId: (Int) -> Boolean = { false }
    /** Desktop EquipmentManager.isWearingOutfit(outfitId). */
    var isWearingOutfit: (Int) -> Boolean = { false }
    /** Desktop Preferences.getString("lastEncounter"). */
    var lastEncounter: () -> String = { "" }
    /** Desktop KoLCharacter.currentNumericModifier(modifierName) — for element resistance. */
    var numericModifier: (String) -> Double = { 0.0 }
    // Desktop ItemPool ids used by dynamic spoilers.
    const val BEAUTIFUL_SOUP = 4511
    const val WALRUS_ICE_CREAM = 4510
    const val HUMPTY_DUMPLINGS = 4514
    const val LOBSTER_QUA_GRILL = 4515
    const val MISSING_WINE = 4516
    const val NOSTRIL_OF_THE_SERPENT = 5645
    const val MAKESHIFT_TURBAN = 2079
    const val SLEEP_MASK = 4241
    const val WAX_BANANA = 6492
    const val WAX_LOCK_IMPRESSION = 6493
    const val REPLICA_KEY = 6494
    const val AUDITORS_BADGE = 6495
    const val BLOOD_KIWI = 6496
    const val EAU_DE_MORT = 6497
    const val BLOODY_KIWITINI = 6498
    const val MOON_AMBER = 6499
    const val MOON_AMBER_NECKLACE = 6501
    const val DREADSYLVANIAN_CLOCKWORK_KEY = 6506
    const val COOL_IRON_INGOT = 6507
    const val GHOST_SHAWL = 6511
    const val WARM_FUR = 6513
    const val GHOST_THREAD = 6525
    const val HOTHAMMER = 6528
    const val MUDDY_SKIRT = 6531
    const val OLD_BALL_AND_CHAIN = 6539
    const val OLD_DRY_BONE = 6540
    const val WEEDY_SKIRT = 6545
    const val DREAD_TARRAGON = 6484
    const val BONE_FLOUR = 6485
    const val DREADFUL_ROAST = 6486
    const val STINKING_AGARICUS = 6487
    const val SHEPHERDS_PIE = 6488
    const val INTRICATE_MUSIC_BOX_PARTS = 6489
    const val DREADSYLVANIAN_SKELETON_KEY = 6424
    const val HELPS_YOU_SLEEP = 6443
    const val FOLDER_HOLDER = 6617
    const val REPLICA_FOLDER_HOLDER = 11220
    const val VALUABLE_TRINKET = 139

    // ── Skill IDs (desktop SkillPool) ───────────────────────────────────────
    const val WORKING_LUNCH_SKILL_ID = 14016
    const val JARLSBERG_COMPANION_PREF = "jarlsbergCompanion"
    private const val SKILL_DRIPPY_EYE_SPROUT = 191
    private const val SKILL_DRIPPY_EYE_STONE = 192
    private const val SKILL_DRIPPY_EYE_BEETLE = 193

    // ── Item IDs (desktop ItemPool) used by new spoilers ─────────────────────
    private const val DRIPPY_STAFF = 10526
    private const val DRIPPY_STEIN = 10524
    private const val YEARBOOK_CAMERA = 6678

    // New item IDs for Track A Batch 1 spoilers.
    private const val INEXPLICABLY_GLOWING_ROCK = 1121
    private const val SPOOKY_GLOVE = 1125
    private const val MULLET_WIG = 267
    private const val BRIEFCASE = 184
    private const val FRILLY_SKIRT = 131
    private const val HOT_WING = 471
    private const val HOBO_NICKEL = 3126
    private const val HOBO_CODE_BINDER = 3220
    private const val SEED_PACKET = 3553
    private const val GREEN_SLIME = 3554
    private const val MERKIN_PRESSUREGLOBE = 3675
    private const val WOODEN_STAKES = 71
    private const val VAMPIRE_HEART = 1518
    private const val BAR_SKIN = 70
    private const val SPOOKY_SAPLING = 75
    private const val SPOOKY_MAP = 74
    private const val TREE_HOLED_COIN = 4676
    private const val SPOOKY_FERTILIZER = 76
    private const val KNOB_GOBLIN_POLEARM = 310
    private const val KNOB_GOBLIN_PANTS = 309
    private const val KNOB_GOBLIN_HELM = 308
    private const val GLOWING_FUNGUS = 5641
    private const val TITANIUM_UMBRELLA = 596
    private const val UNBREAKABLE_UMBRELLA = 10899
    private const val EXTREME_AMULET = 594
    private const val MOHAWK_WIG = 597
    private const val MCCLUSKY_FILE = 6689
    private const val BINDER_CLIP = 7040
    private const val STONE_TRIANGLE = 7041

    // Desktop OutfitPool IDs.
    private const val FRAT_OUTFIT = 3

    // Desktop effect names.
    private const val EFFECT_ONCE_CURSED = "Once-Cursed"
    private const val EFFECT_TWICE_CURSED = "Twice-Cursed"
    private const val EFFECT_THRICE_CURSED = "Thrice-Cursed"
    private const val EFFECT_JOCK_JAMS = "Jamming with the Jocks"
    private const val EFFECT_NERD_WORD = "Nerd Is the Word"
    private const val EFFECT_GREASER_LIGHTNIN = "Greaser Lightnin'"

    /** Desktop defenseToElement mapping for CyberRealm zone half-way choices. */
    private val defenseToElement = mapOf(
        "firewall" to "hot",
        "ICE barrier" to "cold",
        "corruption quarantine" to "stench",
        "parental controls" to "sleaze",
        "null container" to "spooky",
    )

    /** Desktop encounterToElement mapping for CyberRealm zone half-way choices. */
    private val encounterToElement = mapOf(
        "A Funny Thing Happened..." to "hot",
        "A Turboclocked System" to "hot",
        "Boiling Chrome" to "hot",
        "Cracklin' Node" to "hot",
        "A Breezy System" to "cold",
        "A Frozen Network" to "cold",
        "A Severely Underclocked Network" to "cold",
        "Ice Cream Antisocial" to "cold",
        "A Terminal Disease" to "stench",
        "Arsenic & Old Spice" to "stench",
        "One Man's TRS-80" to "stench",
        "People Have Weird Hobbies Sometimes" to "stench",
        "\$1.00,\$1.00,\$1.00" to "sleaze",
        "I Live, You Live..." to "sleaze",
        "pr0n Central" to "sleaze",
        "The Piggy Bank" to "sleaze",
        "A spooky encounter" to "spooky",
        "Grave Secrets" to "spooky",
        "The Fall of the Homepage of Usher" to "spooky",
        "The Skeleton Dance" to "spooky",
    )

    private const val EFFECT_FIRST_BLOOD_KIWI = "First Blood Kiwi"
    private const val EFFECT_SHEPHERDS_BREATH = "Shepherd's Breath"
    private const val EFFECT_TEMPORARY_BLINDNESS = "Temporary Blindness"

    // Desktop effect names for A-Boo Peak elemental forms.
    private const val EFFECT_SPOOKYFORM = "Spooky Form"
    private const val EFFECT_COLDFORM = "Cold Form"
    private const val EFFECT_SLEAZEFORM = "Sleaze Form"
    private const val EFFECT_STENCHFORM = "Stench Form"

    fun choiceSpoilers(choice: Int): ChoiceAdventures.Spoilers? {
        if (choice <= 0) return null
        HaciendaManager.getSpoilers(choice)?.let { return it }
        when (choice) {
            5 -> return heartOfDarknessSpoilers()
            7 -> return howDepressingSpoilers()
            184 -> return eyepatchesSpoilers()
            185 -> return rockStarrrSpoilers()
            187 -> return beerPongSpoilers()
            188 -> return infiltrationistSpoilers()
            191 -> return chatterboxingSpoilers()
            272 -> return marketplaceEntranceSpoilers()
            298 -> return inTheShadeSpoilers()
            304 -> return ventHorizonSpoilers()
            305 -> return sauceBottomSpoilers()
            309 -> return barbackSpoilers()
            360 -> return wumpusSpoilers()
            442 -> return rabbitHoleSpoilers()
            502 -> return arborealRespiteSpoilers()
            522 -> return footlockerSpoilers()
            579 -> return suchGreatHeightsSpoilers()
            580 -> return hiddenHeartSpoilers()
            581 -> return suchGreatDepthsSpoilers()
            582 -> return fittingInSpoilers()
            606 -> return overlookLodgeSpoilers()
            611 -> return booPeakSpoilers()
            in 636..639 -> return oldManPsychosisSpoilers()
            641, 642, 644, 645, 647, 648, 650, 651 -> return mysticPsychosisSpoilers(choice)
            669 -> return fastAndFurryousSpoilers()
            670 -> return gymSpoilers()
            678 -> return punkRockGiantSpoilers()
            692 -> return dailyDungeonDoorSpoilers()
            696 -> return stickAForkInItSpoilers()
            697 -> return sophiesChoiceSpoilers()
            698 -> return fromBadToWorstSpoilers()
            700 -> return deliriumCafeteriaSpoilers()
            704 -> return merkinCatalogSpoilers()
            in 721..724 -> return dreadsylvaniaCabinSpoilers(choice)
            in 725..760 -> return dreadsylvaniaSpoilers(choice)
            772 -> return kolhsSpoilers()
            780 -> return actionElevatorSpoilers()
            781 -> return earthboundSpoilers()
            783 -> return waterYouDuneSpoilers()
            784 -> return youMdSpoilers()
            785 -> return airApparentSpoilers()
            786 -> return workingHolidaySpoilers()
            787 -> return fireWhenReadySpoilers()
            788 -> return cherryOfBowlsSpoilers()
            789 -> return garbagesterSpoilers()
            791 -> return legendOfTempleSpoilers()
            801 -> return reanimatedConversationSpoilers()
            918 -> return yachtzeeSpoilers()
            988 -> return eveContainmentSpoilers()
            1049 -> return tombSpoilers()
            1411 -> return drippyHallSpoilers()
            1489 -> return slaggingOffSpoilers()
            1499 -> return shadowLabyrinthSpoilers()
            1545 -> return cyberHalfWaySpoilers(1, "_cyberZone1Defense")
            1547 -> return cyberHalfWaySpoilers(2, "_cyberZone2Defense")
            1549 -> return cyberHalfWaySpoilers(3, "_cyberZone3Defense")
            1598 -> return baseballSpoilers()
        }
        return null
    }

    private fun chatterboxingSpoilers(): ChoiceAdventures.Spoilers {
        val trinks = itemCount(VALUABLE_TRINKET)
        val banish = if (trinks == 0) {
            "lose hp (no valuable trinkets)"
        } else {
            "use valuable trinket to banish ($trinks in inventory)"
        }
        return ChoiceAdventures.Spoilers(
            191,
            "Chatterboxing",
            listOf(
                ChoiceOption("moxie substats"),
                ChoiceOption(banish),
                ChoiceOption("muscle substats"),
                ChoiceOption("mysticality substats"),
            ),
        )
    }

    private fun lockSpoiler(): String {
        val key = itemCount(DREADSYLVANIAN_SKELETON_KEY)
        return if (key > 0) "possibly locked, key in inventory: "
        else "possibly locked, no key in inventory: "
    }

    private fun shortcutSpoiler(pref: String): ChoiceOption {
        val known = preferences?.getBoolean(pref, false) == true
        return ChoiceOption(if (known) "shortcut KNOWN" else "learn shortcut")
    }

    private fun isMuscle(): Boolean = characterClass()?.let {
        it == CharacterClass.SEAL_CLUBBER || it == CharacterClass.TURTLE_TAMER
    } == true

    private fun isMyst(): Boolean = characterClass()?.let {
        it == CharacterClass.PASTAMANCER || it == CharacterClass.SAUCEROR
    } == true

    private fun isMoxie(): Boolean = characterClass()?.let {
        it == CharacterClass.DISCO_BANDIT || it == CharacterClass.ACCORDION_THIEF
    } == true

    private fun gallowsItem(): String = when {
        isMuscle() -> "hangman's hood"
        isMyst() -> "cursed ring finger ring"
        isMoxie() -> "Dreadsylvanian clockwork key"
        else -> "nothing"
    }

    private fun muddySkirtSpoiler(): String = when {
        hasEquipped(MUDDY_SKIRT) -> "equipped muddy skirt -> weedy skirt and "
        itemCount(MUDDY_SKIRT) > 0 -> "(muddy skirt in inventory but not equipped) "
        else -> ""
    }

    private fun shepherdPieSpoiler(): String =
        "dread tarragon (${itemCount(DREAD_TARRAGON)}) + dreadful roast " +
            "(${itemCount(DREADFUL_ROAST)}) + bone flour (${itemCount(BONE_FLOUR)}) + " +
            "stinking agaricus (${itemCount(STINKING_AGARICUS)}) -> Dreadsylvanian shepherd's pie"

    private fun coolingIronSpoiler(): String =
        "cool iron ingot (${itemCount(COOL_IRON_INGOT)}) + warm fur " +
            "(${itemCount(WARM_FUR)}) -> cooling iron equipment"

    private fun equippedOrAvailable(
        itemId: Int,
        equippedLabel: String,
        availableLabel: String,
        missingLabel: String,
    ): String = when {
        hasEquipped(itemId) -> equippedLabel
        itemCount(itemId) > 0 -> availableLabel
        else -> missingLabel
    }

    private fun wumpusSpoilers(): ChoiceAdventures.Spoilers {
        val warnings = WumpusManager.dynamicChoiceOptions()
        val options = if (warnings.isEmpty()) {
            listOf(ChoiceOption(""), ChoiceOption(""))
        } else {
            warnings.map { ChoiceOption(it) }
        }
        return ChoiceAdventures.Spoilers(360, "The Jungles of Ancient Loathing", options)
    }

    private fun rabbitHoleSpoilers(): ChoiceAdventures.Spoilers {
        var count = 0
        if (itemCount(BEAUTIFUL_SOUP) > 0) count++
        if (itemCount(LOBSTER_QUA_GRILL) > 0) count++
        if (itemCount(MISSING_WINE) > 0) count++
        if (itemCount(WALRUS_ICE_CREAM) > 0) count++
        if (itemCount(HUMPTY_DUMPLINGS) > 0) count++
        val options = listOf(
            ChoiceOption("Seal Clubber/Pastamancer item, or yellow matter custard"),
            ChoiceOption("Sauceror/Accordion Thief item, or delicious comfit?"),
            ChoiceOption("Disco Bandit/Turtle Tamer item, or fight croqueteer"),
            ChoiceOption("you have $count/5 of the items needed for an ittah bittah hookah"),
            ChoiceOption("get a chess cookie"),
            ChoiceOption("skip adventure"),
        )
        return ChoiceAdventures.Spoilers(442, "Rabbit Hole", options)
    }

    private fun suchGreatHeightsSpoilers(): ChoiceAdventures.Spoilers {
        val prefs = preferences
        val haveNostril = itemCount(NOSTRIL_OF_THE_SERPENT) > 0
        val asc = ascensions()
        val gainNostril = !haveNostril &&
            (prefs?.getInt("lastTempleButtonsUnlock", 0) ?: 0) != asc
        val templeAdvs = (prefs?.getInt("lastTempleAdventures", 0) ?: 0) == asc
        val options = listOf(
            ChoiceOption("mysticality substats"),
            if (gainNostril) ChoiceOption("gain the Nostril of the Serpent")
            else ChoiceOption("skip adventure"),
            if (templeAdvs) ChoiceOption("skip adventure")
            else ChoiceOption("gain 3 adventures"),
        )
        return ChoiceAdventures.Spoilers(579, "Such Great Heights", options)
    }

    private fun dreadsylvaniaCabinSpoilers(choice: Int): ChoiceAdventures.Spoilers? {
        val muscle = isMuscle()
        val accordion = characterClass() == CharacterClass.ACCORDION_THIEF
        val bones = itemCount(OLD_DRY_BONE)
        val replica = itemCount(REPLICA_KEY)
        val banana = itemCount(WAX_BANANA)
        val options = when (choice) {
            721 -> {
                val kitchen = buildString {
                    append("dread tarragon")
                    if (muscle) append(", old dry bone ($bones) -> bone flour")
                    append(", -stench")
                }
                val cellar = "Freddies, Bored Stiff (+100 spooky damage), " +
                    "replica key ($replica) -> Dreadsylvanian auditor's badge, " +
                    "wax banana ($banana) -> complicated lock impression"
                val attic = buildString {
                    append(lockSpoiler())
                    append("-spooky")
                    if (accordion) append(" + intricate music box parts")
                    append(", fewer werewolves, fewer vampires, +Moxie")
                }
                listOf(
                    ChoiceOption(kitchen),
                    ChoiceOption(cellar),
                    ChoiceOption(attic),
                    null,
                    shortcutSpoiler("ghostPencil1"),
                    ChoiceOption("Leave this noncombat"),
                )
            }
            722 -> listOf(
                ChoiceOption("dread tarragon"),
                ChoiceOption("old dry bone ($bones) -> bone flour"),
                ChoiceOption("-stench"),
                null,
                null,
                ChoiceOption("Return to The Cabin"),
            )
            723 -> listOf(
                ChoiceOption("Freddies"),
                ChoiceOption("Bored Stiff (+100 spooky damage)"),
                ChoiceOption("replica key ($replica) -> Dreadsylvanian auditor's badge"),
                ChoiceOption("wax banana ($banana) -> complicated lock impression"),
                null,
                ChoiceOption("Return to The Cabin"),
            )
            724 -> listOf(
                ChoiceOption(
                    buildString {
                        append("-spooky")
                        if (accordion) append(" + intricate music box parts")
                    },
                ),
                ChoiceOption("fewer werewolves"),
                ChoiceOption("fewer vampires"),
                ChoiceOption("+Moxie"),
                null,
                ChoiceOption("Return to The Cabin"),
            )
            else -> return null
        }
        val name = when (choice) {
            721 -> "The Cabin in the Dreadsylvanian Woods"
            722 -> "The Kitchen in the Woods"
            723 -> "What Lies Beneath (the Cabin)"
            724 -> "Where it's Attic"
            else -> "Dreadsylvania"
        }
        return ChoiceAdventures.Spoilers(choice, name, options)
    }

    private fun dreadsylvaniaSpoilers(choice: Int): ChoiceAdventures.Spoilers? {
        val options = when (choice) {
            725 -> {
                val climbTree = if (isMuscle()) {
                    "drop blood kiwi, -sleaze, moon-amber"
                } else {
                    "unavailable (Muscle class only)"
                }
                val fireTower = lockSpoiler() + "fewer ghosts, Freddies, +Muscle"
                val baseOfTree = buildString {
                    append("blood kiwi (from above), Dreadsylvanian seed pod")
                    if (hasEquipped(FOLDER_HOLDER) || hasEquipped(REPLICA_FOLDER_HOLDER)) {
                        append(", folder (owl)")
                    }
                }
                listOf(
                    ChoiceOption(climbTree),
                    ChoiceOption(fireTower),
                    ChoiceOption(baseOfTree),
                    null,
                    shortcutSpoiler("ghostPencil2"),
                    ChoiceOption("Leave this noncombat"),
                )
            }
            726 -> listOf(
                ChoiceOption("drop blood kiwi"),
                ChoiceOption("-sleaze"),
                ChoiceOption("moon-amber"),
                null,
                null,
                ChoiceOption("Return to The Tallest Tree"),
            )
            727 -> listOf(
                ChoiceOption("fewer ghosts"),
                ChoiceOption("Freddies"),
                ChoiceOption("+Muscle"),
                null,
                null,
                ChoiceOption("Return to The Tallest Tree"),
            )
            728 -> listOf(
                ChoiceOption("blood kiwi (from above)"),
                ChoiceOption("Dreadsylvanian seed pod"),
                ChoiceOption("folder (owl)"),
                null,
                null,
                ChoiceOption("Return to The Tallest Tree"),
            )
            729 -> {
                val hot = "-hot, Dragged Through the Coals (+100 hot damage), " +
                    "old ball and chain (${itemCount(OLD_BALL_AND_CHAIN)}) -> cool iron ingot"
                listOf(
                    ChoiceOption(hot),
                    ChoiceOption("-cold, +Mysticality, Nature's Bounty (+300 max HP)"),
                    ChoiceOption("fewer bugbears, Freddies"),
                    null,
                    shortcutSpoiler("ghostPencil3"),
                    ChoiceOption("Leave this noncombat"),
                )
            }
            730 -> listOf(
                ChoiceOption("-hot"),
                ChoiceOption("Dragged Through the Coals (+100 hot damage)"),
                ChoiceOption(
                    "old ball and chain (${itemCount(OLD_BALL_AND_CHAIN)}) -> cool iron ingot",
                ),
                null,
                null,
                ChoiceOption("Return to The Burrows"),
            )
            731 -> listOf(
                ChoiceOption("-cold"),
                ChoiceOption("+Mysticality"),
                ChoiceOption("Nature's Bounty (+300 max HP)"),
                null,
                null,
                ChoiceOption("Return to The Burrows"),
            )
            732 -> listOf(
                ChoiceOption("fewer bugbears"),
                ChoiceOption("Freddies"),
                null,
                null,
                null,
                ChoiceOption("Return to The Burrows"),
            )
            733 -> {
                val schoolhouse = lockSpoiler() + "fewer ghosts, ghost pencil, +Mysticality"
                val blacksmith = buildString {
                    append("-cold, Freddies")
                    if (itemCount(HOTHAMMER) > 0) {
                        append(", ${coolingIronSpoiler()}")
                    }
                }
                val gallows = "-spooky, gain ${gallowsItem()} with help of clannie " +
                    "or help clannie gain an item"
                listOf(
                    ChoiceOption(schoolhouse),
                    ChoiceOption(blacksmith),
                    ChoiceOption(gallows),
                    null,
                    shortcutSpoiler("ghostPencil4"),
                    ChoiceOption("Leave this noncombat"),
                )
            }
            734 -> listOf(
                ChoiceOption("fewer ghosts"),
                ChoiceOption("ghost pencil"),
                ChoiceOption("+Mysticality"),
                null,
                null,
                ChoiceOption("Return to The Village Square"),
            )
            735 -> listOf(
                ChoiceOption("-cold"),
                ChoiceOption("Freddies"),
                ChoiceOption(coolingIronSpoiler()),
                null,
                null,
                ChoiceOption("Return to The Village Square"),
            )
            736 -> listOf(
                ChoiceOption("-spooky"),
                ChoiceOption("gain ${gallowsItem()} with help of clannie"),
                null,
                ChoiceOption("help clannie gain an item"),
                null,
                ChoiceOption("Return to The Village Square"),
            )
            737 -> {
                val tickingShack = if (isMoxie()) {
                    buildString {
                        append("Freddies")
                        append(", lock impression (${itemCount(WAX_LOCK_IMPRESSION)}) + music box parts ")
                        append("(${itemCount(INTRICATE_MUSIC_BOX_PARTS)}) -> replica key")
                        append(", moon-amber (${itemCount(MOON_AMBER)}) -> polished moon-amber")
                        append(", 3 music box parts (${itemCount(INTRICATE_MUSIC_BOX_PARTS)}) + ")
                        append("clockwork key (${itemCount(DREADSYLVANIAN_CLOCKWORK_KEY)}) -> ")
                        append("mechanical songbird, 3 lengths of old fuse")
                    }
                } else {
                    "unavailable (Moxie class only)"
                }
                listOf(
                    ChoiceOption("-stench, Sewer-Drenched (+100 stench damage)"),
                    ChoiceOption("fewer skeletons, -sleaze, +Muscle"),
                    ChoiceOption(tickingShack),
                    null,
                    shortcutSpoiler("ghostPencil5"),
                    ChoiceOption("Leave this noncombat"),
                )
            }
            738 -> listOf(
                ChoiceOption("-stench"),
                ChoiceOption("Sewer-Drenched (+100 stench damage)"),
                null,
                null,
                null,
                ChoiceOption("Return to Skid Row"),
            )
            739 -> listOf(
                ChoiceOption("Freddies"),
                ChoiceOption(
                    "lock impression (${itemCount(WAX_LOCK_IMPRESSION)}) + music box parts " +
                        "(${itemCount(INTRICATE_MUSIC_BOX_PARTS)}) -> replica key",
                ),
                ChoiceOption(
                    "moon-amber (${itemCount(MOON_AMBER)}) -> polished moon-amber",
                ),
                ChoiceOption(
                    "3 music box parts (${itemCount(INTRICATE_MUSIC_BOX_PARTS)}) + clockwork key " +
                        "(${itemCount(DREADSYLVANIAN_CLOCKWORK_KEY)}) -> mechanical songbird",
                ),
                ChoiceOption("3 lengths of old fuse"),
                ChoiceOption("Return to Skid Row"),
            )
            740 -> listOf(
                ChoiceOption("fewer skeletons"),
                ChoiceOption("-sleaze"),
                ChoiceOption("+Muscle"),
                null,
                null,
                ChoiceOption("Return to Skid Row"),
            )
            741 -> {
                val servants = buildString {
                    append("-hot")
                    if (isMyst()) {
                        append(", ${shepherdPieSpoiler()}")
                    }
                    append(", +Moxie")
                }
                val masterSuite = lockSpoiler() +
                    "fewer werewolves, eau de mort, 10 ghost thread " +
                    "(${itemCount(GHOST_THREAD)}) -> ghost shawl"
                listOf(
                    ChoiceOption(
                        "fewer zombies, Freddies, Fifty Ways to Bereave Your Lover (+100 sleaze damage)",
                    ),
                    ChoiceOption(servants),
                    ChoiceOption(masterSuite),
                    null,
                    shortcutSpoiler("ghostPencil6"),
                    ChoiceOption("Leave this noncombat"),
                )
            }
            742 -> listOf(
                ChoiceOption("fewer zombies"),
                ChoiceOption("Freddies"),
                ChoiceOption("Fifty Ways to Bereave Your Lover (+100 sleaze damage)"),
                null,
                null,
                ChoiceOption("Return to The Old Duke's Estate"),
            )
            743 -> listOf(
                ChoiceOption("-hot"),
                ChoiceOption(shepherdPieSpoiler()),
                ChoiceOption("+Moxie"),
                null,
                null,
                ChoiceOption("Return to The Old Duke's Estate"),
            )
            744 -> listOf(
                ChoiceOption("fewer werewolves"),
                ChoiceOption("eau de mort"),
                ChoiceOption("10 ghost thread (${itemCount(GHOST_THREAD)}) -> ghost shawl"),
                null,
                null,
                ChoiceOption("Return to The Old Duke's Estate"),
            )
            745 -> {
                val ballroom = lockSpoiler() + "fewer vampires, ${muddySkirtSpoiler()}+Moxie"
                val diningRoom = buildString {
                    append("dreadful roast, -stench")
                    if (isMyst()) append(", wax banana")
                }
                listOf(
                    ChoiceOption(ballroom),
                    ChoiceOption("-cold, Staying Frosty (+100 cold damage)"),
                    ChoiceOption(diningRoom),
                    null,
                    shortcutSpoiler("ghostPencil7"),
                    ChoiceOption("Leave this noncombat"),
                )
            }
            746 -> listOf(
                ChoiceOption("fewer vampires"),
                ChoiceOption("${muddySkirtSpoiler()}+Moxie"),
                null,
                null,
                null,
                ChoiceOption("Return to The Great Hall"),
            )
            747 -> listOf(
                ChoiceOption("-cold"),
                ChoiceOption("Staying Frosty (+100 cold damage)"),
                null,
                null,
                null,
                ChoiceOption("Return to The Great Hall"),
            )
            748 -> listOf(
                ChoiceOption("dreadful roast"),
                ChoiceOption("-stench"),
                ChoiceOption("wax banana"),
                null,
                null,
                ChoiceOption("Return to The Great Hall"),
            )
            749 -> {
                val laboratory = buildString {
                    append(lockSpoiler())
                    append("fewer bugbears, fewer zombies, visit The Machine")
                    if (isMoxie()) {
                        append(", blood kiwi (${itemCount(BLOOD_KIWI)}) + eau de mort ")
                        append("(${itemCount(EAU_DE_MORT)}) -> bloody kiwitini")
                    }
                }
                val books = if (isMyst()) {
                    "fewer skeletons, +Mysticality, learn recipe for moon-amber necklace"
                } else {
                    "unavailable (Mysticality class only)"
                }
                listOf(
                    ChoiceOption(laboratory),
                    ChoiceOption(books),
                    ChoiceOption("-sleaze, Freddies, Magically Fingered (+150 max MP, 40-50 MP regen)"),
                    null,
                    shortcutSpoiler("ghostPencil8"),
                    ChoiceOption("Leave this noncombat"),
                )
            }
            750 -> listOf(
                ChoiceOption("fewer bugbears"),
                ChoiceOption("fewer zombies"),
                ChoiceOption("visit The Machine"),
                ChoiceOption(
                    "blood kiwi (${itemCount(BLOOD_KIWI)}) + eau de mort " +
                        "(${itemCount(EAU_DE_MORT)}) -> bloody kiwitini",
                ),
                null,
                ChoiceOption("Return to The Tower"),
            )
            751 -> listOf(
                ChoiceOption("fewer skeletons"),
                ChoiceOption("+Mysticality"),
                ChoiceOption("learn recipe for moon-amber necklace"),
                null,
                null,
                ChoiceOption("Return to The Tower"),
            )
            752 -> listOf(
                ChoiceOption("-sleaze"),
                ChoiceOption("Freddies"),
                ChoiceOption("Magically Fingered (+150 max MP, 40-50 MP regen)"),
                null,
                null,
                ChoiceOption("Return to The Tower"),
            )
            753 -> listOf(
                ChoiceOption("-spooky, +Muscle, +MP"),
                ChoiceOption("-hot, Freddies, +Muscle/Mysticality/Moxie"),
                ChoiceOption("stinking agaricus, Spore-wreathed (reduce enemy defense by 20%)"),
                null,
                shortcutSpoiler("ghostPencil9"),
                ChoiceOption("Leave this noncombat"),
            )
            754 -> listOf(
                ChoiceOption("-spooky"),
                ChoiceOption("+Muscle"),
                ChoiceOption("+MP"),
                null,
                null,
                ChoiceOption("Return to The Dungeons"),
            )
            755 -> listOf(
                ChoiceOption("-hot"),
                ChoiceOption("Freddies"),
                ChoiceOption("+Muscle/Mysticality/Moxie"),
                null,
                null,
                ChoiceOption("Return to The Dungeons"),
            )
            756 -> listOf(
                ChoiceOption("stinking agaricus"),
                ChoiceOption("Spore-wreathed (reduce enemy defense by 20%)"),
                null,
                null,
                null,
                ChoiceOption("Return to The Dungeons"),
            )
            758 -> {
                val necklace = equippedOrAvailable(
                    MOON_AMBER_NECKLACE,
                    "moon-amber necklace equipped",
                    "moon-amber necklace NOT equipped but in inventory",
                    "moon-amber necklace neither equipped nor available",
                )
                val hasKiwiEffect = hasEffect(EFFECT_FIRST_BLOOD_KIWI)
                val isBlind = hasEffect(EFFECT_TEMPORARY_BLINDNESS) ||
                    hasEquipped(MAKESHIFT_TURBAN) ||
                    hasEquipped(HELPS_YOU_SLEEP) ||
                    hasEquipped(SLEEP_MASK)
                val kiwi = when {
                    hasKiwiEffect -> if (isBlind) "First Blood Kiwi and blind" else "First Blood Kiwi but NOT blind"
                    itemCount(BLOODY_KIWITINI) > 0 -> "bloody kiwitini in inventory"
                    else -> "First Blood Kiwi neither active nor available"
                }
                listOf(
                    ChoiceOption("$necklace / $kiwi"),
                    ChoiceOption("Run away"),
                )
            }
            759 -> {
                val badge = equippedOrAvailable(
                    AUDITORS_BADGE,
                    "Dreadsylvanian auditor's badge equipped",
                    "Dreadsylvanian auditor's badge NOT equipped but in inventory",
                    "Dreadsylvanian auditor's badge neither equipped nor available",
                )
                val skirt = equippedOrAvailable(
                    WEEDY_SKIRT,
                    "weedy skirt equipped",
                    "weedy skirt NOT equipped but in inventory",
                    "weedy skirt neither equipped nor available",
                )
                listOf(
                    ChoiceOption("$badge / $skirt"),
                    ChoiceOption("Run away"),
                )
            }
            760 -> {
                val shawl = equippedOrAvailable(
                    GHOST_SHAWL,
                    "ghost shawl equipped",
                    "ghost shawl NOT equipped but in inventory",
                    "ghost shawl neither equipped nor available",
                )
                val pie = when {
                    hasEffect(EFFECT_SHEPHERDS_BREATH) -> "Shepherd's Breath active"
                    itemCount(SHEPHERDS_PIE) > 0 -> "Dreadsylvanian shepherd's pie in inventory"
                    else -> "Shepherd's Breath neither active nor available"
                }
                listOf(
                    ChoiceOption("$shawl / $pie"),
                    ChoiceOption("Run away"),
                )
            }
            else -> return null
        }
        val name = dreadsylvaniaChoiceName(choice)
        return ChoiceAdventures.Spoilers(choice, name, options)
    }

    private fun dreadsylvaniaChoiceName(choice: Int): String = when (choice) {
        725 -> "Tallest Tree in the Forest"
        726 -> "Top of the Tree, Ma!"
        727 -> "All Along the Watchtower"
        728 -> "Treebasing"
        729 -> "Below the Roots"
        730 -> "Hot Coals"
        731 -> "The Heart of the Matter"
        732 -> "Once Midden, Twice Shy"
        733 -> "Dreadsylvanian Village Square"
        734 -> "Fright School"
        735 -> "Smith, Black as Night"
        736 -> "Gallows"
        737 -> "The Even More Dreadful Part of Town"
        738 -> "A Dreadful Smell"
        739 -> "The Tinker's. Damn."
        740 -> "Eight, Nine, Tenement"
        741 -> "The Old Duke's Estate"
        742 -> "The Plot Thickens"
        743 -> "No Quarter"
        744 -> "The Master Suite -- Sweet!"
        745 -> "This Hall is Really Great"
        746 -> "The Belle of the Ballroom"
        747 -> "Cold Storage"
        748 -> "Dining In (the Castle)"
        749 -> "Tower Most Tall"
        750 -> "Working in the Lab, Late One Night"
        751 -> "Among the Quaint and Curious Tomes."
        752 -> "In The Boudoir"
        753 -> "The Dreadsylvanian Dungeon"
        754 -> "Live from Dungeon Prison"
        755 -> "The Hot Bowels"
        756 -> "Among the Fungus"
        758 -> "End of the Path"
        759 -> "You're About to Fight City Hall"
        760 -> "Holding Court"
        else -> "Dreadsylvania"
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Track A Batch 1 – new dynamic spoilers (Behavioral Deepen XXX)
    // ══════════════════════════════════════════════════════════════════════════

    // ── 5 Heart of Very, Very Dark Darkness ───────────────────────────────────
    private fun heartOfDarknessSpoilers(): ChoiceAdventures.Spoilers {
        val rock = itemCount(INEXPLICABLY_GLOWING_ROCK) >= 1
        val options = listOf(
            ChoiceOption("You ${if (rock) "" else "DON'T "} have an inexplicably glowing rock"),
            ChoiceOption("skip adventure"),
        )
        return ChoiceAdventures.Spoilers(5, "Heart of Very, Very Dark Darkness", options)
    }

    // ── 7 How Depressing ──────────────────────────────────────────────────────
    private fun howDepressingSpoilers(): ChoiceAdventures.Spoilers {
        val glove = hasEquipped(SPOOKY_GLOVE)
        val options = listOf(
            ChoiceOption("spooky glove ${if (glove) "" else "NOT "}equipped"),
            ChoiceOption("skip adventure"),
        )
        return ChoiceAdventures.Spoilers(7, "How Depressing", options)
    }

    // ── 184 That Explains All The Eyepatches ──────────────────────────────────
    private fun eyepatchesSpoilers(): ChoiceAdventures.Spoilers {
        val options = listOf(
            ChoiceOption(
                if (isMyst()) "3 drunk and stats (varies by class)"
                else "enter combat (varies by class)",
            ),
            ChoiceOption(
                if (isMoxie()) "3 drunk and stats (varies by class)"
                else "shot of rotgut (varies by class)",
            ),
            ChoiceOption(
                if (isMuscle()) "3 drunk and stats (varies by class)"
                else "shot of rotgut (varies by class)",
            ),
            ChoiceOption("always 3 drunk & stats"),
            ChoiceOption("always shot of rotgut"),
            ChoiceOption("combat (or rotgut if Myst class)"),
        )
        return ChoiceAdventures.Spoilers(184, "That Explains All The Eyepatches", options)
    }

    // ── 185 Yes, You're a Rock Starrr ─────────────────────────────────────────
    private fun rockStarrrSpoilers(): ChoiceAdventures.Spoilers {
        val drunk = inebriety()
        val options = listOf(
            ChoiceOption("base booze"),
            ChoiceOption("mixed booze"),
            ChoiceOption(if (drunk == 0) "combat" else "stats"),
        )
        return ChoiceAdventures.Spoilers(185, "Yes, You're a Rock Starrr", options)
    }

    // ── 187 Arrr You Man Enough? (Beer Pong) ──────────────────────────────────
    private fun beerPongSpoilers(): ChoiceAdventures.Spoilers {
        val prefs = preferences
        val known = if (prefs != null) PirateInsults.countKnown(prefs) else 0
        val odds = PirateInsults.pirateInsultOdds(known) * 100.0
        val oddsStr = PirateInsults.formatOddsPercent(known)
        val options = listOf(
            ChoiceOption("$oddsStr% chance of winning"),
            ChoiceOption(if (odds >= 100.0) "Oh come on. Do it!" else "Try later"),
        )
        return ChoiceAdventures.Spoilers(187, "Arrr You Man Enough?", options)
    }

    // ── 188 The Infiltrationist ───────────────────────────────────────────────
    private fun infiltrationistSpoilers(): ChoiceAdventures.Spoilers {
        val ok1 = isWearingOutfit(FRAT_OUTFIT)
        val ok2a = hasEquipped(MULLET_WIG)
        val ok2b = itemCount(BRIEFCASE) >= 1
        val ok3a = hasEquipped(FRILLY_SKIRT)
        val wings = itemCount(HOT_WING)
        val options = listOf(
            ChoiceOption("Frat Boy Ensemble (${if (ok1) "" else "NOT "}equipped)"),
            ChoiceOption(
                "mullet wig (${if (ok2a) "" else "NOT "}equipped) + " +
                    "briefcase (${if (ok2b) "OK)" else "0 in inventory)"}",
            ),
            ChoiceOption(
                "frilly skirt (${if (ok3a) "" else "NOT "}equipped) + " +
                    "3 hot wings ($wings in inventory)",
            ),
        )
        return ChoiceAdventures.Spoilers(188, "The Infiltrationist", options)
    }

    // ── 272 Marketplace Entrance ──────────────────────────────────────────────
    private fun marketplaceEntranceSpoilers(): ChoiceAdventures.Spoilers {
        val nickels = itemCount(HOBO_NICKEL)
        val binder = hasEquipped(HOBO_CODE_BINDER)
        val options = listOf(
            ChoiceOption("$nickels nickels, ${if (binder) "" else "NO "} hobo code binder equipped"),
            ChoiceOption("skip adventure"),
        )
        return ChoiceAdventures.Spoilers(272, "Marketplace Entrance", options)
    }

    // ── 298 In the Shade ──────────────────────────────────────────────────────
    private fun inTheShadeSpoilers(): ChoiceAdventures.Spoilers {
        val seeds = itemCount(SEED_PACKET)
        val slime = itemCount(GREEN_SLIME)
        val options = listOf(
            ChoiceOption("$seeds seed packets, $slime globs of green slime"),
            ChoiceOption("skip adventure"),
        )
        return ChoiceAdventures.Spoilers(298, "In the Shade", options)
    }

    // ── 304 A Vent Horizon ────────────────────────────────────────────────────
    private fun ventHorizonSpoilers(): ChoiceAdventures.Spoilers {
        val summons = 3 - (preferences?.getInt("tempuraSummons", 0) ?: 0)
        val options = listOf(
            ChoiceOption("$summons summons left today"),
            ChoiceOption("skip adventure"),
        )
        return ChoiceAdventures.Spoilers(304, "A Vent Horizon", options)
    }

    // ── 305 There is Sauce at the Bottom of the Ocean ─────────────────────────
    private fun sauceBottomSpoilers(): ChoiceAdventures.Spoilers {
        val globes = itemCount(MERKIN_PRESSUREGLOBE)
        val options = listOf(
            ChoiceOption("$globes Mer-kin pressureglobes"),
            ChoiceOption("skip adventure"),
        )
        return ChoiceAdventures.Spoilers(305, "There is Sauce at the Bottom of the Ocean", options)
    }

    // ── 309 Barback ───────────────────────────────────────────────────────────
    private fun barbackSpoilers(): ChoiceAdventures.Spoilers {
        val seaodes = 3 - (preferences?.getInt("seaodesFound", 0) ?: 0)
        val options = listOf(
            ChoiceOption("$seaodes more seodes available today"),
            ChoiceOption("skip adventure"),
        )
        return ChoiceAdventures.Spoilers(309, "Barback", options)
    }

    // ── 502 Arboreal Respite ──────────────────────────────────────────────────
    private fun arborealRespiteSpoilers(): ChoiceAdventures.Spoilers {
        val stakes = itemCount(WOODEN_STAKES)
        val hearts = itemCount(VAMPIRE_HEART)
        val hunterAction = if (stakes > 0) "and get wooden stakes"
        else "and trade $hearts hearts"
        val barskins = itemCount(BAR_SKIN)
        val saplings = itemCount(SPOOKY_SAPLING)
        val haveMap = itemCount(SPOOKY_MAP) > 0
        val haveCoin = itemCount(TREE_HOLED_COIN) > 0
        val asc = ascensions()
        val templeUnlocked = (preferences?.getInt("lastTempleUnlock", 0) ?: 0) == asc
        val getCoin = !haveCoin && !haveMap && !templeUnlocked
        val coinAction = if (getCoin) "gain quest coin" else "skip adventure"
        val fertilizer = itemCount(SPOOKY_FERTILIZER)
        val mapAction = if (haveCoin) ", gain spooky temple map" else ""

        val options = listOf(
            ChoiceOption(
                "gain some meat, meet the vampire hunter $hunterAction, " +
                    "sell bar skins ($barskins) or buy a spooky sapling ($saplings)",
            ),
            ChoiceOption(
                "gain mosquito larva or spooky mushrooms, $coinAction, " +
                    "get stats or fight a vampire",
            ),
            ChoiceOption(
                "gain a starter item, gain Spooky-Gro fertilizer ($fertilizer)$mapAction, gain fake blood",
            ),
            null,
            ChoiceOption("gain 3 fruits"),
        )
        return ChoiceAdventures.Spoilers(502, "Arboreal Respite", options)
    }

    // ── 522 Welcome to the Footlocker ─────────────────────────────────────────
    private fun footlockerSpoilers(): ChoiceAdventures.Spoilers {
        val havePolearm = itemCount(KNOB_GOBLIN_POLEARM) > 0 || hasEquipped(KNOB_GOBLIN_POLEARM)
        val havePants = itemCount(KNOB_GOBLIN_PANTS) > 0 || hasEquipped(KNOB_GOBLIN_PANTS)
        val haveHelm = itemCount(KNOB_GOBLIN_HELM) > 0 || hasEquipped(KNOB_GOBLIN_HELM)
        val item = when {
            !havePolearm -> "knob goblin elite polearm"
            !havePants -> "knob goblin elite pants"
            !haveHelm -> "knob goblin elite helm"
            else -> "knob jelly donut"
        }
        val options = listOf(
            ChoiceOption(item),
            ChoiceOption("skip adventure"),
        )
        return ChoiceAdventures.Spoilers(522, "Welcome to the Footlocker", options)
    }

    // ── 580 The Hidden Heart of the Hidden Temple ─────────────────────────────
    private fun hiddenHeartSpoilers(): ChoiceAdventures.Spoilers {
        val haveNostril = itemCount(NOSTRIL_OF_THE_SERPENT) > 0
        val asc = ascensions()
        val buttonsUnconfused = (preferences?.getInt("lastTempleButtonsUnlock", 0) ?: 0) == asc
        val responseText = lastResponseText()
        val chooseOrRandom = if (buttonsUnconfused || haveNostril) {
            "choose Hidden Heart adventure"
        } else {
            "randomise Hidden Heart adventure"
        }
        val opt2 = "moxie substats and 5 turns of Somewhat poisoned"

        val options: List<ChoiceOption?> = when {
            responseText.contains("door_stone.gif") -> listOf(
                ChoiceOption("muscle substats"),
                ChoiceOption(chooseOrRandom),
                ChoiceOption(opt2),
            )
            responseText.contains("door_sun.gif") -> listOf(
                ChoiceOption("gain ancient calendar fragment"),
                ChoiceOption(chooseOrRandom),
                ChoiceOption(opt2),
            )
            responseText.contains("door_gargoyle.gif") -> listOf(
                ChoiceOption("gain mana"),
                ChoiceOption(chooseOrRandom),
                ChoiceOption(opt2),
            )
            responseText.contains("door_pikachu.gif") -> listOf(
                ChoiceOption("unlock Hidden City"),
                ChoiceOption(chooseOrRandom),
                ChoiceOption(opt2),
            )
            else -> listOf(null, null, null)
        }
        return ChoiceAdventures.Spoilers(580, "The Hidden Heart of the Hidden Temple", options)
    }

    // ── 581 Such Great Depths ─────────────────────────────────────────────────
    private fun suchGreatDepthsSpoilers(): ChoiceAdventures.Spoilers {
        val fungus = itemCount(GLOWING_FUNGUS)
        val prefs = preferences
        val options = listOf(
            ChoiceOption("gain a glowing fungus ($fungus)"),
            if (prefs?.getBoolean("_templeHiddenPower", false) == true) {
                ChoiceOption("skip adventure")
            } else {
                ChoiceOption("5 advs of +15 mus/mys/mox")
            },
            ChoiceOption("fight clan of cave bars"),
        )
        return ChoiceAdventures.Spoilers(581, "Such Great Depths", options)
    }

    // ── 582 Fitting In ────────────────────────────────────────────────────────
    private fun fittingInSpoilers(): ChoiceAdventures.Spoilers {
        val prefs = preferences
        val haveNostril = itemCount(NOSTRIL_OF_THE_SERPENT) > 0
        val asc = ascensions()
        val gainNostril = !haveNostril &&
            (prefs?.getInt("lastTempleButtonsUnlock", 0) ?: 0) != asc
        val nostrilAction = if (gainNostril) "gain the Nostril of the Serpent" else "skip adventure"
        val templeAdvs = (prefs?.getInt("lastTempleAdventures", 0) ?: 0) == asc
        val advAction = if (templeAdvs) "skip adventure" else "gain 3 adventures"
        val powerAction = if (prefs?.getBoolean("_templeHiddenPower", false) == true) {
            "skip adventure"
        } else {
            "Hidden Power"
        }
        val options = listOf(
            ChoiceOption("mysticality substats, $nostrilAction or $advAction"),
            ChoiceOption("Hidden Heart of the Hidden Temple"),
            ChoiceOption("gain a glowing fungus, $powerAction or fight a clan of cave bars"),
        )
        return ChoiceAdventures.Spoilers(582, "Fitting In", options)
    }

    // ── 669 The Fast and the Furry-ous ────────────────────────────────────────
    private fun fastAndFurryousSpoilers(): ChoiceAdventures.Spoilers {
        val hasUmbrella = hasEquipped(TITANIUM_UMBRELLA) || hasEquipped(UNBREAKABLE_UMBRELLA)
        val options = listOf(
            ChoiceOption(
                if (hasUmbrella) {
                    if (hasEquipped(TITANIUM_UMBRELLA)) "open Ground Floor (titanium umbrella equipped)"
                    else "open Ground Floor (unbreakable umbrella equipped)"
                } else {
                    "Neckbeard Choice (titanium/unbreakable umbrella not equipped)"
                },
            ),
            ChoiceOption("200 Moxie substats"),
            ChoiceOption(""),
            ChoiceOption("skip adventure and guarantees this adventure will reoccur"),
        )
        return ChoiceAdventures.Spoilers(669, "The Fast and the Furry-ous", options)
    }

    // ── 670 You Don't Mess Around with Gym ────────────────────────────────────
    private fun gymSpoilers(): ChoiceAdventures.Spoilers {
        val hasAmulet = hasEquipped(EXTREME_AMULET)
        val options = listOf(
            ChoiceOption("massive dumbbell, then skip adventure"),
            ChoiceOption("200 Muscle substats"),
            ChoiceOption("pec oil, giant jar of protein powder, Squat-Thrust Magazine"),
            ChoiceOption(
                if (hasAmulet) "open Ground Floor (amulet equipped)"
                else "skip adventure (amulet not equipped)",
            ),
            ChoiceOption("skip adventure and guarantees this adventure will reoccur"),
        )
        return ChoiceAdventures.Spoilers(670, "You Don't Mess Around with Gym", options)
    }

    // ── 678 Yeah, You're for Me, Punk Rock Giant ──────────────────────────────
    private fun punkRockGiantSpoilers(): ChoiceAdventures.Spoilers {
        val hasMohawk = hasEquipped(MOHAWK_WIG)
        val options = listOf(
            ChoiceOption(
                if (hasMohawk) "Finish quest (mohawk wig equipped)"
                else "Fight Punk Rock Giant (mohawk wig not equipped)",
            ),
            ChoiceOption("500 meat"),
            ChoiceOption("Steampunk Choice"),
            ChoiceOption("Raver Choice"),
        )
        return ChoiceAdventures.Spoilers(678, "Yeah, You're for Me, Punk Rock Giant", options)
    }

    // ── 696 Stick a Fork In It ────────────────────────────────────────────────
    private fun stickAForkInItSpoilers(): ChoiceAdventures.Spoilers {
        val prefs = preferences
        val options = listOf(
            ChoiceOption(
                if (prefs?.getBoolean("maraisDarkUnlock", false) == true) "Dark and Spooky Swamp already unlocked"
                else "unlock Dark and Spooky Swamp",
            ),
            ChoiceOption(
                if (prefs?.getBoolean("maraisWildlifeUnlock", false) == true) "The Wildlife Sanctuarrrrrgh already unlocked"
                else "unlock The Wildlife Sanctuarrrrrgh",
            ),
        )
        return ChoiceAdventures.Spoilers(696, "Stick a Fork In It", options)
    }

    // ── 697 Sophie's Choice ───────────────────────────────────────────────────
    private fun sophiesChoiceSpoilers(): ChoiceAdventures.Spoilers {
        val prefs = preferences
        val options = listOf(
            ChoiceOption(
                if (prefs?.getBoolean("maraisCorpseUnlock", false) == true) "The Corpse Bog already unlocked"
                else "unlock The Corpse Bog",
            ),
            ChoiceOption(
                if (prefs?.getBoolean("maraisWizardUnlock", false) == true) "The Ruined Wizard Tower already unlocked"
                else "unlock The Ruined Wizard Tower",
            ),
        )
        return ChoiceAdventures.Spoilers(697, "Sophie's Choice", options)
    }

    // ── 698 From Bad to Worst ─────────────────────────────────────────────────
    private fun fromBadToWorstSpoilers(): ChoiceAdventures.Spoilers {
        val prefs = preferences
        val options = listOf(
            ChoiceOption(
                if (prefs?.getBoolean("maraisBeaverUnlock", false) == true) "Swamp Beaver Territory already unlocked"
                else "unlock Swamp Beaver Territory",
            ),
            ChoiceOption(
                if (prefs?.getBoolean("maraisVillageUnlock", false) == true) "The Weird Swamp Village already unlocked"
                else "unlock The Weird Swamp Village",
            ),
        )
        return ChoiceAdventures.Spoilers(698, "From Bad to Worst", options)
    }

    // ── 700 Delirium in the Cafeteria ─────────────────────────────────────────
    private fun deliriumCafeteriaSpoilers(): ChoiceAdventures.Spoilers {
        val options = listOf(
            ChoiceOption(if (hasEffect(EFFECT_JOCK_JAMS)) "Gain stats" else "Lose HP"),
            ChoiceOption(if (hasEffect(EFFECT_NERD_WORD)) "Gain stats" else "Lose HP"),
            ChoiceOption(if (hasEffect(EFFECT_GREASER_LIGHTNIN)) "Gain stats" else "Lose HP"),
        )
        return ChoiceAdventures.Spoilers(700, "Delirium in the Cafeteria", options)
    }

    // ── 704 Playing the Catalog Card ────────────────────────────────────────
    private fun merkinCatalogSpoilers(): ChoiceAdventures.Spoilers {
        val responseText = lastResponseText()
        val pref = preferences?.getString("merkinCatalogChoices", "") ?: ""
        val knownChoices = mutableMapOf<Int, String>()
        for (card in pref.split(",")) {
            val segments = card.split(":")
            if (segments.size < 3) continue
            val choiceNum = segments[1].toIntOrNull() ?: continue
            knownChoices[choiceNum] = segments[2]
        }
        val choices = ChoiceUtilities.parseChoices(responseText)
        val options = (1..choices.size).map { i ->
            val spoiler = knownChoices[i]
            ChoiceOption(spoiler ?: "unknown")
        }
        return ChoiceAdventures.Spoilers(704, "Playing the Catalog Card", options)
    }

    // ── 780 Action Elevator (Hidden Apartment) ────────────────────────────────
    private fun actionElevatorSpoilers(): ChoiceAdventures.Spoilers {
        val prefs = preferences
        val progress = prefs?.getInt("hiddenApartmentProgress", 0) ?: 0
        val hasOnceCursed = hasEffect(EFFECT_ONCE_CURSED)
        val hasTwiceCursed = hasEffect(EFFECT_TWICE_CURSED)
        val hasThriceCursed = hasEffect(EFFECT_THRICE_CURSED)
        val lawyersRelocated =
            (prefs?.getInt("relocatePygmyLawyer", 0) ?: 0) == ascensions()
        val opt0 = when {
            progress >= 7 -> "penthouse empty"
            hasThriceCursed -> "Fight ancient protector spirit"
            else -> "Need Thrice-Cursed to fight ancient protector spirit"
        }
        val cursedOption = when {
            hasThriceCursed -> "Increase Thrice-Cursed"
            hasTwiceCursed -> "Get Thrice-Cursed"
            hasOnceCursed -> "Get Twice-Cursed"
            else -> "Get Once-Cursed"
        }
        val opt2 = if (lawyersRelocated) "Waste adventure"
        else "Relocate pygmy witch lawyers to Hidden Park"
        val options = listOf(
            ChoiceOption(opt0),
            ChoiceOption(cursedOption),
            ChoiceOption(opt2),
            ChoiceOption("$cursedOption, then pick again"),
            null,
            ChoiceOption("skip adventure"),
        )
        return ChoiceAdventures.Spoilers(780, "Action Elevator", options)
    }

    // ── 781 Earthbound and Down ───────────────────────────────────────────────
    private fun earthboundSpoilers(): ChoiceAdventures.Spoilers {
        val options = listOf(
            ChoiceOption("Unlock Hidden Apartment Building"),
            ChoiceOption("Get stone triangle"),
            ChoiceOption("Get Blessing of Bulbazinalli"),
            null,
            null,
            ChoiceOption("skip adventure"),
        )
        return ChoiceAdventures.Spoilers(781, "Earthbound and Down", options)
    }

    // ── 783 Water You Dune ────────────────────────────────────────────────────
    private fun waterYouDuneSpoilers(): ChoiceAdventures.Spoilers {
        val options = listOf(
            ChoiceOption("Unlock Hidden Hospital"),
            ChoiceOption("Get stone triangle"),
            ChoiceOption("Get Blessing of Squirtlcthulli"),
            null,
            null,
            ChoiceOption("skip adventure"),
        )
        return ChoiceAdventures.Spoilers(783, "Water You Dune", options)
    }

    // ── 784 You, M. D. ───────────────────────────────────────────────────────
    private fun youMdSpoilers(): ChoiceAdventures.Spoilers {
        val options = listOf(
            ChoiceOption("Fight ancient protector spirit"),
            null,
            null,
            null,
            null,
            ChoiceOption("skip adventure"),
        )
        return ChoiceAdventures.Spoilers(784, "You, M. D.", options)
    }

    // ── 785 Air Apparent ──────────────────────────────────────────────────────
    private fun airApparentSpoilers(): ChoiceAdventures.Spoilers {
        val options = listOf(
            ChoiceOption("Unlock Hidden Office Building"),
            ChoiceOption("Get stone triangle"),
            ChoiceOption("Get Blessing of Pikachutlotal"),
            ChoiceOption("Gain 100x level Meat, then pick again"),
            null,
            ChoiceOption("skip adventure"),
        )
        return ChoiceAdventures.Spoilers(785, "Air Apparent", options)
    }

    // ── 786 Working Holiday (Hidden Office) ───────────────────────────────────
    private fun workingHolidaySpoilers(): ChoiceAdventures.Spoilers {
        val prefs = preferences
        val progress = prefs?.getInt("hiddenOfficeProgress", 0) ?: 0
        val hasBossUnlock = progress >= 6
        val hasMcCluskyFile = itemCount(MCCLUSKY_FILE) > 0
        val hasBinderClip = itemCount(BINDER_CLIP) > 0
        val opt0 = when {
            progress >= 7 -> "office empty"
            hasMcCluskyFile || hasBossUnlock -> "Fight ancient protector spirit"
            else -> "Need McClusky File (complete) to fight ancient protector spirit"
        }
        val opt1 = if (hasBinderClip || hasMcCluskyFile || hasBossUnlock) {
            "Get random item"
        } else {
            "Get boring binder clip"
        }
        val options = listOf(
            ChoiceOption(opt0),
            ChoiceOption(opt1),
            ChoiceOption("Fight pygmy witch accountant"),
            null,
            null,
            ChoiceOption("skip adventure"),
        )
        return ChoiceAdventures.Spoilers(786, "Working Holiday", options)
    }

    // ── 787 Fire when Ready ───────────────────────────────────────────────────
    private fun fireWhenReadySpoilers(): ChoiceAdventures.Spoilers {
        val options = listOf(
            ChoiceOption("Unlock Hidden Bowling Alley"),
            ChoiceOption("Get stone triangle"),
            ChoiceOption("Get Blessing of Charcoatl"),
            null,
            null,
            ChoiceOption("skip adventure"),
        )
        return ChoiceAdventures.Spoilers(787, "Fire when Ready", options)
    }

    // ── 788 Life is Like a Cherry of Bowls ────────────────────────────────────
    private fun cherryOfBowlsSpoilers(): ChoiceAdventures.Spoilers {
        val prefs = preferences
        val progress = prefs?.getInt("hiddenBowlingAlleyProgress", 0) ?: 0
        val opt0 = when {
            progress > 6 -> "Get stats"
            progress == 6 -> "fight ancient protector spirit"
            else -> {
                val left = 6 - progress
                "Get stats, on 5th visit, fight ancient protector spirit ($left visit${if (left != 1) "s" else ""} left"
            }
        }
        val options = listOf(
            ChoiceOption(opt0),
            ChoiceOption("Increment boss counter, then pick again"),
            null,
            null,
            null,
            ChoiceOption("skip adventure"),
        )
        return ChoiceAdventures.Spoilers(788, "Life is Like a Cherry of Bowls", options)
    }

    // ── 789 Where Does The Lone Ranger Take His Garbagester? ──────────────────
    private fun garbagesterSpoilers(): ChoiceAdventures.Spoilers {
        val janitorsRelocated =
            (preferences?.getInt("relocatePygmyJanitor", 0) ?: 0) == ascensions()
        val options = listOf(
            ChoiceOption("Get random items"),
            ChoiceOption(
                if (janitorsRelocated) "Waste adventure"
                else "Relocate pygmy janitors to Hidden Park",
            ),
            null,
            null,
            null,
            ChoiceOption("skip adventure"),
        )
        return ChoiceAdventures.Spoilers(789, "Where Does The Lone Ranger Take His Garbagester?", options)
    }

    // ── 791 Legend of the Temple in the Hidden City ────────────────────────────
    private fun legendOfTempleSpoilers(): ChoiceAdventures.Spoilers {
        val triangles = itemCount(STONE_TRIANGLE)
        val opt0 = if (triangles == 4) "fight Protector Spectre"
        else "Need 4 stone triangles to fight Protector Spectre ($triangles)"
        val options = listOf(
            ChoiceOption(opt0),
            null,
            null,
            null,
            null,
            ChoiceOption("skip adventure"),
        )
        return ChoiceAdventures.Spoilers(791, "Legend of the Temple in the Hidden City", options)
    }

    // ── 801 A Reanimated Conversation ─────────────────────────────────────────
    private fun reanimatedConversationSpoilers(): ChoiceAdventures.Spoilers {
        val options = listOf(
            ChoiceOption("skulls increase meat drops"),
            ChoiceOption("arms deal extra damage"),
            ChoiceOption("legs increase item drops"),
            ChoiceOption("wings sometimes delevel at start of combat"),
            ChoiceOption("weird parts sometimes block enemy attacks"),
            ChoiceOption("get rid of all collected parts"),
            ChoiceOption("no changes"),
        )
        return ChoiceAdventures.Spoilers(801, "A Reanimated Conversation", options)
    }

    // ── 1489 Slagging Off ─────────────────────────────────────────────────────
    private fun slaggingOffSpoilers(): ChoiceAdventures.Spoilers {
        val options = listOf(
            ChoiceOption("Get a crystal Crimbo goblet"),
            ChoiceOption("Get a crystal Crimbo platter"),
            ChoiceOption("Walk away in disappointment"),
        )
        return ChoiceAdventures.Spoilers(1489, "Slagging Off", options)
    }

    // ── 1545/1547/1549 CyberRealm Zone Half-Way ──────────────────────────────
    private fun cyberHalfWaySpoilers(zone: Int, defensePref: String): ChoiceAdventures.Spoilers {
        val element = cyberDefenseElement(defensePref)
        val message = cyberHalfWayMessage(zone, element)
        val choiceId = when (zone) {
            1 -> 1545
            2 -> 1547
            3 -> 1549
            else -> 1545
        }
        val options = listOf(
            ChoiceOption(message),
            ChoiceOption("no reward, no damage"),
        )
        return ChoiceAdventures.Spoilers(choiceId, "CyberRealm Zone $zone Half-Way", options)
    }

    private fun cyberDefenseElement(property: String): String {
        val defense = preferences?.getString(property, "") ?: ""
        defenseToElement[defense]?.let { return it }
        val encounter = lastEncounter()
        encounterToElement[encounter]?.let { return it }
        return "elemental"
    }

    private fun cyberHalfWayMessage(zone: Int, element: String): String {
        val resist = elementalResistance(element)
        val yield = cyberZeroYield(zone, resist)
        val damage = cyberElementalDamage(zone, resist)
        return "Get 0 ($yield) and suffer $damage $element damage"
    }

    private fun elementalResistance(element: String): Int {
        val modName = when (element) {
            "hot" -> "Hot Resistance"
            "cold" -> "Cold Resistance"
            "stench" -> "Stench Resistance"
            "spooky" -> "Spooky Resistance"
            "sleaze" -> "Sleaze Resistance"
            else -> return 0
        }
        return numericModifier(modName).toInt()
    }

    private fun cyberZeroYield(zone: Int, resist: Int): Int = when (zone) {
        1 -> min(1 * resist, 8)
        2 -> min(2 * resist, 16)
        3 -> min(3 * resist, 32)
        else -> 0
    }

    private fun cyberElementalDamage(zone: Int, resist: Int): Int {
        val baseDamage = 50 * zone
        if (resist == 0) return baseDamage
        val percent = elementalResistanceByLevel(resist)
        return ((1.0 - percent / 100.0) * baseDamage).toInt()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Track A – complex spoilers (Behavioral Deepen XXX)
    // ══════════════════════════════════════════════════════════════════════════

    // ── 606 Lost in the Great Overlook Lodge ────────────────────────────────────
    private fun overlookLodgeSpoilers(): ChoiceAdventures.Spoilers {
        val stenchRes = elementalResistanceLevels("stench")
        val exclusionBonus = overlookItemDropExclusionBonus()
        val effectiveDrop = kotlin.math.round(
            itemDropPercent() + foodDropPercent() - exclusionBonus,
        ).toInt()
        val init = kotlin.math.round(initiativeAdjustment()).toInt()
        val options = listOf(
            ChoiceOption("need +4 stench resist, have $stenchRes"),
            ChoiceOption("need +50% item drop, have $effectiveDrop%"),
            ChoiceOption("need jar of oil"),
            ChoiceOption("need +40% init, have $init%"),
            ChoiceOption(""),
            ChoiceOption("flee"),
        )
        return ChoiceAdventures.Spoilers(606, "Lost in the Great Overlook Lodge", options)
    }

    /**
     * Desktop Twin Peak item-drop exclusion — subtracts familiar ITEMDROP+FOODDROP,
     * Clancy lute, Eggman companion, Ed cat servant, Crown of Thrones ITEMDROP,
     * Buddy Bjorn ITEMDROP, and Florist Friar Twin Peak plant ITEMDROP.
     *
     * This matches ChoiceAdventures.java ~7883–7937 in desktop.
     */
    internal fun overlookItemDropExclusionBonus(): Double {
        // Desktop only counts *one* of familiar / Clancy / Eggman / Ed (mutually exclusive).
        val familiarDrop = activeFamiliarItemDrop() + activeFamiliarFoodDrop()
        val sidekickBonus = when {
            hasActiveFamiliar() || familiarDrop != 0.0 -> familiarDrop
            clancyLuteItemDrop() != 0.0 -> clancyLuteItemDrop()
            eggmanItemDrop() != 0.0 -> eggmanItemDrop()
            else -> edCatServantItemDrop()
        }
        // Throne + Bjorn + Florist are additive on top.
        return sidekickBonus +
            enthronedItemDrop() + bjornedItemDrop() + floristTwinPeakItemDrop()
    }

    // ── 611 The Horror... (A-Boo Peak) ────────────────────────────────────────
    private fun booPeakSpoilers(): ChoiceAdventures.Spoilers {
        val option = booPeakDamageOption()
        val options = listOf(option, ChoiceOption("Flee"))
        return ChoiceAdventures.Spoilers(611, "The Horror...", options)
    }

    private fun booPeakDamageOption(): ChoiceOption {
        val responseText = lastResponseText()
        val decisionText = findChoiceDecisionText(1, responseText)
        val level = ToppingPeakNcSync.findBooPeakLevel(decisionText)
        if (level < 1) return ChoiceOption("")

        var damageTaken = 0
        var diff = 0
        when (level) {
            1 -> { damageTaken = 30; diff = 17 }
            2 -> { damageTaken = 30; diff = 5 }
            3 -> damageTaken = 50
            4 -> damageTaken = 125
            5 -> damageTaken = 250
        }

        val spookyDamage = if (hasEffect(EFFECT_SPOOKYFORM)) {
            1.0
        } else {
            val resLevels = elementalResistanceLevels("spooky")
            val resPct = elementalResistanceByLevel(resLevels)
            max(damageTaken * (100.0 - resPct) / 100.0 - diff, 1.0)
        }.let { d ->
            if (hasEffect(EFFECT_COLDFORM) || hasEffect(EFFECT_SLEAZEFORM)) d * 2 else d
        }

        val coldDamage = if (hasEffect(EFFECT_COLDFORM)) {
            1.0
        } else {
            val resLevels = elementalResistanceLevels("cold")
            val resPct = elementalResistanceByLevel(resLevels)
            max(damageTaken * (100.0 - resPct) / 100.0 - diff, 1.0)
        }.let { d ->
            if (hasEffect(EFFECT_SLEAZEFORM) || hasEffect(EFFECT_STENCHFORM)) d * 2 else d
        }

        return ChoiceOption("${ceil(spookyDamage).toInt()} spooky damage, ${ceil(coldDamage).toInt()} cold damage")
    }

    // ── 636-639 Old Man psychosis ─────────────────────────────────────────────
    private val OLD_MAN_PSYCHOSIS_SPOILERS = arrayOf(
        arrayOf("Draw a Monster with a Crayon", "-1 Crayon, Add Cray-Kin"),
        arrayOf("Build a Bubble Mountain", "+3 crew, -8-10 bubbles"),
        arrayOf("Ask Mom for More Bath Toys", "+2 crayons, +8-11 bubbles"),
        arrayOf("Draw a Bunch of Coconuts with Crayons", "Block Ferocious roc, -2 crayons"),
        arrayOf("Splash in the Water", "Add Bristled Man-O-War"),
        arrayOf("Draw a Big Storm Cloud on the Shower Wall", "Block Deadly Hydra, -3 crayons"),
        arrayOf("Knock an Action Figure Overboard", "+20-23 bubbles, -1 crew"),
        arrayOf("Submerge Some Bubbles", "Block giant man-eating shark, -16 bubbles"),
        arrayOf("Turn on the Shower Wand", "Add Deadly Hydra"),
        arrayOf("Dump Bubble Bottle and Turn on the Faucet", "+13-19 bubbles"),
        arrayOf("Put the Toy Boat on the Side of the Tub", "+4 crayon, -1 crew"),
        arrayOf("Cover the Ship in Bubbles", "Block fearsome giant squid, -13-20 bubbles"),
        arrayOf("Pull the Drain Plug", "-8 crew, -3 crayons, -17 bubbles, increase NC rate"),
        arrayOf("Open a New Bathtub Crayon Box", "+3 crayons"),
        arrayOf("Sing a Bathtime Tune", "+3 crayons, +16 bubbles, -2 crew"),
        arrayOf("Surround Bubbles with Crayons", "+5 crew, -6-16 bubbles, -2 crayons"),
    )

    /** Desktop DECISION_BUTTON_PATTERN — matches option value + button text from choice HTML. */
    private val DECISION_BUTTON_REGEX = Regex(
        """<input type=hidden name=option value=(\d+)>.*?<input +class=button type=submit value="(.*?)">""",
    )

    private fun oldManPsychosisSpoilers(): ChoiceAdventures.Spoilers {
        val responseText = lastResponseText()
        val buttons = DECISION_BUTTON_REGEX.findAll(responseText).take(4).toList()
        val spoilers = Array(4) { ChoiceOption("") }
        for (match in buttons) {
            val optionNum = match.groupValues[1].toIntOrNull() ?: continue
            val buttonText = match.groupValues[2]
            val spoilerText = OLD_MAN_PSYCHOSIS_SPOILERS.firstOrNull { it[0] == buttonText }
                ?.get(1)
            if (spoilerText != null && optionNum in 1..4) {
                spoilers[optionNum - 1] = ChoiceOption(spoilerText)
            }
        }
        return ChoiceAdventures.Spoilers(
            0, // choice id is variable 636-639
            "First Mate's Log Entry",
            spoilers.toList(),
        )
    }

    // ── 641-651 Mystic's psychoses ────────────────────────────────────────────
    private fun mysticPsychosisSpoilers(choice: Int): ChoiceAdventures.Spoilers? {
        val (name, option) = when (choice) {
            641 -> {
                // Stupid Pipes — hot damage check
                val res = elementalResistanceLevels("hot")
                val damage = (2.50 * (100.0 - elementalResistanceByLevel(res))).toInt()
                val hp = currentHP()
                "Stupid Pipes." to ChoiceOption(
                    "take $damage hot damage, current HP = $hp, current hot resistance = $res",
                )
            }
            642 -> {
                // You're Freaking Kidding Me — 50 buffed stats check
                val m = buffedMuscle()
                val my = buffedMyst()
                val mx = buffedMoxie()
                "You're Freaking Kidding Me" to ChoiceOption(
                    "50 buffed Muscle/Mysticality/Moxie required, have $m/$my/$mx",
                )
            }
            644 -> {
                // Snakes — 50 buffed Moxie check
                "Snakes." to ChoiceOption("50 buffed Moxie required, have ${buffedMoxie()}")
            }
            645 -> {
                // So... Many... Skulls... — spooky damage check
                val res = elementalResistanceLevels("spooky")
                val damage = (2.50 * (100.0 - elementalResistanceByLevel(res))).toInt()
                val hp = currentHP()
                "So... Many... Skulls..." to ChoiceOption(
                    "take $damage spooky damage, current HP = $hp, current spooky resistance = $res",
                )
            }
            647 -> {
                // A Stupid Dummy — 100 weapon damage check
                "A Stupid Dummy. Also, a Straw Man." to ChoiceOption("100 weapon damage required")
            }
            648 -> {
                // Slings and Arrows — 101 HP check
                "Slings and Arrows" to ChoiceOption("101 HP required, have ${currentHP()}")
            }
            650 -> {
                // This Is Your Life — 101 MP check
                "This Is Your Life. Your Horrible, Horrible Life." to ChoiceOption(
                    "101 MP required, have ${currentMP()}",
                )
            }
            651 -> {
                // The Wall of Wailing — 10 prismatic damage check
                "The Wall of Wailing" to ChoiceOption(
                    "10 prismatic damage required, have ${currentPrismaticDamage()}",
                )
            }
            else -> return null
        }
        val options = listOf(option, ChoiceOption("flickering pixel"), ChoiceOption("skip adventure"))
        return ChoiceAdventures.Spoilers(choice, name, options)
    }

    // ── 692 I Wanna Be a Door (Daily Dungeon) ────────────────────────────────
    private fun dailyDungeonDoorSpoilers(): ChoiceAdventures.Spoilers {
        val mus = buffedMuscle()
        val mys = buffedMyst()
        val mox = buffedMoxie()
        val options = listOf(
            ChoiceOption("suffer trap effects"),
            ChoiceOption("unlock door with key, no turn spent"),
            ChoiceOption("pick lock with lockpicks, no turn spent"),
            ChoiceOption(if (mus >= 30) "bypass trap with muscle" else "suffer trap effects"),
            ChoiceOption(if (mys >= 30) "bypass trap with mysticality" else "suffer trap effects"),
            ChoiceOption(if (mox >= 30) "bypass trap with moxie" else "suffer trap effects"),
            ChoiceOption("open door with card, no turn spent"),
            ChoiceOption("leave, no turn spent"),
        )
        return ChoiceAdventures.Spoilers(692, "I Wanna Be a Door", options)
    }

    // ── 772 Saved by the Bell (KOLHS) ─────────────────────────────────────────
    private fun kolhsSpoilers(): ChoiceAdventures.Spoilers {
        val prefs = preferences
        prefs?.setInt("_kolhsAdventures", 40)

        val spiritedCount = (prefs?.getInt("kolhsTotalSchoolSpirited", 0) ?: 0) + 1
        val spiritedTurns = spiritedCount * 10
        val options = listOf(
            ChoiceOption(
                if (prefs?.getBoolean("_kolhsSchoolSpirited", false) == true) {
                    "Already got School Spirited today"
                } else {
                    "Get $spiritedTurns turns of School Spirited (+100% Meat drop, +50% Item drop)"
                },
            ),
            ChoiceOption(
                if (prefs?.getBoolean("_kolhsPoeticallyLicenced", false) == true) {
                    "Already got Poetically Licenced today"
                } else {
                    "50 turns of Poetically Licenced (+20% Myst, -20% Muscle, +2 Myst stats/fight, +10% Spell damage)"
                },
            ),
            ChoiceOption(
                if (itemCount(YEARBOOK_CAMERA) > 0 || hasEquipped(YEARBOOK_CAMERA)) {
                    "Turn in yesterday's photo (if you have it)"
                } else {
                    "Get Yearbook Camera"
                },
            ),
            ChoiceOption(
                if (prefs?.getBoolean("_kolhsCutButNotDried", false) == true) {
                    "Already got Cut But Not Dried today"
                } else {
                    "50 turns of Cut But Not Dried (+20% Muscle, -20% Moxie, +2 Muscle stats/fight, +10% Weapon damage)"
                },
            ),
            ChoiceOption(
                if (prefs?.getBoolean("_kolhsIsskayLikeAnAshtray", false) == true) {
                    "Already got Isskay Like An Ashtray today"
                } else {
                    "50 turns of Isskay Like An Ashtray (+20% Moxie, -20% Myst, +2 Moxie stats/fight, +10% Pickpocket chance)"
                },
            ),
            ChoiceOption("Make items"),
            ChoiceOption("Make items"),
            ChoiceOption("Make items"),
            ChoiceOption(""),
            ChoiceOption("Leave"),
        )
        return ChoiceAdventures.Spoilers(772, "Saved by the Bell", options)
    }

    // ── 918 Yachtzee! ─────────────────────────────────────────────────────────
    private fun yachtzeeSpoilers(): ChoiceAdventures.Spoilers {
        // Desktop tries to parse umdLastObtained date; simplified: just show both.
        val lastUmd = preferences?.getString("umdLastObtained", "") ?: ""
        val option0 = if (lastUmd.isBlank()) {
            "get cocktail ingredients (sometimes Ultimate Mind Destroyer)"
        } else {
            "get cocktail ingredients (sometimes Ultimate Mind Destroyer)"
        }
        val options = listOf(
            ChoiceOption(option0),
            ChoiceOption("get 5k meat and random item"),
            ChoiceOption("get Beach Bucks"),
        )
        return ChoiceAdventures.Spoilers(918, "Yachtzee!", options)
    }

    // ── 988 The Containment Unit (EVE) ────────────────────────────────────────
    private fun eveContainmentSpoilers(): ChoiceAdventures.Spoilers {
        val containment = preferences?.getString("EVEDirections", "") ?: ""
        val options: List<ChoiceOption> = if (containment.length != 6) {
            listOf(ChoiceOption(""), ChoiceOption(""))
        } else {
            val progress = containment.substring(5, 6).toIntOrNull() ?: -1
            if (progress < 0 || progress > 5) {
                listOf(ChoiceOption(""), ChoiceOption(""))
            } else {
                when (containment[progress]) {
                    'L' -> listOf(ChoiceOption("right way"), ChoiceOption(""))
                    'R' -> listOf(ChoiceOption(""), ChoiceOption("right way"))
                    else -> listOf(ChoiceOption("unknown"), ChoiceOption("unknown"))
                }
            }
        }
        return ChoiceAdventures.Spoilers(988, "The Containment Unit", options)
    }

    // ── 1049 Tomb of the Unknown Your Class Here ──────────────────────────────
    private fun tombSpoilers(): ChoiceAdventures.Spoilers {
        val responseText = lastResponseText()
        val choices = ChoiceUtilities.parseChoices(responseText)
        val optionCount = choices.size
        if (optionCount <= 1) {
            return ChoiceAdventures.Spoilers(1049, "Tomb of the Unknown Your Class Here", emptyList())
        }

        // Desktop ChoiceManager.getDecision(1049): find the right answer for the riddle
        // based on class. The option numbers are randomized each visit, but the correct
        // answer text remains the same.
        val decision = tombDecision(choices)
        if (decision == 0) {
            return ChoiceAdventures.Spoilers(1049, "Tomb of the Unknown Your Class Here", emptyList())
        }

        val options = (1..optionCount).map { i ->
            ChoiceOption(if (i == decision) "right answer" else "wrong answer")
        }
        return ChoiceAdventures.Spoilers(1049, "Tomb of the Unknown Your Class Here", options)
    }

    /**
     * Desktop ChoiceManager.getDecision(1049) — returns 1-based option index
     * of the right answer for the class riddle, or 0 if unknown.
     */
    internal fun tombDecision(choices: Map<Int, String>): Int {
        val answer = when (characterClass()) {
            CharacterClass.SEAL_CLUBBER -> "Boredom."
            CharacterClass.TURTLE_TAMER -> "Friendship."
            CharacterClass.PASTAMANCER -> "Binding pasta thralls."
            CharacterClass.SAUCEROR -> "Power."
            CharacterClass.DISCO_BANDIT -> "Me. Duh."
            CharacterClass.ACCORDION_THIEF -> "Music."
            else -> return 0
        }
        for ((key, text) in choices) {
            if (text.contains(answer)) return key
        }
        return 0
    }

    // ── 1411 The Hall in the Hall (Drippy Hall) ───────────────────────────────
    private fun drippyHallSpoilers(): ChoiceAdventures.Spoilers {
        val haveStaff = itemCount(DRIPPY_STAFF) > 0
        val drunk = inebriety()
        val poolSkill = estimatedPoolSkill()
        val opt0 = (if (haveStaff) "M" else "A drippy staff and m") +
            "aybe a drippy orb (Pool Skill at $drunk inebriety = $poolSkill)"

        val opt1 = "Buy a drippy candy bar for 10,000 Meat or get Driplets"

        val item = when {
            hasSkillId(SKILL_DRIPPY_EYE_SPROUT) -> "a drippy seed"
            hasSkillId(SKILL_DRIPPY_EYE_STONE) -> "a drippy bezoar"
            hasSkillId(SKILL_DRIPPY_EYE_BEETLE) -> "a drippy grub"
            else -> "nothing"
        }
        val opt2 = "Get $item"

        val steins = itemCount(DRIPPY_STEIN)
        val opt3 = if (steins > 0) "Trade a drippy stein for a drippy pilsner" else "Get nothing"

        val options = listOf(
            ChoiceOption(opt0),
            ChoiceOption(opt1),
            ChoiceOption(opt2),
            ChoiceOption(opt3),
            ChoiceOption("Get some Driplets"),
        )
        return ChoiceAdventures.Spoilers(1411, "The Hall in the Hall", options)
    }

    // ── 1499 A Labyrinth of Shadows ───────────────────────────────────────────
    private fun shadowLabyrinthSpoilers(): ChoiceAdventures.Spoilers {
        val responseText = lastResponseText()
        val choices = ChoiceUtilities.parseChoices(responseText)
        val optionCount = choices.size
        if (optionCount == 0) {
            return ChoiceAdventures.Spoilers(1499, "A Labyrinth of Shadows", emptyList())
        }
        val options = mutableListOf<ChoiceOption>()
        // option 0: "Randomize themes"
        options.add(ChoiceOption("Randomize themes"))
        // options 1-3: themed spoilers
        for (i in 1..3) {
            options.add(shadowLabyrinthSpoiler(choices[i + 1]))
        }
        // option 4: "Randomize themes"
        options.add(ChoiceOption("Randomize themes"))
        // option 5: "Leave with nothing"
        options.add(ChoiceOption("Leave with nothing"))
        return ChoiceAdventures.Spoilers(1499, "A Labyrinth of Shadows", options)
    }

    /**
     * Desktop [RufusManager.shadowLabyrinthSpoiler] — returns spoiler for one labyrinth theme
     * choice button text.
     */
    private fun shadowLabyrinthSpoiler(text: String?): ChoiceOption {
        if (text == null) return ChoiceOption("unknown theme")
        val theme = RufusManager.shadowLabyrinthThemeStatic(text) ?: return ChoiceOption("unknown theme")
        var spoiler = theme.normalDisplay
        val prefs = preferences
        if (prefs != null &&
            prefs.getString("rufusQuestType", "").equals("artifact", ignoreCase = true) &&
            questDatabase?.isQuestStep(Quest.RUFUS, QuestDatabase.STARTED) == true
        ) {
            val target = prefs.getString("rufusQuestTarget", "")
            val artifact = theme.artifact
            if (artifact != null && target.equals(artifact, ignoreCase = true)) {
                spoiler = artifact
            }
        }
        return ChoiceOption(spoiler)
    }

    // ── 1598 Play Ball! (Baseball) ────────────────────────────────────────────
    private fun baseballSpoilers(): ChoiceAdventures.Spoilers {
        val responseText = lastResponseText()
        val choices = ChoiceUtilities.parseChoices(responseText)
        val options = (0 until 5).map { i ->
            val text = choices[i + 1]
            val message = if (text == null) {
                "unknown"
            } else {
                when {
                    text.contains("Some Smoke") -> "add +5 Mus, Mys, Mox to Baseball Diamond enchants"
                    text.contains("Bring the Heat") -> "add +5 Hot Damage to Baseball Diamond enchants"
                    text.contains("Schenectady") -> "get all of batter's item drops"
                    text.contains("Deep Freeze") -> "add +3 Damage Reduction to Baseball Diamond enchants"
                    text.contains("Snow Ball") -> "add +2-4 MP Regeneration to Baseball Diamond enchants"
                    text.contains("Ice") -> "banish batter until rollover"
                    text.contains("Ghost") -> "add +3-5 HP Regeneration to Baseball Diamond enchants"
                    text.contains("Skull") -> "batter attack / defense reduced by 50% at combat start"
                    text.contains("Curveball") -> "next 3 fights with batter are free"
                    text.contains("Garbage") -> "gain either discard hot dog or most of a beer (awesome food / booze)"
                    text.contains("Bean") -> "batter takes passive stench damage each round"
                    text.contains("Cheddar") -> "track batter with +3 copies and ignore queue rejection"
                    text.contains("Slurve") -> "add +1 Sleaze Resistance to Baseball Diamond enchants"
                    text.contains("Slider") -> "add +5 Combat Initiative to Baseball Diamond enchants"
                    text.contains("Screwball") -> "increase batter ML by 3*player level"
                    else -> "unknown"
                }
            }
            ChoiceOption(message)
        } + ChoiceOption("finish the inning")
        return ChoiceAdventures.Spoilers(1598, "Play Ball!", options)
    }

    // ── Utility: findChoiceDecisionText (desktop ChoiceUtilities) ─────────────
    private fun findChoiceDecisionText(index: Int, responseText: String): String? {
        DECISION_BUTTON_REGEX.findAll(responseText).forEach { match ->
            val optionNum = match.groupValues[1].toIntOrNull() ?: return@forEach
            if (optionNum == index) return match.groupValues[2]
        }
        return null
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Track C – helper methods (Behavioral Deepen XXX)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Desktop [KoLCharacter.estimatedPoolSkill] — estimate pool skill from equipment,
     * training pref, pool shark count, and inebriety bonus/penalty.
     */
    fun computeEstimatedPoolSkill(
        poolSkillFromEquip: Int,
        inebriety: Int,
        poolSkillPref: Int,
        poolSharkCount: Int,
    ): Int {
        val drunkBonus = inebriety - (if (inebriety > 10) (inebriety - 10) * 3 else 0)
        val poolSharkBonus = when {
            poolSharkCount > 25 -> 10
            poolSharkCount > 0 -> floor(2 * sqrt(poolSharkCount.toDouble())).toInt()
            else -> 0
        }
        return poolSkillFromEquip + poolSkillPref + poolSharkBonus + drunkBonus
    }

    /** Desktop ChoiceAdventures choice 606 Clancy lute / Ed cat weight formula. */
    internal fun computeWeightedSidekickItemDrop(weight: Int): Double {
        if (weight <= 0) return 0.0
        return sqrt(55.0 * weight) + weight - 3
    }

    /** Desktop CharPaneRequest.LUTE + minstrel level weight (5 * level). */
    internal fun computeClancyLuteItemDrop(instrument: String, minstrelLevel: Int): Double {
        if (!instrument.equals("lute", ignoreCase = true)) return 0.0
        return computeWeightedSidekickItemDrop(5 * minstrelLevel)
    }

    /** Desktop EdServantData cat servant (id 1) at level 7+. */
    internal fun computeEdCatServantItemDrop(servantType: String, level: Int): Double {
        if (!servantType.equals("Cat", ignoreCase = true) || level < 7) return 0.0
        return computeWeightedSidekickItemDrop(level)
    }

    /** Desktop Companion.EGGMAN + SkillPool.WORKING_LUNCH gate. */
    internal fun computeEggmanItemDrop(hasWorkingLunch: Boolean): Double =
        if (hasWorkingLunch) 75.0 else 50.0

    /** Sum Twin Peak florist plant ITEMDROP modifiers (ModifierType.FLORIST). */
    internal fun computeFloristTwinPeakItemDrop(plants: List<FloristRequest.Florist>): Double =
        plants.sumOf { plant ->
            val entry = ModifierDatabase.get("Florist", plant.plantName) ?: return@sumOf 0.0
            ModifierParser.parse(entry.modifiers).get(DoubleModifier.ITEMDROP)
        }
}
