package net.sourceforge.kolmafia.ash

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.MainStat
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.MonsterDefinition
import net.sourceforge.kolmafia.modifiers.CurrentModifiers
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ModifierValues
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Combat adjustment math ported from desktop [KoLCharacter] / [RuntimeLibrary].
 */
internal object CombatAdjustment {

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
    ): Int {
        if (monster == null) return 0
        val attack = monster.attack + attackModifier
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
     * otherwise [max(1, base + ml)]. Full scaling/beeosity deferred.
     */
    fun monsterStatWithMl(base: Int, ml: Int): Int {
        if (base == 0) return 0
        return max(1, base + ml)
    }

    fun monsterAttack(monster: MonsterDefinition?, ml: Int): Int =
        monsterStatWithMl(monster?.attack ?: 0, ml)

    fun monsterDefense(monster: MonsterDefinition?, ml: Int): Int =
        monsterStatWithMl(monster?.defense ?: 0, ml)

    fun monsterHp(monster: MonsterDefinition?, ml: Int): Int =
        monsterStatWithMl(monster?.hp ?: 0, ml)

    fun monsterInitiative(monster: MonsterDefinition?): Int =
        monster?.initiative ?: 0

    fun monsterPhylum(monster: MonsterDefinition?): String =
        monster?.phylum.orEmpty()

    fun monsterDefenseElement(monster: MonsterDefinition?): String =
        monster?.defenseElement.orEmpty()

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
    fun monsterInitiativeWithMl(monster: MonsterDefinition?, ml: Int): Int {
        val base = monster?.initiative ?: 0
        if (base == -1 || base == 10000 || base == -10000) return base
        return base + initPenalty(ml)
    }

    /**
     * Desktop-lite [MonsterData.getJumpChance].
     * [initMl] feeds initPenalty; [attackMl] feeds attack (desktop quirk: overload ml
     * only affects initiative, while attack uses current character ML).
     */
    fun jumpChance(
        monster: MonsterDefinition?,
        initBonus: Int,
        initMl: Int,
        attackMl: Int,
        baseMainstat: Int,
    ): Int {
        if (monster == null) return 0
        val monsterInit = monsterInitiativeWithMl(monster, initMl)
        if (monsterInit == 10000) return 0
        if (monsterInit == -10000) return 100
        val attack = monsterAttack(monster, attackMl)
        val jump = 100 - monsterInit + initBonus + max(0, baseMainstat - attack)
        return jump.coerceIn(0, 100)
    }
}

internal fun GameRuntimeLibrary.lastLocationName(): String =
    preferences?.getString(Preferences.LAST_LOCATION, "") ?: ""

internal fun GameRuntimeLibrary.resolveMonsterDefinition(name: String): MonsterDefinition? {
    if (name.isBlank()) return null
    return gameDatabase?.monster(name)
        ?: net.sourceforge.kolmafia.data.MonsterDatabase.getByName(name)
}
