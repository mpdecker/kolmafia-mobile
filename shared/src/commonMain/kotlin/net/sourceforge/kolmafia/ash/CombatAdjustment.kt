package net.sourceforge.kolmafia.ash

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.MainStat
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.CombatDatabase
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.MonsterDefinition
import net.sourceforge.kolmafia.modifiers.BooleanModifier
import net.sourceforge.kolmafia.modifiers.CurrentModifiers
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.modifiers.ModifierExpression
import net.sourceforge.kolmafia.modifiers.ModifierValues
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.modifiers.StatNames

/**
 * Combat adjustment math ported from desktop [KoLCharacter] / [RuntimeLibrary].
 */
internal object CombatAdjustment {

    data class ScaleParams(val scale: Int, val cap: Int, val floor: Int)

    fun elementalResistanceByLevel(levels: Int, mystBonus: Boolean, isMystClass: Boolean): Double {
        val value = if (levels > 4) {
            90.0 - 50.0 * (5.0 / 6.0).pow(levels - 4)
        } else {
            levels * 10.0
        }
        return if (mystBonus && isMystClass) value + 5.0 else value
    }

    fun damageAbsorptionPercent(raw: Int): Double {
        val capped = min(1000, raw)
        if (capped == 0) return 0.0
        return (sqrt(capped / 10.0) - 1.0) * 10.0
    }

    fun monsterLevelAdjustment(
        modifiers: CurrentModifiers,
        character: CharacterState?,
        lastLocation: String,
    ): Int {
        val ml = modifiers.values.get(DoubleModifier.MONSTER_LEVEL).toInt()
        if (character?.isRaincore != true) return ml
        val water = AdventureDatabase.getByName(lastLocation)?.waterLevel ?: 0
        return ml + water * 10
    }

    fun weightAdjustment(modifiers: CurrentModifiers): Int =
        (modifiers.values.get(DoubleModifier.FAMILIAR_WEIGHT) +
            modifiers.values.get(DoubleModifier.HIDDEN_FAMILIAR_WEIGHT)).toInt()

    fun manaCostModifier(modifiers: CurrentModifiers, combat: Boolean): Int {
        var total = modifiers.values.get(DoubleModifier.MANA_COST) +
            modifiers.values.get(DoubleModifier.STACKABLE_MANA_COST)
        if (combat) total += modifiers.values.get(DoubleModifier.COMBAT_MANA_COST)
        return total.toInt()
    }

    fun combatRateModifier(modifiers: CurrentModifiers, lastLocation: String): Double {
        var rate = modifiers.values.get(DoubleModifier.COMBAT_RATE)
        val env = AdventureDatabase.getByName(lastLocation)?.environment
        if (env.equals("underwater", ignoreCase = true)) {
            rate += modifiers.values.get(DoubleModifier.UNDERWATER_COMBAT_RATE)
        }
        return rate
    }

    fun elementalResistanceLevels(modifiers: CurrentModifiers, element: String): Int {
        val mod = elementalResistanceModifier(element) ?: return 0
        return modifiers.values.get(mod).toInt()
    }

    fun elementalResistancePercent(
        modifiers: CurrentModifiers,
        element: String,
        character: CharacterState?,
    ): Double {
        val normalized = element.lowercase().trim()
        if (normalized.isEmpty() || normalized == "none") return 0.0
        val levels = elementalResistanceLevels(modifiers, normalized)
        val mystBonus = normalized != "slime"
        val isMyst = character?.mainStat == MainStat.MYSTICALITY
        return elementalResistanceByLevel(levels, mystBonus, isMyst)
    }

