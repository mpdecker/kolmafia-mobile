package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.Gender
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.AdventureSpentTracker
import net.sourceforge.kolmafia.session.EncounterManager
import net.sourceforge.kolmafia.track.TrackManager

/**
 * Desktop [AreaCombatData.getMonsterData] / [AreaCombatData.areaCombatPercent] /
 * [AreaCombatData.superlikelyChance] / [AreaCombatData.adjustConditionalWeighting].
 *
 * Phases 5051–5070: banishes, rejection flags, superlikely, soft combat%.
 * Phases 5111–5130 (XXI): combat-rate wiring, moonlight rejection, saber/crystal-ball
 * queue gates, conditional weighting batch 1, expanded stateful NC%.
 * Phases 5171–5215 (XXII): conditional weighting batch 2–3 (Haert/F'c'le/shrines/
 * Oil Peak/Island War/NS contests/Nemesis/Slime/Post-Mall) + monsterLevel context.
 * Phases 5231–5260 (XXIII): Telegram/Gingerbread/Shadow Rift/Crimbo23/Fitzsimmons
 * residual weighting + Context familiar/outfit/multi-pass.
 */
object ZoneCombatCalculator {
    data class Context(
        val preferences: Preferences? = null,
        val banishManager: BanishManager? = null,
        val adventureSpent: AdventureSpentTracker? = null,
        val questDatabase: QuestDatabase? = null,
        val characterState: CharacterState? = null,
        val turnsPlayed: Int = 0,
        val ascensions: Int = 0,
        val combatRateAdjustment: Double = 0.0,
        val initiativeAdjustment: Double = 0.0,
        val crystalBallEquipped: Boolean = false,
        /** Desktop [KoLCharacter.currentNumericModifier(MONSTER_LEVEL)]. */
        val monsterLevel: Int = 0,
        val familiarId: Int = 0,
        val hatItemId: Int = 0,
        val pantsItemId: Int = 0,
        val hasMultiPass: Boolean = false,
    )

    /**
     * Monster appearance rates as percentages (0–100 scale matching desktop ASH FLOAT values).
     * Key "" is the noncombat / "none" rate.
     */
    fun appearanceRates(
        locationName: String,
        includeQueue: Boolean,
        ctx: Context = Context(),
    ): Map<String, Double> {
        val data = CombatDatabase.getByLocation(locationName) ?: return emptyMap()
        val combatPct = areaCombatPercent(data, includeQueue, ctx)
        val noneRate = if (data.combatPercent < 0) -1.0 else (100.0 - combatPct)
        val result = linkedMapOf<String, Double>("" to noneRate)
        if (combatPct < 0) return result

        val normal = data.monsters.filter { !it.superlikely }
        val superlikely = data.monsters.filter { it.superlikely }

        var totalSuperlikely = 0.0
        for (mw in superlikely) {
            val chance = superlikelyChance(mw.name, ctx)
            if (chance > 0) {
                result[mw.name] = chance
                totalSuperlikely += chance
            }
        }

        data class Effective(val name: String, val weight: Int, val rejection: Int)
        val effective = mutableListOf<Effective>()
        for (mw in normal) {
            val w = effectiveWeight(mw, locationName, includeQueue, ctx)
            if (w <= 0) continue
            effective.add(Effective(mw.name, w, getRejection(mw, ctx).coerceIn(0, 100)))
        }

        val totalWeight = effective.sumOf { it.weight * (1.0 - it.rejection / 100.0) }
        if (totalWeight <= 0) return result

        val combatFactor = combatPct / 100.0
        val nonSuperlikelyShare = (1.0 - totalSuperlikely / 100.0).coerceAtLeast(0.0)
        val weightByName = effective.associate { it.name.lowercase() to it.weight }

        for (e in effective) {
            val numerator = 100.0 * combatFactor * nonSuperlikelyShare *
                e.weight * (1.0 - e.rejection / 100.0)
            val rate = if (includeQueue) {
                AdventureQueueDatabase.applyQueueEffects(
                    numerator = numerator,
                    monsterName = e.name,
                    locationName = locationName,
                    totalWeighting = totalWeight.toInt().coerceAtLeast(1),
                    weightOf = { name -> weightByName[name.lowercase()] ?: 0 },
                    preferences = ctx.preferences,
                    turnsPlayed = ctx.turnsPlayed,
                    areaCombatPercent = combatPct,
                    crystalBallEquipped = ctx.crystalBallEquipped,
                )
            } else {
                numerator / totalWeight
            }
            result[e.name] = rate
        }
        return result
    }

