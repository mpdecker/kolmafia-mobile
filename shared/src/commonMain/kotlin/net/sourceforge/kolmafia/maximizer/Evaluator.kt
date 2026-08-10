package net.sourceforge.kolmafia.maximizer

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

            for (index in indices) {
                weights[index] = (weights[index] ?: 0.0) + weight
                lastIndex = index
            }
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
    }
}