    fun expectedDamage(
        monster: MonsterDefinition?,
        character: CharacterState?,
        modifiers: CurrentModifiers,
        attackModifier: Int = 0,
        ml: Int = 0,
        expressionContext: ExpressionContext? = null,
    ): Int {
        if (monster == null) return 0
        val attack = monsterAttack(monster, ml, expressionContext) + attackModifier
        val defenseStat = character?.buffedMoxie ?: 0
        val da = modifiers.values.get(DoubleModifier.DAMAGE_ABSORPTION).toInt()
        val dr = modifiers.values.get(DoubleModifier.DAMAGE_REDUCTION).toInt()
        val damageAbsorb =
            1.0 - (sqrt(min(1000, da) / 10.0) - 1.0) / 10.0

        val baseValue: Int
        val elementAbsorb: Double
        if (monster.name.equals("ninja snowman assassin", ignoreCase = true)) {
            baseValue = max(0, attack - defenseStat) + 120
            val coldLevels = elementalResistanceLevels(modifiers, "cold")
            val modifiedRes = max(0, coldLevels - 5)
            val isMyst = character?.mainStat == MainStat.MYSTICALITY
            elementAbsorb =
                1.0 - elementalResistanceByLevel(modifiedRes, mystBonus = true, isMyst) / 100.0
        } else {
            baseValue = max(0, attack - defenseStat) + attack / 4 - dr
            val elemPct = elementalResistancePercent(modifiers, monster.attackElement, character)
            elementAbsorb = 1.0 - elemPct / 100.0
        }
        return ceil(baseValue * damageAbsorb * elementAbsorb).toInt()
    }

    /** Desktop [KoLCharacter.getInitiativeAdjustment] — penalty constrained non-positive. */
    fun initiativeModifier(values: ModifierValues): Double =
        values.get(DoubleModifier.INITIATIVE) +
            min(values.get(DoubleModifier.INITIATIVE_PENALTY), 0.0)

    fun initiativeModifier(modifiers: CurrentModifiers): Double =
        initiativeModifier(modifiers.values)

    /** Desktop [KoLCharacter.getMeatDropPercentAdjustment]. */
    fun meatDropModifier(values: ModifierValues): Double =
        values.get(DoubleModifier.MEATDROP) +
            min(values.get(DoubleModifier.MEATDROP_PENALTY), 0.0)

    fun meatDropModifier(modifiers: CurrentModifiers): Double =
        meatDropModifier(modifiers.values)

    /** Desktop [KoLCharacter.getItemDropPercentAdjustment] — no GEARDROP. */
    fun itemDropModifier(values: ModifierValues): Double =
        values.get(DoubleModifier.ITEMDROP) +
            min(values.get(DoubleModifier.ITEMDROP_PENALTY), 0.0)

    fun itemDropModifier(modifiers: CurrentModifiers): Double =
        itemDropModifier(modifiers.values)

    /** Desktop [KoLCharacter.getExperienceAdjustment] — prime-stat fixed XP only. */
    fun experienceBonus(values: ModifierValues, character: CharacterState?): Double {
        val mod = when (character?.mainStat) {
            MainStat.MUSCLE -> DoubleModifier.MUS_EXPERIENCE
            MainStat.MYSTICALITY -> DoubleModifier.MYS_EXPERIENCE
            MainStat.MOXIE -> DoubleModifier.MOX_EXPERIENCE
            null -> return 0.0
        }
        return values.get(mod)
    }

    fun experienceBonus(modifiers: CurrentModifiers, character: CharacterState?): Double =
        experienceBonus(modifiers.values, character)

    /**
     * Desktop-lite ML adjustment for Atk/Def/HP: unknown/zero base stays 0;
     * otherwise [max(1, base + ml)]. Beeosity deferred.
     */
    fun monsterStatWithMl(base: Int, ml: Int): Int {
        if (base == 0) return 0
        return max(1, base + ml)
    }

    /**
     * Desktop [MonsterData.ML]: global ML × evaluate(MLMult, default=1).
     * Absent MLMult → identity; MLMult:0 → zero ML contribution.
     */
    fun effectiveMonsterLevel(
        monster: MonsterDefinition?,
        ml: Int,
        expressionContext: ExpressionContext? = null,
    ): Int {
        if (monster == null || !monster.hasMlMult) return ml
        val mult = monster.mlMultExpression?.let {
            ModifierExpression(it).evaluate(expressionContext ?: ExpressionContext.EMPTY).toInt()
        } ?: monster.mlMult
        return ml * mult
    }