    fun areaCombatPercent(data: ZoneCombatData, stateful: Boolean, ctx: Context): Double {
        if (stateful) {
            val prefs = ctx.preferences
            if (EncounterManager.isSaberForceZone(data.locationName, prefs)) {
                return 100.0
            }
            when (data.locationName) {
                "The Defiled Alcove",
                "The Defiled Cranny",
                "The Defiled Niche",
                "The Defiled Nook",
                -> {
                    val pref = when (data.locationName) {
                        "The Defiled Alcove" -> "cyrptAlcoveEvilness"
                        "The Defiled Cranny" -> "cyrptCrannyEvilness"
                        "The Defiled Niche" -> "cyrptNicheEvilness"
                        else -> "cyrptNookEvilness"
                    }
                    if ((prefs?.getInt(pref, 999) ?: 999) <= 13) return 100.0
                }
                "The Smut Orc Logging Camp" -> {
                    val progress = prefs?.getInt("smutOrcNoncombatProgress", 0) ?: 0
                    val bridge = prefs?.getInt("chasmBridgeProgress", 0) ?: 0
                    return if (progress < 15 || bridge >= 30) 100.0 else 0.0
                }
                "Barf Mountain" -> {
                    if (prefs?.getBoolean("dinseyRollercoasterNext", false) == true) {
                        return 0.0
                    }
                    return 100.0
                }
                "Investigating a Plaintive Telegram" -> {
                    val stage = prefs?.getInt("lttQuestStageCount", 0) ?: 0
                    val started = ctx.questDatabase?.isQuestStep(Quest.TELEGRAM, QuestDatabase.STARTED) == true
                    return if (stage == 9 || started) 0.0 else 100.0
                }
                "The Dripping Trees" -> {
                    val advs = prefs?.getInt("drippingTreesAdventuresSinceAscension", 0) ?: 0
                    return if (advs > 0 && advs % 15 == 0) 0.0 else 100.0
                }
                "The SMOOCH Army HQ" -> {
                    if ((prefs?.getInt("_smoochArmyHQCombats", 0) ?: 0) == 50) return 0.0
                    return 100.0
                }
            }
        }
        if (data.combatPercent < 0) return 100.0
        if (data.combatPercent == 0 || data.combatPercent == 100) return data.combatPercent.toDouble()
        val pct = data.combatPercent + ctx.combatRateAdjustment
        return pct.coerceIn(0.0, 100.0)
    }

    fun superlikelyChance(monsterName: String, ctx: Context): Double {
        val spent = ctx.adventureSpent
        val prefs = ctx.preferences
        return when (monsterName.lowercase()) {
            "screambat" -> {
                val turns = (spent?.getTurns("Guano Junction") ?: 0) +
                    (spent?.getTurns("The Batrat and Ratbat Burrow") ?: 0) +
                    (spent?.getTurns("The Beanbat Chamber") ?: 0)
                if (turns > 0 && turns % 8 == 0) 100.0 else 0.0
            }
            "modern zmobie" -> {
                if ((prefs?.getInt("cyrptAlcoveEvilness", 0) ?: 0) > 13) {
                    (15 + ctx.initiativeAdjustment / 10).coerceIn(0.0, 100.0)
                } else {
                    0.0
                }
            }
            "ninja snowman assassin" -> {
                if (ctx.combatRateAdjustment <= 0) return 0.0
                val snowmanTurns = spent?.getTurns("Lair of the Ninja Snowmen") ?: 0
                if (snowmanTurns == 10 || snowmanTurns == 20 || snowmanTurns == 30) return 100.0
                (ctx.combatRateAdjustment / 2 + snowmanTurns * 1.5).coerceIn(0.0, 100.0)
            }
            "mother hellseal" -> {
                ((prefs?.getInt("_sealScreeches", 0) ?: 0) * 10.0).coerceIn(0.0, 100.0)
            }
            "brick mulligan, the bartender" -> {
                val turns = spent?.getTurns("Kokomo Resort") ?: 0
                if (turns > 0 && turns % 25 == 0) 100.0 else 0.0
            }
            else -> if (EncounterManager.isSuperlikelyMonster(monsterName)) 0.0 else 0.0
        }
    }

    /** Desktop [AreaCombatData.getRejection] including moonlight specials. */
    fun getRejection(mw: MonsterWeight, ctx: Context): Int {
        val moonlight = when (mw.name.lowercase()) {
            "alielf", "cat-alien", "dog-alien" ->
                moonlightRejection(KolGameHolidayCalendar.grimaceMoonlight())
            "dogcat", "ferrelf", "hamsterpus" ->
                moonlightRejection(KolGameHolidayCalendar.ronaldMoonlight())
            else -> null
        }
        if (moonlight != null) return moonlight
        return mw.rejectionPercent
    }

    private fun moonlightRejection(moonlight: Int): Int {
        // Desktop: 0,1 → 8/8; 2,3,4 → 7/8, 6/8, 5/8
        return ((minOf(8, 9 - moonlight) / 8.0) * 100).toInt()
    }

