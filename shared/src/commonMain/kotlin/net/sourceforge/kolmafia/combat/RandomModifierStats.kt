package net.sourceforge.kolmafia.combat

import net.sourceforge.kolmafia.ash.CombatAdjustment
import net.sourceforge.kolmafia.data.MonsterDefinition
import net.sourceforge.kolmafia.data.primaryAttackElement
import net.sourceforge.kolmafia.modifiers.ExpressionContext

/**
 * Applies desktop [MonsterData.handleRandomModifiers] stat mutations to a fight instance.
 */
object RandomModifierStats {

    fun apply(
        template: MonsterDefinition,
        modifiers: List<String>,
        expressionContext: ExpressionContext?,
    ): MonsterDefinition {
        if (modifiers.isEmpty()) return template
        var monster = template.copy(randomModifiers = modifiers)
        for (modifier in modifiers) {
            monster = applyModifier(monster, modifier, expressionContext)
        }
        return monster
    }

    private fun applyModifier(
        monster: MonsterDefinition,
        modifier: String,
        ctx: ExpressionContext?,
    ): MonsterDefinition {
        val rawHp = rawHp(monster, ctx)
        val rawAtk = rawAtk(monster, ctx)
        val rawDef = rawDef(monster, ctx)
        return when (modifier) {
            "askew" -> withNumericStats(monster, attack = rawAtk * 11 / 10)
            "bouncing" -> withNumericStats(monster, attack = rawAtk * 3 / 2)
            "broke" -> monster.copy(meatDrop = 5)
            "cloned", "huge" -> withNumericStats(
                monster,
                hp = rawHp * 2,
                attack = rawAtk * 2,
                defense = rawDef * 2,
            )
            "dancin'", "floating" -> withNumericStats(monster, defense = rawDef * 3 / 2)
            "fragile" -> withNumericStats(monster, hp = 1)
            "frozen", "ice-cold", "cold-blooded", "chilling" -> setAttackElement(monster, "cold")
            "ghostly" -> if (effectivePhysicalResistance(monster, ctx) == 0) {
                monster.copy(physicalResistance = 90, physicalResistanceExpression = null)
            } else {
                monster
            }
            "left-handed" -> monster.copy(attack = monster.defense, defense = monster.attack)
            "red-hot", "hot-blooded", "steamy" -> setAttackElement(monster, "hot")
            "short" -> withNumericStats(monster, hp = rawHp / 2, defense = rawDef * 2)
            "skinny" -> withNumericStats(monster, hp = rawHp / 2, defense = rawDef / 2)
            "sleazy", "slimy", "sweaty" -> setAttackElement(monster, "sleaze")
            "solid gold" -> monster.copy(meatDrop = 1000)
            "spooky", "carrion-eating", "mist-shrouded" -> setAttackElement(monster, "spooky")
            "stinky", "swamp", "foul-smelling" -> setAttackElement(monster, "stench")
            "throbbing" -> withNumericStats(monster, hp = rawHp * 2)
            "tiny" -> withNumericStats(
                monster,
                hp = rawHp / 10,
                attack = rawAtk / 10,
                defense = rawDef / 10,
            )
            "turgid" -> withNumericStats(monster, hp = rawHp * 5)
            "unlucky" -> withNumericStats(monster, hp = 13, attack = 13, defense = 13)
            "mutant" -> withNumericStats(
                monster,
                hp = rawHp * 6 / 5,
                attack = rawAtk * 6 / 5,
                defense = rawDef * 6 / 5,
            )
            "Mr. mask", "Bonerdagon mask" -> if (!monster.isScaling) {
                withNumericStats(
                    monster,
                    hp = rawHp * 2,
                    attack = rawAtk * 2,
                    defense = rawDef * 2,
                )
            } else {
                monster
            }
            "ninja mask" -> withNumericStats(monster, initiative = 10000)
            "opera mask" -> if (!monster.isScaling) {
                withNumericStats(monster, attack = rawAtk * 2)
            } else {
                monster
            }
            "bandit mask" -> if (!monster.isScaling) {
                withNumericStats(monster, defense = rawDef * 4)
            } else {
                monster
            }
            "fencing mask" -> {
                var m = monster
                if (effectivePhysicalResistance(m, ctx) == 0) {
                    m = m.copy(physicalResistance = 90, physicalResistanceExpression = null)
                }
                if (effectiveElementalResistance(m, ctx) == 0) {
                    m = m.copy(elementalResistance = 90, elementalResistanceExpression = null)
                }
                m
            }
            "Naughty Sorceress mask" -> if (!monster.isScaling) {
                withNumericStats(
                    monster,
                    hp = rawHp * 3,
                    attack = rawAtk * 3,
                    defense = rawDef * 3,
                )
            } else {
                monster
            }
            "chicken" -> withNumericStats(monster, hp = 1, attack = 1, defense = 1)
            "ghostasaurus" -> monster.copy(physicalResistance = 100, physicalResistanceExpression = null)
            "terrycloth turban" -> withNumericStats(monster, hp = rawHp * 5 / 4)
            "extra-tight skullcap" -> withNumericStats(monster, attack = rawAtk * 5 / 4)
            "tinfoil hat" -> if (effectiveElementalResistance(monster, ctx) == 0) {
                monster.copy(elementalResistance = 20, elementalResistanceExpression = null)
            } else {
                monster
            }
            "construction hardhat" -> if (effectivePhysicalResistance(monster, ctx) == 0) {
                monster.copy(physicalResistance = 20, physicalResistanceExpression = null)
            } else {
                monster
            }
            else -> monster
        }
    }