    /**
     * Resolve Scale/Cap/Floor: expression eval when present, else numeric fields.
     */
    fun resolveScaleParams(
        monster: MonsterDefinition,
        expressionContext: ExpressionContext? = null,
    ): ScaleParams {
        val ctx = expressionContext ?: ExpressionContext.EMPTY
        val scale = monster.scaleExpression?.let {
            ModifierExpression(it).evaluate(ctx).toInt()
        } ?: monster.scale
        val cap = monster.capExpression?.let {
            ModifierExpression(it).evaluate(ctx).toInt()
        } ?: monster.cap
        val floor = monster.floorExpression?.let {
            ModifierExpression(it).evaluate(ctx).toInt()
        } ?: monster.floor
        return ScaleParams(scale = scale, cap = cap, floor = floor)
    }

    /**
     * Scale path for attack (beeosity = 1):
     * max(1, max(floor, min(cap, buffedMoxie + scale) + max(ml, 0))).
     * [ml] should already be [effectiveMonsterLevel] when called from [monsterAttack].
     */
    fun scaledAttack(
        monster: MonsterDefinition,
        ml: Int,
        expressionContext: ExpressionContext? = null,
    ): Int {
        val buffedMoxie = expressionContext?.buffedMoxie ?: 0
        val p = resolveScaleParams(monster, expressionContext)
        var attack = min(buffedMoxie + p.scale, p.cap)
        attack += max(ml, 0)
        attack = max(attack, p.floor)
        return max(1, attack)
    }

    /**
     * Scale path for defense (beeosity = 1):
     * max(1, max(floor, min(cap, buffedMuscle + scale) + max(ml, 0))).
     * REDUCE_ENEMY_DEFENSE applied by [monsterDefense].
     */
    fun scaledDefense(
        monster: MonsterDefinition,
        ml: Int,
        expressionContext: ExpressionContext? = null,
    ): Int {
        val buffedMuscle = expressionContext?.buffedMuscle ?: 0
        val p = resolveScaleParams(monster, expressionContext)
        var defense = min(buffedMuscle + p.scale, p.cap)
        defense += max(ml, 0)
        defense = max(defense, p.floor)
        return max(1, defense)
    }

    /**
     * Scale path for HP (beeosity = 1):
     * max(1, max(floor*0.75, floor((min(cap, buffedMuscle + scale) + max(ml, 0)) * 0.75))).
     */
    fun scaledHp(
        monster: MonsterDefinition,
        ml: Int,
        expressionContext: ExpressionContext? = null,
    ): Int {
        val buffedMuscle = expressionContext?.buffedMuscle ?: 0
        val p = resolveScaleParams(monster, expressionContext)
        var hp = min(buffedMuscle + p.scale, p.cap)
        hp = floor((hp + max(ml, 0)) * 0.75).toInt()
        val floorAdj = (p.floor * 0.75).toInt()
        hp = max(hp, floorAdj)
        return max(1, hp)
    }

    /**
     * Desktop [MonsterData.getRawAttack]: pre-ML.
     * Scale (no Atk:) → buffed moxie + scale, cap/floor, no ML; missing → −1;
     * else max(1, [resolveBaseAttack]).
     */
    fun monsterRawAttack(
        monster: MonsterDefinition?,
        expressionContext: ExpressionContext? = null,
    ): Int {
        if (monster == null) return 0
        if (!monster.hasAttack) {
            if (monster.isScaling) return rawScaledAttack(monster, expressionContext)
            return -1
        }
        return max(1, resolveBaseAttack(monster, expressionContext))
    }

    /**
     * Desktop [MonsterData.getRawDefense]: pre-ML, no REDUCE_ENEMY_DEFENSE.
     */
    fun monsterRawDefense(
        monster: MonsterDefinition?,
        expressionContext: ExpressionContext? = null,
    ): Int {
        if (monster == null) return 0
        if (!monster.hasDefense) {
            if (monster.isScaling) return rawScaledDefense(monster, expressionContext)
            return -1
        }
        return max(1, resolveBaseDefense(monster, expressionContext))
    }

    /**
     * Desktop [MonsterData.getRawHP]: pre-ML.
     * Scale path applies floor/cap before ×0.75 (unlike [scaledHp]).
     */
    fun monsterRawHp(
        monster: MonsterDefinition?,
        expressionContext: ExpressionContext? = null,
    ): Int {
        if (monster == null) return 0
        if (!monster.hasHp) {
            if (monster.isScaling) return rawScaledHp(monster, expressionContext)
            return -1
        }
        return max(1, resolveBaseHp(monster, expressionContext))
    }