    private fun effectiveWeight(
        mw: MonsterWeight,
        locationName: String,
        stateful: Boolean,
        ctx: Context,
    ): Int {
        var weight = mw.weight
        // Desktop: 'o' bit → available on even ascensions; 'e' bit → odd ascensions.
        if (mw.ascensionParity == 1 && ctx.ascensions % 2 == 1) return -2
        if (mw.ascensionParity == 2 && ctx.ascensions % 2 == 0) return -2

        weight = adjustConditionalWeighting(locationName, mw.name, weight, ctx)

        if (!stateful) return weight
        if (ctx.banishManager?.isBanished(mw.name, ctx.turnsPlayed) == true) return -3
        val prefs = ctx.preferences
        if (prefs != null) {
            val copies = TrackManager.countCopies(prefs, mw.name, ctx.turnsPlayed)
            if (copies > 0 && weight > 0) weight += copies * mw.weight.coerceAtLeast(1)
            val rwbLoc = prefs.getString("rwbLocation", "")
            val rwbCount = prefs.getInt("rwbMonsterCount", 0)
            if (rwbCount > 0 && rwbLoc.equals(locationName, ignoreCase = true) &&
                !prefs.getString("rwbMonster", "").equals(mw.name, ignoreCase = true)
            ) {
                return -3
            }
            val holdCount = prefs.getInt("holdHandsMonsterCount", 0)
            if (holdCount > 0 &&
                prefs.getString("holdHandsLocation", "").equals(locationName, ignoreCase = true) &&
                prefs.getString("holdHandsMonster", "").equals(mw.name, ignoreCase = true) &&
                weight > 0
            ) {
                weight += mw.weight.coerceAtLeast(1)
            }
        }
        return weight
    }