    private fun rawHp(monster: MonsterDefinition, ctx: ExpressionContext?): Int {
        val raw = CombatAdjustment.monsterRawHp(monster, ctx)
        return if (raw > 0) raw else maxOf(monster.hp, 1)
    }

    private fun rawAtk(monster: MonsterDefinition, ctx: ExpressionContext?): Int {
        val raw = CombatAdjustment.monsterRawAttack(monster, ctx)
        return if (raw > 0) raw else maxOf(monster.attack, 1)
    }

    private fun rawDef(monster: MonsterDefinition, ctx: ExpressionContext?): Int {
        val raw = CombatAdjustment.monsterRawDefense(monster, ctx)
        return if (raw > 0) raw else maxOf(monster.defense, 1)
    }

    private fun effectivePhysicalResistance(monster: MonsterDefinition, ctx: ExpressionContext?): Int =
        CombatAdjustment.monsterPhysicalResistance(monster, ctx, ml = 0)

    private fun effectiveElementalResistance(monster: MonsterDefinition, ctx: ExpressionContext?): Int =
        CombatAdjustment.monsterElementalResistance(monster, ctx)

    private fun setAttackElement(monster: MonsterDefinition, element: String): MonsterDefinition {
        val elements = if (element in monster.attackElements) {
            monster.attackElements
        } else {
            monster.attackElements + element
        }
        return monster.copy(
            attackElements = elements,
            attackElement = primaryAttackElement(elements),
        )
    }

    private fun withNumericStats(
        monster: MonsterDefinition,
        hp: Int? = null,
        attack: Int? = null,
        defense: Int? = null,
        initiative: Int? = null,
        meatDrop: Int? = null,
    ): MonsterDefinition = monster.copy(
        hp = hp ?: monster.hp,
        attack = attack ?: monster.attack,
        defense = defense ?: monster.defense,
        initiative = initiative ?: monster.initiative,
        meatDrop = meatDrop ?: monster.meatDrop,
        hpExpression = if (hp != null) null else monster.hpExpression,
        attackExpression = if (attack != null) null else monster.attackExpression,
        defenseExpression = if (defense != null) null else monster.defenseExpression,
        initiativeExpression = if (initiative != null) null else monster.initiativeExpression,
        hasHp = if (hp != null) true else monster.hasHp,
        hasAttack = if (attack != null) true else monster.hasAttack,
        hasDefense = if (defense != null) true else monster.hasDefense,
        hasInitiative = if (initiative != null) true else monster.hasInitiative,
    )
}