    /**
     * Desktop [MonsterData.getRawInitiative]: evaluate(init, 0); no initPenalty.
     * Missing Init: → 0 (desktop evaluate null default).
     */
    fun monsterRawInitiative(
        monster: MonsterDefinition?,
        expressionContext: ExpressionContext? = null,
    ): Int {
        if (monster == null) return 0
        if (!monster.hasInitiative) return 0
        return resolveBaseInitiative(monster, expressionContext)
    }

    /** Raw Scale Atk: `val > cap ? cap : max(val, floor)`, no ML. */
    fun rawScaledAttack(
        monster: MonsterDefinition,
        expressionContext: ExpressionContext? = null,
    ): Int {
        val buffedMoxie = expressionContext?.buffedMoxie ?: 0
        val p = resolveScaleParams(monster, expressionContext)
        var attack = buffedMoxie + p.scale
        attack = if (attack > p.cap) p.cap else max(attack, p.floor)
        return max(1, attack)
    }

    /** Raw Scale Def: same order as [rawScaledAttack], muscle-based. */
    fun rawScaledDefense(
        monster: MonsterDefinition,
        expressionContext: ExpressionContext? = null,
    ): Int {
        val buffedMuscle = expressionContext?.buffedMuscle ?: 0
        val p = resolveScaleParams(monster, expressionContext)
        var defense = buffedMuscle + p.scale
        defense = if (defense > p.cap) p.cap else max(defense, p.floor)
        return max(1, defense)
    }

    /**
     * Raw Scale HP: floor/cap on (muscle+scale), then `floor(max(1, hp * 0.75))`.
     */
    fun rawScaledHp(
        monster: MonsterDefinition,
        expressionContext: ExpressionContext? = null,
    ): Int {
        val buffedMuscle = expressionContext?.buffedMuscle ?: 0
        val p = resolveScaleParams(monster, expressionContext)
        var hp = buffedMuscle + p.scale
        hp = if (hp > p.cap) p.cap else max(hp, p.floor)
        return floor(max(1.0, hp * 0.75)).toInt()
    }

    /**
     * Desktop-lite [MonsterData.getExperience] (beeosity = 1).
     * Scale-implied / default attack-based / explicit Exp: [expr] or numeric.
     * [ml] is global ML; [effectiveMonsterLevel] applied for Scale/default formulas.
     */
    fun monsterExperience(
        monster: MonsterDefinition?,
        ml: Int,
        expressionContext: ExpressionContext? = null,
        xpMultiplier: Int = 1,
    ): Double {
        if (monster == null) return 0.0
        val mult = max(1, xpMultiplier).toDouble()
        val ctx = expressionContext ?: ExpressionContext.EMPTY
        val effectiveMl = effectiveMonsterLevel(monster, ml, expressionContext)

        if (monster.hasExperience || monster.experienceExpression != null) {
            val raw = monster.experienceExpression?.let {
                ModifierExpression(it).evaluate(ctx)
            } ?: monster.experience.toDouble()
            return raw * mult / 2.0
        }

        if (monster.isScaling) {
            val mainstat = maxOf(ctx.buffedMuscle, ctx.buffedMysticality, ctx.buffedMoxie)
            val p = resolveScaleParams(monster, expressionContext)
            var exp = mainstat + p.scale
            exp = if (exp > p.cap) p.cap else max(exp, p.floor)
            val mlTerm = max(effectiveMl, 0).toDouble()
            return max(1.0, (exp / 8.0 + mlTerm / 6.0) * mult)
        }

        // Pass raw [ml] so monsterAttack applies MLMult once.
        val attack = monsterAttack(monster, ml, expressionContext).toDouble()
        return max((attack - effectiveMl) / 8.0 + effectiveMl / 6.0, 0.0) * mult
    }

    /**
     * Evaluate base attack: numeric Atk, or Atk: [expr] via [expressionContext].
     * Expression path returns max(1, eval) — no outer ML (desktop MonsterExpression).
     */
    fun resolveBaseAttack(
        monster: MonsterDefinition?,
        expressionContext: ExpressionContext? = null,
    ): Int {
        if (monster == null) return 0
        val expr = monster.attackExpression
        if (expr != null) {
            val raw = ModifierExpression(expr).evaluate(expressionContext ?: ExpressionContext.EMPTY)
            return max(1, raw.toInt())
        }
        return monster.attack
    }