    /**
     * Desktop [AreaCombatData.adjustConditionalWeighting] — batches 1–3.
     */
    internal fun adjustConditionalWeighting(
        zone: String,
        monster: String,
        weighting: Int,
        ctx: Context,
    ): Int {
        val prefs = ctx.preferences
        val quest = ctx.questDatabase
        val state = ctx.characterState
        val spent = ctx.adventureSpent
        val mon = monster.lowercase()
        val ml = ctx.monsterLevel
        return when (zone) {
            "The Boss Bat's Lair" -> {
                val turns = spent?.getTurns(zone) ?: 0
                if (mon == "boss bat") {
                    if (turns > 3 && quest?.isQuestLaterThan(Quest.BAT, "step3") != true) 1 else 0
                } else {
                    if (turns > 7 || quest?.isQuestLaterThan(Quest.BAT, "step3") == true) -4 else 1
                }
            }
            "The Hidden Park" -> when {
                mon == "pygmy janitor" &&
                    (prefs?.getInt("relocatePygmyJanitor", -1) ?: -1) != ctx.ascensions -> -4
                mon == "pygmy witch lawyer" &&
                    (prefs?.getInt("relocatePygmyLawyer", -1) ?: -1) != ctx.ascensions -> -4
                else -> weighting
            }
            "The Hidden Apartment Building",
            "The Hidden Hospital",
            "The Hidden Office Building",
            "The Hidden Bowling Alley",
            -> when {
                mon == "pygmy janitor" &&
                    (prefs?.getInt("relocatePygmyJanitor", -1) ?: -1) == ctx.ascensions -> -4
                mon == "pygmy witch lawyer" &&
                    (prefs?.getInt("relocatePygmyLawyer", -1) ?: -1) == ctx.ascensions -> -4
                mon == "drunk pygmy" &&
                    (prefs?.getInt("_drunkPygmyBanishes", 0) ?: 0) >= 11 -> -4
                else -> weighting
            }
            "The Fungal Nethers" -> when (mon) {
                "muscular mushroom guy" -> if (state?.isSealClubber == true) 1 else 0
                "armored mushroom guy" -> if (state?.isTurtleTamer == true) 1 else 0
                "wizardly mushroom guy" -> if (state?.isPastamancer == true) 1 else 0
                "fiery mushroom guy" -> if (state?.isSauceror == true) 1 else 0
                "dancing mushroom guy" -> if (state?.isDiscoBandit == true) 1 else 0
                "wailing mushroom guy" -> if (state?.isAccordionThief == true) 1 else 0
                else -> weighting
            }
            "Pirates of the Garbage Barges" -> {
                if (mon == "flashy pirate" && prefs?.getBoolean("dinseyGarbagePirate", false) != true) {
                    0
                } else {
                    weighting
                }
            }
            "Uncle Gator's Country Fun-Time Liquid Waste Sluice" -> {
                if (mon == "nasty bear" &&
                    quest?.isQuestStep(Quest.NASTY_BEARS, "step1") == true
                ) {
                    1
                } else {
                    weighting
                }
            }
            "Throne Room" -> {
                if (mon == "knob goblin king" && quest?.isQuestFinished(Quest.GOBLIN) == true) {
                    0
                } else {
                    weighting
                }
            }
            "The Defiled Alcove" -> {
                val evil = prefs?.getInt("cyrptAlcoveEvilness", 0) ?: 0
                when {
                    mon == "conjoined zmombie" -> if (evil in 1..13) 1 else 0
                    mon != "modern zmobie" -> if (evil > 13) 1 else 0
                    else -> weighting
                }
            }
            "The Defiled Cranny" -> {
                val evil = prefs?.getInt("cyrptCrannyEvilness", 0) ?: 0
                when (mon) {
                    "huge ghuol" -> if (evil in 1..13) 1 else 0
                    "gaunt ghuol", "gluttonous ghuol" -> if (evil > 13) 1 else 0
                    else -> weighting
                }
            }
            "The Defiled Niche" -> {
                val evil = prefs?.getInt("cyrptNicheEvilness", 0) ?: 0
                if (mon == "gargantulihc") {
                    if (evil in 1..13) 1 else 0
                } else {
                    if (evil > 13) 1 else 0
                }
            }
            "The Defiled Nook" -> {
                val evil = prefs?.getInt("cyrptNookEvilness", 0) ?: 0
                if (mon == "giant skeelton") {
                    if (evil in 1..13) 1 else 0
                } else {
                    if (evil > 13) 1 else 0
                }
            }
            "Haert of the Cyrpt" -> {
                if (mon == "bonerdagon" &&
                    quest?.isQuestLaterThan(Quest.CYRPT, QuestDatabase.STARTED) == true
                ) {
                    0
                } else {
                    weighting
                }
            }
            "The F'c'le" -> when (mon) {
                "clingy pirate (female)" -> if (state?.gender == Gender.MALE) 1 else 0
                "clingy pirate (male)" -> if (state?.gender == Gender.FEMALE) 1 else 0
                else -> weighting
            }
            "Summoning Chamber" -> {
                if (mon == "lord spookyraven" && quest?.isQuestFinished(Quest.MANOR) == true) {
                    0
                } else {
                    weighting
                }
            }
            "An Overgrown Shrine (Northwest)" -> {
                if (mon == "dense liana" &&
                    (prefs?.getInt("hiddenApartmentProgress", 0) ?: 0) > 0
                ) {
                    0
                } else {
                    weighting
                }
            }
            "An Overgrown Shrine (Northeast)" -> {
                if (mon == "dense liana" &&
                    (prefs?.getInt("hiddenOfficeProgress", 0) ?: 0) > 0
                ) {
                    0
                } else {
                    weighting
                }
            }
            "An Overgrown Shrine (Southwest)" -> {
                if (mon == "dense liana" &&
                    (prefs?.getInt("hiddenHospitalProgress", 0) ?: 0) > 0
                ) {
                    0
                } else {
                    weighting
                }
            }
            "An Overgrown Shrine (Southeast)" -> {
                if (mon == "dense liana" &&
                    (prefs?.getInt("hiddenBowlingAlleyProgress", 0) ?: 0) > 0
                ) {
                    0
                } else {
                    weighting
                }
            }
            "A Massive Ziggurat" -> {
                val zoneTurns = spent?.getTurns(zone) ?: 0
                when {
                    mon == "dense liana" &&
                        (zoneTurns >= 3 || quest?.isQuestFinished(Quest.WORSHIP) == true) -> 0
                    mon == "protector spectre" &&
                        quest?.isQuestStep(Quest.WORSHIP, "step4") == true -> 1
                    else -> weighting
                }
            }
            "Oil Peak" -> when (mon) {
                "oil slick" -> if (ml < 20) 1 else 0
                "oil tycoon" -> if (ml in 20..49) 1 else 0
                "oil baron" -> if (ml in 50..99) 1 else 0
                "oil cartel" -> if (ml >= 100) 1 else 0
                else -> weighting
            }
            "The Battlefield (Frat Uniform)" -> fratBattlefieldWeight(monster, weighting, prefs)
            "The Battlefield (Hippy Uniform)" -> hippyBattlefieldWeight(monster, weighting, prefs)
            "Fastest Adventurer Contest" -> nsContestWeight(
                monster = monster,
                boss = "Tasmanian Dervish",
                opponentsLeft = prefs?.getInt("nsContestants1", 0) ?: 0,
            )
            "Strongest Adventurer Contest" -> nsContestWeight(
                monster = monster,
                boss = "Mr. Loathing",
                opponentsLeft = if (prefs?.getString("nsChallenge1", "") == "Muscle") {
                    prefs.getInt("nsContestants2", 0)
                } else {
                    0
                },
            )
            "Smartest Adventurer Contest" -> nsContestWeight(
                monster = monster,
                boss = "The Mastermind",
                opponentsLeft = if (prefs?.getString("nsChallenge1", "") == "Mysticality") {
                    prefs.getInt("nsContestants2", 0)
                } else {
                    0
                },
            )
            // Desktop AreaCombatData uses Muscle for Smoothest (mirrors Strongest gate).
            "Smoothest Adventurer Contest" -> nsContestWeight(
                monster = monster,
                boss = "Seannery the Conman",
                opponentsLeft = if (prefs?.getString("nsChallenge1", "") == "Muscle") {
                    prefs.getInt("nsContestants2", 0)
                } else {
                    0
                },
            )
            "Coldest Adventurer Contest" -> nsContestWeight(
                monster = monster,
                boss = "Mrs. Freeze",
                opponentsLeft = if (prefs?.getString("nsChallenge2", "") == "cold") {
                    prefs.getInt("nsContestants3", 0)
                } else {
                    0
                },
            )
            // Desktop uses Mrs. Freeze for Hottest as well.
            "Hottest Adventurer Contest" -> nsContestWeight(
                monster = monster,
                boss = "Mrs. Freeze",
                opponentsLeft = if (prefs?.getString("nsChallenge2", "") == "hot") {
                    prefs.getInt("nsContestants3", 0)
                } else {
                    0
                },
            )
            "Sleaziest Adventurer Contest" -> nsContestWeight(
                monster = monster,
                boss = "Leonard",
                opponentsLeft = if (prefs?.getString("nsChallenge2", "") == "sleaze") {
                    prefs.getInt("nsContestants3", 0)
                } else {
                    0
                },
            )
            "Spookiest Adventurer Contest" -> nsContestWeight(
                monster = monster,
                boss = "Arthur Frankenstein",
                opponentsLeft = if (prefs?.getString("nsChallenge2", "") == "spooky") {
                    prefs.getInt("nsContestants3", 0)
                } else {
                    0
                },
            )
            "Stinkiest Adventurer Contest" -> nsContestWeight(
                monster = monster,
                boss = "Odorous Humongous",
                opponentsLeft = if (prefs?.getString("nsChallenge2", "") == "stinky") {
                    prefs.getInt("nsContestants3", 0)
                } else {
                    0
                },
            )
            "The Nemesis' Lair" -> {
                val lairTurns = spent?.getTurns(zone) ?: 0
                when (mon) {
                    "hellseal guardian" -> if (state?.isSealClubber == true) 1 else 0
                    "gorgolok, the infernal seal (inner sanctum)" ->
                        if (state?.isSealClubber == true && lairTurns >= 4) 1 else 0
                    "warehouse worker" -> if (state?.isTurtleTamer == true) 1 else 0
                    "stella, the turtle poacher (inner sanctum)" ->
                        if (state?.isTurtleTamer == true && lairTurns >= 4) 1 else 0
                    "evil spaghetti cult zealot" -> if (state?.isPastamancer == true) 1 else 0
                    "spaghetti elemental (inner sanctum)" ->
                        if (state?.isPastamancer == true && lairTurns >= 4) 1 else 0
                    "security slime" -> if (state?.isSauceror == true) 1 else 0
                    "lumpy, the sinister sauceblob (inner sanctum)" ->
                        if (state?.isSauceror == true && lairTurns >= 4) 1 else 0
                    "daft punk" -> if (state?.isDiscoBandit == true) 1 else 0
                    "spirit of new wave (inner sanctum)" ->
                        if (state?.isDiscoBandit == true && lairTurns >= 4) 1 else 0
                    "mariachi bruiser" -> if (state?.isAccordionThief == true) 1 else 0
                    "somerset lopez, dread mariachi (inner sanctum)" ->
                        if (state?.isAccordionThief == true && lairTurns >= 4) 1 else 0
                    else -> weighting
                }
            }
            "The Slime Tube" -> when (mon) {
                "slime" -> if (ml <= 100) 1 else 0
                "slime hand" -> if (ml in 101..300) 1 else 0
                "slime mouth" -> if (ml in 301..600) 1 else 0
                "slime construct" -> if (ml in 601..1000) 1 else 0
                "slime colossus" -> if (ml > 1000) 1 else 0
                else -> weighting
            }
            "The Post-Mall" -> {
                val mallTurns = spent?.getTurns(zone) ?: 0
                if (mon == "sentient atm") {
                    if (mallTurns == 11) 1 else 0
                } else {
                    if (mallTurns == 11) -4 else 1
                }
            }
            "Investigating a Plaintive Telegram" ->
                telegramWeight(monster, weighting, prefs)
            "Gingerbread Civic Center",
            "Gingerbread Train Station",
            "Gingerbread Industrial Zone",
            "Gingerbread Upscale Retail District",
            -> {
                if (mon == "gingerbread pigeon" || mon == "gingerbread rat") {
                    if (prefs?.getBoolean("gingerSewersUnlocked", false) == true) 0 else 1
                } else {
                    weighting
                }
            }
            "The Canadian Wildlife Preserve" -> {
                if (mon == "wild reindeer") {
                    if (ctx.familiarId == YULE_HOUND) 1 else 0
                } else {
                    weighting
                }
            }
            "The Clumsiness Grove" -> {
                if (monster.equals("The Bat in the Spats", ignoreCase = true) ||
                    monster.equals("The Thorax", ignoreCase = true)
                ) {
                    if (monster.equals(prefs?.getString("clumsinessGroveBoss", ""), ignoreCase = true)) {
                        1
                    } else {
                        0
                    }
                } else {
                    weighting
                }
            }
            "The Maelstrom of Lovers" -> {
                if (monster.equals("The Terrible Pinch", ignoreCase = true) ||
                    monster.equals("Thug 1 and Thug 2", ignoreCase = true)
                ) {
                    if (monster.equals(prefs?.getString("maelstromOfLoversBoss", ""), ignoreCase = true)) {
                        1
                    } else {
                        0
                    }
                } else {
                    weighting
                }
            }
            "The Glacier of Jerks" -> {
                if (monster.equals("Mammon the Elephant", ignoreCase = true) ||
                    monster.equals("The Large-Bellied Snitch", ignoreCase = true)
                ) {
                    if (monster.equals(prefs?.getString("glacierOfJerksBoss", ""), ignoreCase = true)) {
                        1
                    } else {
                        0
                    }
                } else {
                    weighting
                }
            }
            "The Jungles of Ancient Loathing" -> {
                if (mon == "evil cultist") {
                    if (quest?.isQuestFinished(Quest.PRIMORDIAL) == true) 1 else 0
                } else {
                    weighting
                }
            }
            "Seaside Megalopolis" -> when (mon) {
                "cyborg policeman" ->
                    if (ctx.hasMultiPass && quest?.isQuestFinished(Quest.FUTURE) != true) 1 else 0
                "obese tourist", "terrifying robot" ->
                    if (quest?.isQuestLaterThan(Quest.FUTURE, "step1") == true) 1 else 0
                else -> weighting
            }
            "The SMOOCH Army HQ" -> {
                val combats = prefs?.getInt("_smoochArmyHQCombats", 0) ?: 0
                val minimum = when (mon) {
                    "smooch sergeant" -> 20
                    "smooch general" -> 40
                    else -> 0
                }
                if (combats >= minimum) weighting else 0
            }
            "Shadow Rift" -> shadowRiftWeight(monster, weighting, prefs)
            "Abuela's Cottage (Contested)",
            "The Embattled Factory",
            "The Bar At War",
            "A Cafe Divided",
            "The Armory Up In Arms",
            -> crimbo23Weight(monster, weighting, ctx)
            "The Brinier Deepers" -> {
                if (mon == "trophyfish" &&
                    prefs?.getBoolean("grandpaUnlockedTrophyFish", false) != true
                ) {
                    0
                } else {
                    weighting
                }
            }
            "The Wreck of the Edgar Fitzsimmons" -> {
                val hatchTurn = prefs?.getInt("_lastFitzsimmonsHatch", -1) ?: -1
                val hatchOpen = hatchTurn >= 0 && (ctx.turnsPlayed - hatchTurn) < 20
                val present = when (mon) {
                    "cargo crab", "drowned sailor" -> !hatchOpen
                    "mine crab", "unholy diver" -> hatchOpen
                    else -> true
                }
                if (present) weighting else 0
            }
            else -> weighting
        }
    }

