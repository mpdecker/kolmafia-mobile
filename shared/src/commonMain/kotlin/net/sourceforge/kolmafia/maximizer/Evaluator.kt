package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.MainStat
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.OutfitDatabase
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.modifiers.BooleanModifier
import net.sourceforge.kolmafia.modifiers.CurrentModifiers
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * Desktop Evaluator scoring subset: weighted goal parsing + [getScore] special cases.
 * Phase 364: addFudge + boolean constraint flags ([checkConstraints], [failed]).
 * Phase 367: global loadout gates ([totalMin], [totalMax], [exceeded]).
 * Phase 382: +current/-current, outfit/equip gates ([checkEquipment]).
 * Phase 383: bonus/letter/number, plumber/cold plumber, neg equip.
 */
class Evaluator private constructor(
    private val weights: MutableMap<DoubleModifier, Double>,
    private val mins: MutableMap<DoubleModifier, Double>,
    private val maxes: MutableMap<DoubleModifier, Double>,
    private var mainstatWeight: Double,
    private var totalMin: Double = Double.NEGATIVE_INFINITY,
    private var totalMax: Double = Double.POSITIVE_INFINITY,
    private val booleanMask: MutableSet<BooleanModifier> = mutableSetOf(),
    private val booleanValue: MutableSet<BooleanModifier> = mutableSetOf(),
    private var current: Boolean = false,
    private var noTiebreaker: Boolean = false,
    val posOutfits: MutableSet<String> = mutableSetOf(),
    val negOutfits: MutableSet<String> = mutableSetOf(),
    val posEquip: MutableSet<String> = mutableSetOf(),
    val negEquip: MutableSet<String> = mutableSetOf(),
    private val bonuses: MutableMap<String, Double> = mutableMapOf(),
    private val bonusFuncs: MutableList<Pair<(String) -> Double, Double>> = mutableListOf(),
    private var plumberRequested: Boolean = false,
    private var coldPlumberRequested: Boolean = false,
) {
    enum class Constraint {
        /** Item violates a constraint, don't use it */
        VIOLATES,
        /** Item not relevant to any constraints */
        IRRELEVANT,
        /** Item meets a constraint, give it special handling */
        MEETS,
    }

    /** Set when [getScore] finds a per-modifier min violation (desktop Evaluator.failed). */
    var failed: Boolean = false
        private set

    internal fun markFailed() {
        failed = true
    }

    /** Set when total score reaches [totalMax] (desktop Evaluator.exceeded). */
    var exceeded: Boolean = false
        private set

    constructor(expr: String) : this(
        mutableMapOf(),
        mutableMapOf(),
        mutableMapOf(),
        0.0,
    ) {
        parse(expr)
    }

    /** Desktop Evaluator.current — keep zero-delta equipped items during enumeration. */
    fun considerCurrent(): Boolean = current

    /** Desktop Evaluator.isUsingTiebreaker — false when `-tie` or `ocrs` disables tiebreak. */
    fun usesTiebreaker(): Boolean = !noTiebreaker

    fun addPosOutfit(name: String) {
        posOutfits.add(name)
    }

    fun addNegOutfit(name: String) {
        negOutfits.add(name)
    }

    fun addPosEquip(name: String) {
        posEquip.add(name)
    }

    fun addNegEquip(name: String) {
        negEquip.add(name)
    }

    fun isNegEquip(itemName: String): Boolean =
        negEquip.any { it.equals(itemName, ignoreCase = true) }

    /** Flat + functional bonuses for one equipment piece (enumeration delta). */
    fun itemBonus(itemName: String): Double {
        if (bonuses.isEmpty() && bonusFuncs.isEmpty()) return 0.0
        var score = 0.0
        for ((name, weight) in bonuses) {
            if (name.equals(itemName, ignoreCase = true)) {
                score += weight
            }
        }
        for ((func, weight) in bonusFuncs) {
            score += func(itemName) * weight
        }
        return score
    }

    /** Sum of [itemBonus] over a worn loadout (desktop getScore equipment bonuses). */
    fun equipmentBonus(equipmentNames: Collection<String>): Double {
        if (bonuses.isEmpty() && bonusFuncs.isEmpty()) return 0.0
        return equipmentNames.sumOf { itemBonus(it) }
    }

    /**
     * Resolve deferred `plumber` / `cold plumber` goal terms once inventory is known.
     * Returns false when path or tools are unavailable (desktop aborts maximize).
     */
    fun resolvePlumberTools(
        charState: CharacterState,
        accessibleCount: (Int) -> Int,
        gameDatabase: GameDatabase,
    ): Boolean {
        if (!plumberRequested && !coldPlumberRequested) return true
        if (charState.ascensionPath != AscensionPath.PLUMBER) return false

        if (coldPlumberRequested) {
            val flower = pickPlumberTool(1, accessibleCount, gameDatabase)
                ?: return false
            posEquip.add(flower)
            resolveEquipName(FROSTY_BUTTON, gameDatabase)?.let { posEquip.add(it) }
                ?: posEquip.add(FROSTY_BUTTON)
        } else if (plumberRequested) {
            val primeIndex = when (charState.mainStat) {
                MainStat.MUSCLE -> 0
                MainStat.MYSTICALITY -> 1
                MainStat.MOXIE -> 2
            }
            val tool = pickPlumberTool(primeIndex, accessibleCount, gameDatabase)
                ?: pickPlumberTool(-1, accessibleCount, gameDatabase)
                ?: return false
            posEquip.add(tool)
        }
        return true
    }

    /**
     * Desktop Evaluator.checkEquipment — final loadout outfit/equip/beeosity gates.
     */
    fun checkEquipment(
        equipment: Map<EquipmentSlot, String>,
        beeosity: Int,
        maxBeeosity: Int,
        inBeecore: Boolean = false,
    ) {
        if (failed) return
        var outfitSatisfied = posOutfits.isEmpty()
        var equipSatisfied = posEquip.isEmpty()
        if (posEquip.isNotEmpty()) {
            equipSatisfied = posEquip.all { required ->
                equipment.values.any { it.equals(required, ignoreCase = true) }
            }
        }
        val outfitName = detectOutfitName(equipment)
        if (outfitName != null && negOutfits.any { it.equals(outfitName, ignoreCase = true) }) {
            failed = true
            return
        }
        if (posOutfits.isNotEmpty()) {
            outfitSatisfied = outfitName != null &&
                posOutfits.any { it.equals(outfitName, ignoreCase = true) }
        }
        if (!outfitSatisfied || !equipSatisfied) {
            failed = true
        }
        if (inBeecore && beeosity > maxBeeosity) {
            failed = true
        }
    }

    /** Tiebreaker score honoring [usesTiebreaker]. */
    fun tiebreakerScore(mods: CurrentModifiers): Double =
        if (noTiebreaker) 0.0 else tiebreaker().getScore(mods)

    /** Parsed weight for [mod] (test / Phase 362 multi-stat goals). */
    internal fun weightOf(mod: DoubleModifier): Double = weights[mod] ?: 0.0

    /** Parsed per-modifier max cap (test). */
    internal fun maxOf(mod: DoubleModifier): Double =
        maxes[mod] ?: Double.POSITIVE_INFINITY

    /** Parsed standalone total score floor (desktop Evaluator.totalMin). */
    internal fun totalMin(): Double = totalMin

    /** Parsed standalone total score ceiling (desktop Evaluator.totalMax). */
    internal fun totalMax(): Double = totalMax

    /** Sync boolean constraints parsed by [MaximizeGoal] into desktop booleanMask/booleanValue. */
    fun applyBooleanConstraints(
        required: Set<BooleanModifier>,
        forbidden: Set<BooleanModifier>,
    ) {
        booleanMask.clear()
        booleanValue.clear()
        booleanMask.addAll(required)
        booleanMask.addAll(forbidden)
        booleanValue.addAll(required)
    }

    /** Desktop Evaluator.checkConstraints — boolean goal terms vs item modifiers. */
    fun checkConstraints(values: net.sourceforge.kolmafia.modifiers.ModifierValues): Constraint {
        if (booleanMask.isEmpty()) return Constraint.IRRELEVANT
        val active = booleanMask.filter { values.get(it) }.toSet()
        if (!booleanValue.containsAll(active)) return Constraint.VIOLATES
        if (active.isNotEmpty()) return Constraint.MEETS
        return Constraint.IRRELEVANT
    }

    /** Highest-weight stat for thrall/familiar helper scoring. */
    internal fun highestWeightedStat(): DoubleModifier? {
        val tieOrder = listOf(
            DoubleModifier.ITEMDROP,
            DoubleModifier.MEATDROP,
            DoubleModifier.MUS,
            DoubleModifier.MYS,
            DoubleModifier.MOX,
        )
        var bestWeight = 0.0
        var best: DoubleModifier? = null
        for (mod in DoubleModifier.entries) {
            val w = weights[mod] ?: 0.0
            if (w > bestWeight + 1e-9) {
                bestWeight = w
                best = mod
            }
        }
        if (best == null || bestWeight == 0.0) return null
        val tied = weights.filter { (_, w) -> kotlin.math.abs(w - bestWeight) < 1e-9 }.keys
        for (preferred in tieOrder) {
            if (preferred in tied) return preferred
        }
        return best
    }

    /** Weighted item modifier sum for candidate ranking (no loadout-only baselines). */
    fun getItemContribution(values: net.sourceforge.kolmafia.modifiers.ModifierValues): Double {
        var score = 0.0
        for (mod in DoubleModifier.entries) {
            val weight = weights[mod] ?: 0.0
            if (weight == 0.0) continue
            var value = values.get(mod)
            value = when (mod) {
                DoubleModifier.COLD_RESISTANCE,
                DoubleModifier.HOT_RESISTANCE,
                DoubleModifier.SLEAZE_RESISTANCE,
                DoubleModifier.SPOOKY_RESISTANCE,
                DoubleModifier.STENCH_RESISTANCE,
                -> resistanceValue(values, mod)
                DoubleModifier.WEAPON_DAMAGE ->
                    value + values.get(DoubleModifier.WEAPON_DAMAGE_PCT)
                DoubleModifier.RANGED_DAMAGE ->
                    value + values.get(DoubleModifier.RANGED_DAMAGE_PCT)
                DoubleModifier.SPELL_DAMAGE ->
                    value + values.get(DoubleModifier.SPELL_DAMAGE_PCT)
                DoubleModifier.DAMAGE_AURA ->
                    value + values.get(DoubleModifier.SPORADIC_DAMAGE_AURA)
                DoubleModifier.THORNS ->
                    value + values.get(DoubleModifier.SPORADIC_THORNS)
                else -> value
            }
            val maxBound = maxes[mod] ?: Double.POSITIVE_INFINITY
            score += weight * min(value, maxBound)
        }
        return score
    }

    fun getScore(mods: CurrentModifiers): Double {
        failed = false
        exceeded = false
        val v = mods.values
        var score = 0.0

        for (mod in DoubleModifier.entries) {
            val weight = weights[mod] ?: 0.0
            val minBound = mins[mod] ?: Double.NEGATIVE_INFINITY
            if (weight == 0.0 && minBound == Double.NEGATIVE_INFINITY) continue

            var value = v.get(mod)
            val maxBound = maxes[mod] ?: Double.POSITIVE_INFINITY

            value = when (mod) {
                DoubleModifier.MUS -> mods.buffedMuscle().toDouble()
                DoubleModifier.MYS -> mods.buffedMysticality().toDouble()
                DoubleModifier.MOX -> mods.buffedMoxie().toDouble()
                DoubleModifier.HP -> mods.buffedHp().toDouble()
                DoubleModifier.MP -> mods.buffedMp().toDouble()
                DoubleModifier.FAMILIAR_WEIGHT -> {
                    var fw = value + v.get(DoubleModifier.HIDDEN_FAMILIAR_WEIGHT)
                    if (v.get(DoubleModifier.FAMILIAR_WEIGHT_PCT) < 0.0) fw *= 0.5
                    fw
                }
                DoubleModifier.MANA_COST ->
                    value + v.get(DoubleModifier.STACKABLE_MANA_COST)
                DoubleModifier.INITIATIVE ->
                    value + min(0.0, v.get(DoubleModifier.INITIATIVE_PENALTY))
                DoubleModifier.MEATDROP ->
                    value + 100.0 +
                        min(0.0, v.get(DoubleModifier.MEATDROP_PENALTY)) +
                        v.get(DoubleModifier.SPORADIC_MEATDROP) +
                        v.get(DoubleModifier.MEAT_BONUS) / 10_000.0
                DoubleModifier.ITEMDROP ->
                    value + 100.0 +
                        min(0.0, v.get(DoubleModifier.ITEMDROP_PENALTY)) +
                        v.get(DoubleModifier.SPORADIC_ITEMDROP)
                DoubleModifier.WEAPON_DAMAGE ->
                    value + v.get(DoubleModifier.WEAPON_DAMAGE_PCT)
                DoubleModifier.RANGED_DAMAGE ->
                    value + v.get(DoubleModifier.RANGED_DAMAGE_PCT)
                DoubleModifier.SPELL_DAMAGE ->
                    value + v.get(DoubleModifier.SPELL_DAMAGE_PCT)
                DoubleModifier.COLD_RESISTANCE -> resistanceValue(v, mod)
                DoubleModifier.HOT_RESISTANCE -> resistanceValue(v, mod)
                DoubleModifier.SLEAZE_RESISTANCE -> resistanceValue(v, mod)
                DoubleModifier.SPOOKY_RESISTANCE -> resistanceValue(v, mod)
                DoubleModifier.STENCH_RESISTANCE -> resistanceValue(v, mod)
                DoubleModifier.EXPERIENCE -> experienceValue(mods, v)
                DoubleModifier.DAMAGE_AURA ->
                    value + v.get(DoubleModifier.SPORADIC_DAMAGE_AURA)
                DoubleModifier.THORNS ->
                    value + v.get(DoubleModifier.SPORADIC_THORNS)
                else -> value
            }

            if (value < minBound) failed = true
            score += weight * min(value, maxBound)
        }

        if (mainstatWeight != 0.0) {
            score += mainstatWeight * mods.buffedMainStat()
        }

        if (score < totalMin) failed = true
        if (score >= totalMax) exceeded = true

        return score
    }

    private fun parse(expr: String) {
        var remaining = expr.trim().lowercase()
        var lastIndex: DoubleModifier? = null
        var forceCurrent = false

        while (remaining.isNotEmpty()) {
            val match = TERM_PATTERN.matchAt(remaining, 0) ?: break
            remaining = remaining.substring(match.range.last + 1)

            val sign = match.groupValues[1]
            val numStr = match.groupValues[2]
            val weight = parseWeight(sign, numStr)

            var keyword = match.groupValues[3].trim()
            if (keyword.startsWith('"') && keyword.endsWith('"')) {
                keyword = keyword.substring(1, keyword.length - 1).trim()
            }

            when (keyword) {
                "min" -> {
                    if (lastIndex != null) {
                        mins[lastIndex] = weight
                    } else {
                        totalMin = weight
                    }
                    continue
                }
                "max" -> {
                    if (lastIndex != null) {
                        maxes[lastIndex] = weight
                    } else {
                        totalMax = weight
                    }
                    continue
                }
            }

            if (keyword.startsWith("tie")) {
                noTiebreaker = weight < 0.0
                continue
            }

            if (keyword.startsWith("current")) {
                current = weight > 0.0
                forceCurrent = true
                continue
            }

            if (keyword.startsWith("equip ")) {
                resolveEquipName(keyword.substring(6).trim())?.let { resolved ->
                    if (weight > 0.0) posEquip.add(resolved) else negEquip.add(resolved)
                }
                continue
            }

            if (keyword.startsWith("bonus ")) {
                resolveEquipName(keyword.substring(6).trim())?.let { resolved ->
                    bonuses[resolved] = weight
                }
                continue
            }

            if (keyword.startsWith("letter")) {
                val letterKey = keyword.substring(6).trim()
                if (letterKey.isEmpty()) {
                    bonusFuncs.add({ name: String -> MaximizerLetterBonus.letterBonus(name) } to weight)
                } else {
                    val letter = letterKey
                    bonusFuncs.add({ name: String -> MaximizerLetterBonus.letterBonus(name, letter) } to weight)
                }
                continue
            }

            if (keyword == "number") {
                bonusFuncs.add({ name: String -> MaximizerLetterBonus.numberBonus(name) } to weight)
                continue
            }

            if (keyword == "plumber") {
                plumberRequested = true
                continue
            }

            if (keyword == "cold plumber") {
                coldPlumberRequested = true
                continue
            }

            keyword = normalizeKeyword(keyword)

            if (keyword.startsWith("main")) {
                mainstatWeight += weight
                lastIndex = null
                continue
            }

            when (keyword) {
                "hp regen" -> {
                    val half = weight / 2.0
                    weights[DoubleModifier.HP_REGEN_MIN] =
                        (weights[DoubleModifier.HP_REGEN_MIN] ?: 0.0) + half
                    weights[DoubleModifier.HP_REGEN_MAX] =
                        (weights[DoubleModifier.HP_REGEN_MAX] ?: 0.0) + half
                    lastIndex = DoubleModifier.HP_REGEN_MAX
                    continue
                }
                "mp regen" -> {
                    val half = weight / 2.0
                    weights[DoubleModifier.MP_REGEN_MIN] =
                        (weights[DoubleModifier.MP_REGEN_MIN] ?: 0.0) + half
                    weights[DoubleModifier.MP_REGEN_MAX] =
                        (weights[DoubleModifier.MP_REGEN_MAX] ?: 0.0) + half
                    lastIndex = DoubleModifier.MP_REGEN_MAX
                    continue
                }
            }

            val indices = resolveKeyword(keyword)
            if (indices.isEmpty()) continue

            if (indices.singleOrNull() == DoubleModifier.RANDOM_MONSTER_MODIFIERS && keyword == "ocrs") {
                noTiebreaker = true
            }

            for (index in indices) {
                weights[index] = (weights[index] ?: 0.0) + weight
                lastIndex = index
            }
        }

        // If no tiebreaker, consider current unless -current specified
        if (!forceCurrent && noTiebreaker) {
            current = true
        }

        applyFudgeGroups()
    }

    private fun applyFudgeGroups() {
        addFudge(
            DoubleModifier.EXPERIENCE,
            DoubleModifier.MONSTER_LEVEL,
            DoubleModifier.MONSTER_LEVEL_PERCENT,
            DoubleModifier.MUS_EXPERIENCE,
            DoubleModifier.MYS_EXPERIENCE,
            DoubleModifier.MOX_EXPERIENCE,
            DoubleModifier.MUS_EXPERIENCE_PCT,
            DoubleModifier.MYS_EXPERIENCE_PCT,
            DoubleModifier.MOX_EXPERIENCE_PCT,
            DoubleModifier.VOLLEYBALL_WEIGHT,
            DoubleModifier.SOMBRERO_WEIGHT,
            DoubleModifier.VOLLEYBALL_EFFECTIVENESS,
            DoubleModifier.SOMBRERO_EFFECTIVENESS,
            DoubleModifier.SOMBRERO_BONUS,
        )
        addFudge(
            DoubleModifier.ITEMDROP,
            DoubleModifier.FOODDROP,
            DoubleModifier.BOOZEDROP,
            DoubleModifier.HATDROP,
            DoubleModifier.WEAPONDROP,
            DoubleModifier.OFFHANDDROP,
            DoubleModifier.SHIRTDROP,
            DoubleModifier.PANTSDROP,
            DoubleModifier.ACCESSORYDROP,
            DoubleModifier.CANDYDROP,
            DoubleModifier.GEARDROP,
            DoubleModifier.FAIRY_WEIGHT,
            DoubleModifier.FAIRY_EFFECTIVENESS,
            DoubleModifier.SPORADIC_ITEMDROP,
            DoubleModifier.PICKPOCKET_CHANCE,
        )
        addFudge(
            DoubleModifier.MEATDROP,
            DoubleModifier.LEPRECHAUN_WEIGHT,
            DoubleModifier.LEPRECHAUN_EFFECTIVENESS,
            DoubleModifier.SPORADIC_MEATDROP,
            DoubleModifier.MEAT_BONUS,
        )
        addFudge(DoubleModifier.DAMAGE_AURA, DoubleModifier.SPORADIC_DAMAGE_AURA)
        addFudge(DoubleModifier.THORNS, DoubleModifier.SPORADIC_THORNS)
    }

    private fun addFudge(source: DoubleModifier, vararg extras: DoubleModifier) {
        val fudge = weightOf(source) * 0.0001
        if (fudge <= 0.0) return
        for (extra in extras) {
            weights[extra] = (weights[extra] ?: 0.0) + fudge
        }
    }

    private fun parseWeight(sign: String, numStr: String): Double {
        if (numStr.isEmpty()) {
            return when (sign) {
                "-" -> -1.0
                else -> 1.0
            }
        }
        val combined = sign + numStr
        return combined.toDoubleOrNull() ?: 1.0
    }

    private fun normalizeKeyword(keyword: String): String {
        var k = keyword
        if (k == "all res" || k == "all resistance") return k
        if (k.endsWith(" res")) {
            k = k.substring(0, k.length - 3) + "istance"
        } else if (k.endsWith(" dmg")) {
            k = k.substring(0, k.length - 3) + "damage"
        } else if (k.endsWith(" dmg percent")) {
            k = k.substring(0, k.length - 11) + "damage percent"
        } else if (k.endsWith(" exp")) {
            k = k.substring(0, k.length - 3) + "experience"
        } else if (k.startsWith("organ")) {
            k = "organ capacity"
        }
        return k
    }

    private fun resolveKeyword(keyword: String): List<DoubleModifier> = when (keyword) {
        "all resistance", "all res" -> ALL_RESISTANCE
        "elemental damage" -> ELEMENTAL_DAMAGE
        "passive damage" -> listOf(
            DoubleModifier.DAMAGE_AURA,
            DoubleModifier.THORNS,
        )
        "organ capacity" -> listOf(
            DoubleModifier.STOMACH_CAPACITY,
            DoubleModifier.LIVER_CAPACITY,
            DoubleModifier.SPLEEN_CAPACITY,
        )
        "init", "initiative" -> listOf(DoubleModifier.INITIATIVE)
        "hp", "maxhp" -> listOf(DoubleModifier.HP)
        "mp", "maxmp", "mana" -> listOf(DoubleModifier.MP)
        "da", "damage absorption" -> listOf(DoubleModifier.DAMAGE_ABSORPTION)
        "dr", "damage reduction" -> listOf(DoubleModifier.DAMAGE_REDUCTION)
        "item", "item drop" -> listOf(DoubleModifier.ITEMDROP)
        "meat", "meat drop" -> listOf(DoubleModifier.MEATDROP)
        "exp", "experience" -> listOf(DoubleModifier.EXPERIENCE)
        "ocrs" -> listOf(DoubleModifier.RANDOM_MONSTER_MODIFIERS)
        else -> {
            val fromGoal = MaximizeGoal.parseSingleModifier(keyword)
            if (fromGoal != null) listOf(fromGoal)
            else DoubleModifier.byTag(keyword)?.let { listOf(it) }.orEmpty()
        }
    }

    companion object {
        private val TERM_PATTERN =
            Regex("""^\s*(\+|-|)([\d.]*)\s*("(?:[^"\\]|\\.)*"|(?:[^-+,0-9]|(?<! )[-+0-9])+)\s*,?\s*""")

        private val ALL_RESISTANCE = listOf(
            DoubleModifier.COLD_RESISTANCE,
            DoubleModifier.HOT_RESISTANCE,
            DoubleModifier.SLEAZE_RESISTANCE,
            DoubleModifier.SPOOKY_RESISTANCE,
            DoubleModifier.STENCH_RESISTANCE,
        )

        private val ELEMENTAL_DAMAGE = listOf(
            DoubleModifier.COLD_DAMAGE,
            DoubleModifier.HOT_DAMAGE,
            DoubleModifier.SLEAZE_DAMAGE,
            DoubleModifier.SPOOKY_DAMAGE,
            DoubleModifier.STENCH_DAMAGE,
        )

        /** Desktop Evaluator.TIEBREAKER verbatim. */
        private const val TIEBREAKER =
            "1 familiar weight, 1 familiar experience, 1 initiative, 5 exp, 1 item, 1 meat, 0.1 DA 1000 max, 1 DR, 0.5 all res, -10 mana cost, 1.0 mus, 0.5 mys, 1.0 mox, 1.5 mainstat, 1 HP, 1 MP, 1 weapon damage, 1 ranged damage, 1 spell damage, 1 cold damage, 1 hot damage, 1 sleaze damage, 1 spooky damage, 1 stench damage, 1 cold spell damage, 1 hot spell damage, 1 sleaze spell damage, 1 spooky spell damage, 1 stench spell damage, -1 fumble, 1 HP regen max, 3 MP regen max, 1 critical hit percent, 0.1 food drop, 0.1 booze drop, 0.1 hat drop, 0.1 weapon drop, 0.1 offhand drop, 0.1 shirt drop, 0.1 pants drop, 0.1 accessory drop, 1 DB combat damage, 0.1 sixgun damage"

        private val tiebreakerInstance by lazy { Evaluator(TIEBREAKER) }

        fun tiebreaker(): Evaluator = tiebreakerInstance

        private fun resistanceValue(
            v: net.sourceforge.kolmafia.modifiers.ModifierValues,
            mod: DoubleModifier,
        ): Double {
            var value = v.get(mod)
            val (immunity, vulnerability) = when (mod) {
                DoubleModifier.COLD_RESISTANCE ->
                    BooleanModifier.COLD_IMMUNITY to BooleanModifier.COLD_VULNERABILITY
                DoubleModifier.HOT_RESISTANCE ->
                    BooleanModifier.HOT_IMMUNITY to BooleanModifier.HOT_VULNERABILITY
                DoubleModifier.SLEAZE_RESISTANCE ->
                    BooleanModifier.SLEAZE_IMMUNITY to BooleanModifier.SLEAZE_VULNERABILITY
                DoubleModifier.SPOOKY_RESISTANCE ->
                    BooleanModifier.SPOOKY_IMMUNITY to BooleanModifier.SPOOKY_VULNERABILITY
                DoubleModifier.STENCH_RESISTANCE ->
                    BooleanModifier.STENCH_IMMUNITY to BooleanModifier.STENCH_VULNERABILITY
                else -> return value
            }
            if (v.get(immunity)) value = 100.0
            else if (v.get(vulnerability)) value -= 100.0
            return value
        }

        private fun experienceValue(
            mods: CurrentModifiers,
            v: net.sourceforge.kolmafia.modifiers.ModifierValues,
        ): Double {
            val monsterLevel = v.get(DoubleModifier.MONSTER_LEVEL) *
                (1 + v.get(DoubleModifier.MONSTER_LEVEL_PERCENT) / 100.0)
            val zoneMl = max(monsterLevel, mods.characterLevel().toDouble())
            val baseExp = estimatedBaseExp(monsterLevel, zoneMl)
            val expPct = mods.primeStatExperiencePercent() / 100.0
            val exp = mods.primeStatExperience()
            return ((baseExp + exp) * (1 + expPct)) / 2.0
        }

        /** Desktop KoLCharacter.estimatedBaseExp — zone ML passed explicitly. */
        internal fun estimatedBaseExp(monsterLevel: Double, zoneMl: Double): Double {
            val baseStats = zoneMl / 4.0
            val bonusStats = if (monsterLevel > 0) monsterLevel / 3.0 else monsterLevel / 4.0
            return round((baseStats + bonusStats) * 100.0) / 100.0
        }

        internal fun detectOutfitName(equipment: Map<EquipmentSlot, String>): String? {
            for (outfit in OutfitDatabase.allOutfits()) {
                if (outfit.equipment.isEmpty()) continue
                if (OutfitManager.isWearingPieces(outfit.equipment, equipment)) {
                    return outfit.name
                }
            }
            return null
        }

        private const val FROSTY_BUTTON = "frosty button"

        internal fun resolveEquipName(query: String, gameDatabase: GameDatabase? = null): String? {
            if (query.isBlank()) return null
            ItemDatabase.getByName(query)?.name?.let { return it }
            gameDatabase?.item(query)?.name?.let { return it }
            return query
        }

        private fun pickPlumberTool(
            primeIndex: Int,
            accessibleCount: (Int) -> Int,
            gameDatabase: GameDatabase,
        ): String? {
            fun have(name: String): Boolean {
                val id = resolveEquipName(name, gameDatabase)?.let { gameDatabase.item(it)?.id }
                    ?: gameDatabase.item(name)?.id
                return id != null && accessibleCount(id) > 0
            }

            val heavyHammer = HEAVY_HAMMER
            val hammer = HAMMER
            val bonfireFlower = BONFIRE_FLOWER
            val fireFlower = FIRE_FLOWER
            val fancyBoots = FANCY_BOOTS
            val workBoots = WORK_BOOTS

            return when (primeIndex) {
                0 -> when {
                    have(heavyHammer) -> heavyHammer
                    have(hammer) -> hammer
                    else -> null
                }
                1 -> when {
                    have(bonfireFlower) -> bonfireFlower
                    have(fireFlower) -> fireFlower
                    else -> null
                }
                2 -> when {
                    have(fancyBoots) -> fancyBoots
                    have(workBoots) -> workBoots
                    else -> null
                }
                else -> when {
                    have(heavyHammer) -> heavyHammer
                    have(bonfireFlower) -> bonfireFlower
                    have(fancyBoots) -> fancyBoots
                    have(hammer) -> hammer
                    have(fireFlower) -> fireFlower
                    have(workBoots) -> workBoots
                    else -> null
                }
            }
        }

        private const val HEAVY_HAMMER = "heavy hammer"
        private const val HAMMER = "hammer"
        private const val BONFIRE_FLOWER = "bonfire flower"
        private const val FIRE_FLOWER = "[10462]fire flower"
        private const val FANCY_BOOTS = "fancy boots"
        private const val WORK_BOOTS = "work boots"
    }
}