    /**
     * Desktop-lite [MonsterData.getAttack]: expression → resolve only;
     * Scale (no Atk:) → [scaledAttack]; integer → [monsterStatWithMl].
     * [ml] is global ML; converted via [effectiveMonsterLevel] before Scale / integer paths.
     */
    fun monsterAttack(
        monster: MonsterDefinition?,
        ml: Int,
        expressionContext: ExpressionContext? = null,
    ): Int {
        if (monster?.attackExpression != null) {
            return resolveBaseAttack(monster, expressionContext)
        }
        val effectiveMl = effectiveMonsterLevel(monster, ml, expressionContext)
        if (monster != null && monster.isScaling && !monster.hasAttack) {
            return scaledAttack(monster, effectiveMl, expressionContext)
        }
        return monsterStatWithMl(monster?.attack ?: 0, effectiveMl)
    }

    /**
     * Evaluate base defense: numeric Def, or Def: [expr] via [expressionContext].
     * Expression path returns max(1, eval) — no outer ML (desktop MonsterExpression).
     */
    fun resolveBaseDefense(
        monster: MonsterDefinition?,
        expressionContext: ExpressionContext? = null,
    ): Int {
        if (monster == null) return 0
        val expr = monster.defenseExpression
        if (expr != null) {
            val raw = ModifierExpression(expr).evaluate(expressionContext ?: ExpressionContext.EMPTY)
            return max(1, raw.toInt())
        }
        return monster.defense
    }

    /**
     * Desktop-lite [MonsterData.getDefense]: expression → resolve only;
     * Scale (no Def:) → [scaledDefense]; integer → [monsterStatWithMl];
     * then [applyEnemyDefenseReduce] for Reduce Enemy Defense %.
     * [ml] is global ML; converted via [effectiveMonsterLevel] before Scale / integer paths.
     */
    fun monsterDefense(
        monster: MonsterDefinition?,
        ml: Int,
        expressionContext: ExpressionContext? = null,
        reduceEnemyDefensePercent: Double = 0.0,
    ): Int {
        val effectiveMl = effectiveMonsterLevel(monster, ml, expressionContext)
        val raw = when {
            monster == null -> 0
            monster.defenseExpression != null -> resolveBaseDefense(monster, expressionContext)
            monster.isScaling && !monster.hasDefense -> scaledDefense(monster, effectiveMl, expressionContext)
            else -> monsterStatWithMl(monster.defense, effectiveMl)
        }
        return applyEnemyDefenseReduce(raw, reduceEnemyDefensePercent)
    }

    /** Percent from [DoubleModifier.REDUCE_ENEMY_DEFENSE] (0–100 scale). */
    fun reduceEnemyDefensePercent(modifiers: CurrentModifiers?): Double =
        modifiers?.values?.get(DoubleModifier.REDUCE_ENEMY_DEFENSE) ?: 0.0

    /**
     * Desktop: `floor(max(1, defense * (1 - reduce/100)))`.
     * Raw 0 (unknown / null) stays 0.
     */
    fun applyEnemyDefenseReduce(rawDefense: Int, reducePercent: Double): Int {
        if (rawDefense <= 0) return rawDefense
        return floor(max(1.0, rawDefense * (1.0 - reducePercent / 100.0))).toInt()
    }

    /**
     * Evaluate base HP: numeric HP, or HP: [expr] via [expressionContext].
     * Expression path returns max(1, eval) — no outer ML (desktop MonsterExpression).
     * Note: `HP` token in expressions is character max HP, not monster HP.
     */
    fun resolveBaseHp(
        monster: MonsterDefinition?,
        expressionContext: ExpressionContext? = null,
    ): Int {
        if (monster == null) return 0
        val expr = monster.hpExpression
        if (expr != null) {
            val raw = ModifierExpression(expr).evaluate(expressionContext ?: ExpressionContext.EMPTY)
            return max(1, raw.toInt())
        }
        return monster.hp
    }