    private const val YULE_HOUND = 269
    private const val ELF_GUARD_PATROL_CAP = 11365
    private const val ELF_GUARD_HOTPANTS = 11366
    private const val CRIMBUCCANEER_TRICORN = 11367
    private const val CRIMBUCCANEER_BREECHES = 11368

    private fun telegramWeight(monster: String, weighting: Int, prefs: Preferences?): Int {
        val quest = prefs?.getString("lttQuestName", "").orEmpty()
        val step = prefs?.getString("questLTTQuestByWire", "").orEmpty()
        fun match(vararg pairs: Pair<String, String>): Boolean =
            pairs.any { (q, s) -> quest == q && step == s }
        return when (monster) {
            "drunk cowpoke" -> if (
                match(
                    "Missing: Fancy Man" to "step1",
                    "Help!  Desperados|" to "step1",
                    "Big Gambling Tournament Announced" to "step1",
                    "Sheriff Wanted" to "step1",
                    "Madness at the Mine" to "step1",
                )
            ) 1 else 0
            "surly gambler" -> if (
                match(
                    "Missing: Fancy Man" to "step1",
                    "Big Gambling Tournament Announced" to "step3",
                    "Sheriff Wanted" to "step1",
                )
            ) 1 else 0
            "wannabe gunslinger" -> if (
                match(
                    "Help!  Desperados|" to "step1",
                    "Big Gambling Tournament Announced" to "step1",
                    "Sheriff Wanted" to "step1",
                    "Wagon Train Escort Wanted" to "step3",
                )
            ) 1 else 0
            "cow cultist" -> if (
                match(
                    "Missing: Pioneer Daughter" to "step2",
                    "Haunted Boneyard" to "step3",
                    "Sheriff Wanted" to "step2",
                    "Missing: Many Children" to "step1",
                )
            ) 1 else 0
            "hired gun" -> if (
                match(
                    "Missing: Fancy Man" to "step1",
                    "Help!  Desperados|" to "step1",
                    "Missing: Pioneer Daughter" to "step2",
                    "Big Gambling Tournament Announced" to "step3",
                    "Sheriff Wanted" to "step3",
                    "Missing: Many Children" to "step1",
                    "Wagon Train Escort Wanted" to "step3",
                )
            ) 1 else 0
            "camp cook" -> if (
                match(
                    "Missing: Fancy Man" to "step2",
                    "Sheriff Wanted" to "step3",
                    "Madness at the Mine" to "step1",
                    "Wagon Train Escort Wanted" to "step3",
                )
            ) 1 else 0
            "skeletal gunslinger" -> if (
                match(
                    "Help!  Desperados|" to "step3",
                    "Haunted Boneyard" to "step1",
                    "Madness at the Mine" to "step3",
                    "Wagon Train Escort Wanted" to "step2",
                )
            ) 1 else 0
            "restless ghost" -> if (
                match(
                    "Missing: Fancy Man" to "step3",
                    "Missing: Pioneer Daughter" to "step1",
                    "Haunted Boneyard" to "step2",
                    "Madness at the Mine" to "step3",
                    "Missing: Many Children" to "step2",
                    "Wagon Train Escort Wanted" to "step2",
                )
            ) 1 else 0
            "buzzard" -> if (
                match(
                    "Missing: Fancy Man" to "step2",
                    "Help! Desperados|" to "step2",
                    "Missing: Pioneer Daughter" to "step1",
                    "Haunted Boneyard" to "step1",
                )
            ) 1 else 0
            "mountain lion" -> if (
                match(
                    "Missing: Fancy Man" to "step2",
                    "Help!  Desperados|" to "step2",
                    "Sheriff Wanted" to "step2",
                    "Madness at the Mine" to "step2",
                    "Wagon Train Escort Wanted" to "step1",
                )
            ) 1 else 0
            "grizzled bear" -> if (
                match(
                    "Help!  Desperados|" to "step3",
                    "Madness at the Mine" to "step3",
                    "Wagon Train Escort Wanted" to "step1",
                )
            ) 1 else 0
            "diamondback rattler" -> if (
                match(
                    "Help!  Desperados|" to "step2",
                    "Big Gambling Tournament Announced" to "step2",
                    "Madness at the Mine" to "step2",
                    "Wagon Train Escort Wanted" to "step1",
                )
            ) 1 else 0
            "coal snake" -> if (
                match(
                    "Missing: Fancy Man" to "step3",
                    "Big Gambling Tournament Announced" to "step2",
                    "Madness at the Mine" to "step1",
                )
            ) 1 else 0
            "frontwinder" -> if (
                match(
                    "Big Gambling Tournament Announced" to "step2",
                    "Sheriff Wanted" to "step2",
                )
            ) 1 else 0
            "caugr" -> if (
                match(
                    "Missing: Pioneer Daughter" to "step3",
                    "Missing: Many Children" to "step3",
                )
            ) 1 else 0
            "pyrobove" -> if (
                match(
                    "Missing: Pioneer Daughter" to "step3",
                    "Missing: Many Children" to "step3",
                    "Wagon Train Escort Wanted" to "step2",
                )
            ) 1 else 0
            "spidercow" -> if (
                match(
                    "Missing: Pioneer Daughter" to "step3",
                    "Haunted Boneyard" to "step3",
                    "Missing: Many Children" to "step1",
                )
            ) 1 else 0
            "moomy" -> if (
                match(
                    "Haunted Boneyard" to "step3",
                    "Madness at the Mine" to "step2",
                    "Missing: Many Children" to "step3",
                )
            ) 1 else 0
            "Jeff the Fancy Skeleton" ->
                if (quest == "Missing: Fancy Man" && step == "step4") 1 else 0
            "Daisy the Unclean" ->
                if (quest == "Missing: Pioneer Daughter" && step == "step4") 1 else 0
            "Pecos Dave" ->
                if (quest == "Help!  Desperados|" && step == "step4") 1 else 0
            "Pharaoh Amoon-Ra Cowtep" ->
                if (quest == "Haunted Boneyard" && step == "step4") 1 else 0
            "Snake-Eyes Glenn" ->
                if (quest == "Big Gambling Tournament Announced" && step == "step4") 1 else 0
            "Former Sheriff Dan Driscoll" ->
                if (quest == "Sheriff Wanted" && step == "step4") 1 else 0
            "unusual construct" ->
                if (quest == "Madness at the Mine" && step == "step4") 1 else 0
            "Clara" ->
                if (quest == "Missing: Many Children" && step == "step4") 1 else 0
            "Granny Hackleton" ->
                if (quest == "Wagon Train Escort Wanted" && step == "step4") 1 else 0
            else -> weighting
        }
    }