    /**
     * Desktop-lite [MonsterData.getHP]: expression → resolve only;
     * Scale (no HP:) → [scaledHp]; integer → [monsterStatWithMl]. Beeosity deferred.
     * [ml] is global ML; converted via [effectiveMonsterLevel] before Scale / integer paths.
     */
    fun monsterHp(
        monster: MonsterDefinition?,
        ml: Int,
        expressionContext: ExpressionContext? = null,
    ): Int {
        if (monster?.hpExpression != null) {
            return resolveBaseHp(monster, expressionContext)
        }
        val effectiveMl = effectiveMonsterLevel(monster, ml, expressionContext)
        if (monster != null && monster.isScaling && !monster.hasHp) {
            return scaledHp(monster, effectiveMl, expressionContext)
        }
        return monsterStatWithMl(monster?.hp ?: 0, effectiveMl)
    }

    /**
     * Evaluate base initiative: numeric Init, or Init: [expr] via [expressionContext].
     * Missing expression context for expr Init falls back to ExpressionContext.EMPTY (prefs 0, KW/KV/KC=1).
     */
    fun resolveBaseInitiative(
        monster: MonsterDefinition?,
        expressionContext: ExpressionContext? = null,
    ): Int {
        if (monster == null) return 0
        val expr = monster.initiativeExpression
        if (expr != null) {
            return ModifierExpression(expr).evaluate(expressionContext ?: ExpressionContext.EMPTY)
                .toInt()
        }
        return monster.initiative
    }

    fun monsterInitiative(
        monster: MonsterDefinition?,
        expressionContext: ExpressionContext? = null,
    ): Int = resolveBaseInitiative(monster, expressionContext)

    fun monsterPhylum(monster: MonsterDefinition?): String =
        monster?.phylum.orEmpty()

    fun monsterDefenseElement(monster: MonsterDefinition?): String =
        monster?.defenseElement.orEmpty()

    fun monsterMinSprinkles(
        monster: MonsterDefinition?,
        expressionContext: ExpressionContext?,
    ): Int {
        if (monster == null) return 0
        return evaluateResistanceValue(
            monster.minSprinkles,
            monster.minSprinklesExpression,
            expressionContext,
        )
    }

    fun monsterMaxSprinkles(
        monster: MonsterDefinition?,
        expressionContext: ExpressionContext?,
    ): Int {
        if (monster == null) return 0
        return evaluateResistanceValue(
            monster.maxSprinkles,
            monster.maxSprinklesExpression,
            expressionContext,
        )
    }

    private fun evaluateResistanceValue(
        numeric: Int,
        expression: String?,
        expressionContext: ExpressionContext?,
    ): Int {
        if (expression != null) {
            return ModifierExpression(expression)
                .evaluate(expressionContext ?: ExpressionContext.EMPTY)
                .toInt()
        }
        return numeric
    }

    /**
     * Desktop [MonsterData.handleMonsterLevel] phys-res boost:
     * `min(floor(ML() / 2.5), 50)`; Scale monsters clamp negative boost to 0.
     * Uses [effectiveMonsterLevel] (AshP58 MLMult).
     */
    fun mlPhysicalResistanceBoost(
        monster: MonsterDefinition?,
        ml: Int,
        expressionContext: ExpressionContext? = null,
    ): Int {
        val effectiveMl = effectiveMonsterLevel(monster, ml, expressionContext)
        var boost = min(floor(effectiveMl / 2.5).toInt(), 50)
        if (monster?.isScaling == true && boost < 0) boost = 0
        return boost
    }

    /**
     * Desktop [MonsterData.getPhysicalResistance] plus fight-time
     * [MonsterData.handleMonsterLevel] ML boost when [ml] ≠ 0.
     */
    fun monsterPhysicalResistance(
        monster: MonsterDefinition?,
        expressionContext: ExpressionContext? = null,
        ml: Int = 0,
    ): Int {
        if (monster == null) return 0
        val base = evaluateResistanceValue(
            monster.physicalResistance,
            monster.physicalResistanceExpression,
            expressionContext,
        )
        val boost = mlPhysicalResistanceBoost(monster, ml, expressionContext)
        if (boost <= base) return base
        return if (base == 0) boost else max(boost, base)
    }

    /** Desktop [MonsterData.getElementalResistance]. */
    fun monsterElementalResistance(
        monster: MonsterDefinition?,
        expressionContext: ExpressionContext? = null,
    ): Int {
        if (monster == null) return 0
        return evaluateResistanceValue(
            monster.elementalResistance,
            monster.elementalResistanceExpression,
            expressionContext,
        )
    }

    /**
     * Desktop [MonsterData.getResistance]: per-element attr, else [monsterElementalResistance].
     * [element] is hot/cold/stench/spooky/sleaze (case-insensitive).
     */
    fun monsterElementResistance(
        monster: MonsterDefinition?,
        element: String,
        expressionContext: ExpressionContext? = null,
    ): Int {
        if (monster == null) return 0
        val (numeric, expression) = when (element.lowercase()) {
            "hot" -> monster.hotResistance to monster.hotResistanceExpression
            "cold" -> monster.coldResistance to monster.coldResistanceExpression
            "stench" -> monster.stenchResistance to monster.stenchResistanceExpression
            "spooky" -> monster.spookyResistance to monster.spookyResistanceExpression
            "sleaze" -> monster.sleazeResistance to monster.sleazeResistanceExpression
            else -> return monsterElementalResistance(monster, expressionContext)
        }
        val specific = evaluateResistanceValue(numeric, expression, expressionContext)
        if (specific == 0) return monsterElementalResistance(monster, expressionContext)
        return specific
    }

    /** Desktop [MonsterData.initPenalty]. */
    fun initPenalty(monsterLevel: Int): Int =
        when {
            monsterLevel <= 20 -> 0
            monsterLevel <= 40 -> monsterLevel - 20
            monsterLevel <= 60 -> 20 + 2 * (monsterLevel - 40)
            monsterLevel <= 80 -> 60 + 3 * (monsterLevel - 60)
            monsterLevel <= 100 -> 120 + 4 * (monsterLevel - 80)
            else -> 200 + 5 * (monsterLevel - 100)
        }

    /** Desktop [MonsterData.getInitiative(ml)] — ±10000 / -1 sentinels unchanged. */
    fun monsterInitiativeWithMl(
        monster: MonsterDefinition?,
        ml: Int,
        expressionContext: ExpressionContext? = null,
    ): Int {
        val base = resolveBaseInitiative(monster, expressionContext)
        if (base == -1 || base == 10000 || base == -10000) return base
        return base + initPenalty(ml)
    }

    /**
     * Desktop-lite [MonsterData.getJumpChance].
     * [initMl] feeds initPenalty; [attackMl] feeds attack (desktop quirk: overload ml
     * only affects initiative, while attack uses current character ML).
     * Missing Init: → −1. Overclocked +200 vs Source Agent.
     */
    fun jumpChance(
        monster: MonsterDefinition?,
        initBonus: Int,
        initMl: Int,
        attackMl: Int,
        baseMainstat: Int,
        hasOverclocked: Boolean = false,
        expressionContext: ExpressionContext? = null,
    ): Int {
        if (monster == null) return 0
        if (!monster.hasInitiative) return -1
        val monsterInit = monsterInitiativeWithMl(monster, initMl, expressionContext)
        if (monsterInit == 10000) return 0
        if (monsterInit == -10000) return 100
        var charInit = initBonus
        if (hasOverclocked && monster.name.contains("Source Agent")) {
            charInit += 200
        }
        val attack = monsterAttack(monster, attackMl, expressionContext)
        val jump = 100 - monsterInit + charInit + max(0, baseMainstat - attack)
        return jump.coerceIn(0, 100)
    }

    /**
     * Desktop [AreaCombatData.getJumpChance]: min jump chance over weight &gt; 0
     * monsters in the zone; unknown/empty zone → 0.
     */
    fun locationJumpChance(
        locationName: String,
        initBonus: Int,
        initMl: Int,
        attackMl: Int,
        baseMainstat: Int,
        hasOverclocked: Boolean = false,
        expressionContext: ExpressionContext? = null,
        resolveMonster: (String) -> MonsterDefinition?,
    ): Int {
        val data = CombatDatabase.getByLocation(locationName) ?: return 0
        var minJump: Int? = null
        for (mw in data.monsters) {
            if (mw.weight <= 0) continue
            val chance = jumpChance(
                resolveMonster(mw.name),
                initBonus,
                initMl,
                attackMl,
                baseMainstat,
                hasOverclocked,
                expressionContext,
            )
            minJump = if (minJump == null) chance else min(minJump, chance)
        }
        return minJump ?: 0
    }