    private fun shadowRiftWeight(
        monster: String,
        weighting: Int,
        prefs: Preferences?,
    ): Int {
        val ingress = prefs?.getString("shadowRiftIngress", "").orEmpty()
        val ok = when (monster.lowercase()) {
            "shadow bat" -> ingress in setOf("manor3", "pyramid", "plains", "giantcastle")
            "shadow cow" -> ingress in setOf("mclargehuge", "plains", "town_right")
            "shadow devil" -> ingress in setOf("desertbeach", "manor3", "woods")
            "shadow guy" -> ingress in setOf("forestvillage", "town_right", "giantcastle", "cemetery")
            "shadow hexagon" -> ingress in setOf("mclargehuge", "8bit", "forestvillage")
            "shadow orb" -> ingress in setOf("desertbeach", "8bit", "beanstalk", "giantcastle")
            "shadow prism" -> ingress in setOf("8bit", "town_right", "beanstalk")
            "shadow slab" -> ingress in setOf("pyramid", "hiddencity", "cemetery")
            "shadow spider" -> ingress in setOf("manor3", "forestvillage", "plains")
            "shadow snake" -> ingress in setOf("desertbeach", "pyramid", "hiddencity")
            "shadow stalk" -> ingress in setOf("hiddencity", "beanstalk", "woods")
            "shadow tree" -> ingress in setOf("mclargehuge", "woods", "cemetery")
            else -> true
        }
        return if (ok) weighting else -4
    }

    private fun crimbo23Weight(monster: String, weighting: Int, ctx: Context): Int {
        val elfOutfit =
            ctx.hatItemId == ELF_GUARD_PATROL_CAP && ctx.pantsItemId == ELF_GUARD_HOTPANTS
        val pirateOutfit =
            ctx.hatItemId == CRIMBUCCANEER_TRICORN && ctx.pantsItemId == CRIMBUCCANEER_BREECHES
        val elfMonster = monster.startsWith("Elf Guard")
        val pirateMonster = monster.startsWith("Crimbuccaneer")
        return when (monster) {
            "Crimbuccaneer military school dropout",
            "Crimbuccaneer new recruit",
            "Crimbuccaneer privateer",
            "Elf Guard conscript",
            "Elf Guard convict",
            "Elf Guard private",
            -> if (!elfOutfit && !pirateOutfit) 1 else 0
            else -> if ((elfOutfit && pirateMonster) || (pirateOutfit && elfMonster)) 1 else 0
        }
    }

    private fun nsContestWeight(monster: String, boss: String, opponentsLeft: Int): Int =
        if (monster.equals(boss, ignoreCase = true)) {
            if (opponentsLeft == 1) 1 else 0
        } else {
            if (opponentsLeft > 1) 1 else 0
        }

    private fun fratBattlefieldWeight(
        monster: String,
        weighting: Int,
        prefs: Preferences?,
    ): Int {
        val hippiesDefeated = prefs?.getInt("hippiesDefeated", 0) ?: 0
        if (hippiesDefeated == 1000) {
            return if (monster.equals("The Big Wisniewski", ignoreCase = true)) 1 else 0
        }
        return when (monster) {
            "Bailey's Beetle" ->
                if (prefs?.getString("sidequestJunkyardCompleted", "") == "hippy") 1 else 0
            "Green Ops Soldier" -> if (hippiesDefeated >= 401) 1 else 0
            "Mobile Armored Sweat Lodge" -> if (hippiesDefeated >= 151) 1 else 0
            "War Hippy Airborne Commander" -> if (hippiesDefeated >= 351) 1 else 0
            "War Hippy Baker" -> if (hippiesDefeated <= 600) 2 else 0
            "War Hippy Dread Squad" -> if (hippiesDefeated <= 850) 1 else 0
            "War Hippy Elder Shaman" -> if (hippiesDefeated >= 251) 1 else 0
            "War Hippy Elite Fire Spinner" -> if (hippiesDefeated >= 501) 1 else 0
            "War Hippy Elite Rigger" -> if (hippiesDefeated >= 301) 2 else 0
            "War Hippy F.R.O.G." -> if (hippiesDefeated in 51..500) 2 else 0
            "War Hippy Fire Spinner" -> if (hippiesDefeated in 301..650) 1 else 0
            "War Hippy Green Gourmet" -> if (hippiesDefeated in 201..750) 2 else 0
            "War Hippy Homeopath" -> if (hippiesDefeated <= 900) 1 else 0
            "War Hippy Infantryman" -> if (hippiesDefeated <= 400) 2 else 0
            "War Hippy Naturopathic Homeopath" -> if (hippiesDefeated >= 451) 1 else 0
            "War Hippy Rigger" -> if (hippiesDefeated <= 800) 2 else 0
            "War Hippy Shaman" -> if (hippiesDefeated in 26..700) 1 else 0
            "War Hippy Sky Captain" -> if (hippiesDefeated in 76..550) 1 else 0
            "War Hippy Windtalker" -> if (hippiesDefeated > 0) 1 else 0
            "Slow Talkin' Elliot" -> if (hippiesDefeated in 501..600) -1 else 0
            "Neil" -> if (hippiesDefeated in 601..700) -1 else 0
            "Zim Merman" -> if (hippiesDefeated in 701..800) -1 else 0
            "C.A.R.N.I.V.O.R.E. Operative" -> if (hippiesDefeated in 801..900) -1 else 0
            "Glass of Orange Juice" -> if (hippiesDefeated in 901..999) -1 else 0
            else -> weighting
        }
    }

    private fun hippyBattlefieldWeight(
        monster: String,
        weighting: Int,
        prefs: Preferences?,
    ): Int {
        val fratboysDefeated = prefs?.getInt("fratboysDefeated", 0) ?: 0
        if (fratboysDefeated == 1000) {
            return if (monster.equals("The Man", ignoreCase = true)) 1 else 0
        }
        return when (monster) {
            "War Frat Mobile Grill Unit" ->
                if (prefs?.getString("sidequestJunkyardCompleted", "") == "fratboy") 1 else 0
            "Sorority Operator" -> if (fratboysDefeated >= 151) 1 else 0
            "Panty Raider Frat Boy" -> if (fratboysDefeated >= 401) 1 else 0
            "Next-generation Frat Boy" -> if (fratboysDefeated in 501..600) -1 else 0
            "Monty Basingstoke-Pratt, IV" -> if (fratboysDefeated in 601..700) -1 else 0
            "Brutus, the toga-clad lout" -> if (fratboysDefeated in 701..800) -1 else 0
            "Danglin' Chad" -> if (fratboysDefeated in 801..900) -1 else 0
            "War Frat Streaker" -> if (fratboysDefeated in 901..999) -1 else 0
            else -> weighting
        }
    }
}