    /** Desktop [AreaCombatData.hitPercent]. */
    fun hitPercent(attack: Int, defense: Int): Double {
        val percent = 100.0 * (attack - defense) / 18.0 + 50.0
        return percent.coerceIn(0.0, 100.0)
    }

    /**
     * Desktop-lite [EquipmentManager.getHitStatType] — Mox-req weapon → moxie, else muscle.
     * Knife / Fourth Saber / skill edges deferred.
     */
    fun hitStatKind(weaponName: String?): HitStatKind {
        if (weaponName.isNullOrBlank()) return HitStatKind.MUSCLE
        val req = EquipmentDatabase.getByName(weaponName)?.statRequirement
            ?: return HitStatKind.MUSCLE
        return if (req.startsWith("Mox", ignoreCase = true)) HitStatKind.MOXIE else HitStatKind.MUSCLE
    }

    fun buffedHitStat(
        character: CharacterState?,
        modifiers: CurrentModifiers,
        weaponName: String?,
    ): Int {
        if (modifiers.values.get(BooleanModifier.ATTACKS_CANT_MISS)) return Int.MAX_VALUE
        val mus = resolveBuffedMuscle(character, modifiers)
        val mox = resolveBuffedMoxie(character, modifiers)
        return when (hitStatKind(weaponName)) {
            HitStatKind.MOXIE -> mox
            HitStatKind.MUSCLE -> mus
        }
    }

    fun currentHitStatName(weaponName: String?): String =
        when (hitStatKind(weaponName)) {
            HitStatKind.MOXIE -> StatNames.MOXIE
            HitStatKind.MUSCLE -> StatNames.MUSCLE
        }

    /** Desktop [MonsterData.willUsuallyDodge] with offenseModifier (ASH uses 0). */
    fun willUsuallyDodge(
        monster: MonsterDefinition?,
        buffedMoxie: Int,
        ml: Int,
        offenseModifier: Int = 0,
        expressionContext: ExpressionContext? = null,
    ): Boolean {
        if (monster == null) return false
        val attack = monsterAttack(monster, ml, expressionContext)
        return buffedMoxie - (attack + offenseModifier) - 6 > 0
    }

    /** Desktop [MonsterData.willUsuallyMiss] with defenseModifier (ASH uses 0). */
    fun willUsuallyMiss(
        monster: MonsterDefinition?,
        hitStat: Int,
        ml: Int,
        defenseModifier: Int = 0,
        expressionContext: ExpressionContext? = null,
        reduceEnemyDefensePercent: Double = 0.0,
    ): Boolean {
        if (monster == null) return false
        val defense = monsterDefense(monster, ml, expressionContext, reduceEnemyDefensePercent)
        return hitPercent(hitStat - defenseModifier, defense) <= 50.0
    }

    private fun resolveBuffedMuscle(character: CharacterState?, modifiers: CurrentModifiers): Int {
        val fromMods = modifiers.buffedMuscle()
        if (fromMods != 0) return fromMods
        return character?.buffedMusc ?: 0
    }

    private fun resolveBuffedMoxie(character: CharacterState?, modifiers: CurrentModifiers): Int {
        val fromMods = modifiers.buffedMoxie()
        if (fromMods != 0) return fromMods
        return character?.buffedMoxie ?: 0
    }
}

/** Desktop-lite hit-stat kind (ranged/Mox → moxie, else muscle). */
internal enum class HitStatKind { MUSCLE, MOXIE }

internal fun GameRuntimeLibrary.lastLocationName(): String =
    preferences?.getString(Preferences.LAST_LOCATION, "") ?: ""

internal fun GameRuntimeLibrary.resolveMonsterDefinition(name: String): MonsterDefinition? {
    if (name.isBlank()) return null
    return gameDatabase?.monster(name)
        ?: net.sourceforge.kolmafia.data.MonsterDatabase.getByName(name)
}
