package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterPartsDatabase
import net.sourceforge.kolmafia.modifiers.ExpressionContext

/**
 * Resolves `$monster[field]` bracket access. Mirrors desktop [MonsterProxy].
 */
internal object MonsterEntityFields {

    private val partsAggregateType = AggregateType(AshType.INT, AshType.STRING)

    fun resolve(
        monsterName: String,
        fieldName: String,
        gameDatabase: GameDatabase?,
        expressionContext: ExpressionContext? = null,
        ml: Int = 0,
        xpMultiplier: Int = 1,
    ): AshValue {
        val monster = gameDatabase?.monster(monsterName)
        return when (fieldName.lowercase()) {
            "id" -> AshValue.of((monster?.id ?: 0).toLong())
            "name" -> AshValue.of(monster?.name ?: "")
            "article" -> AshValue.of(monster?.article ?: "")
            "image" -> AshValue.of(monster?.image ?: "")
            "base_hp" -> AshValue.of((monster?.hp ?: 0).toLong())
            "base_attack" -> AshValue.of((monster?.attack ?: 0).toLong())
            "base_defense" -> AshValue.of((monster?.defense ?: 0).toLong())
            "base_initiative" -> AshValue.of((monster?.initiative ?: 0).toLong())
            "raw_hp" -> AshValue.of(
                CombatAdjustment.monsterRawHp(monster, expressionContext).toLong(),
            )
            "raw_attack" -> AshValue.of(
                CombatAdjustment.monsterRawAttack(monster, expressionContext).toLong(),
            )
            "raw_defense" -> AshValue.of(
                CombatAdjustment.monsterRawDefense(monster, expressionContext).toLong(),
            )
            "raw_initiative" -> AshValue.of(
                CombatAdjustment.monsterRawInitiative(monster, expressionContext).toLong(),
            )
            "base_mainstat_exp" -> AshValue.of(
                CombatAdjustment.monsterExperience(
                    monster,
                    ml,
                    expressionContext,
                    xpMultiplier,
                ),
            )
            "min_meat" -> AshValue.of((monster?.meatDrop ?: 0).toLong())
            "max_meat" -> AshValue.of((monster?.meatDrop ?: 0).toLong())
            "phylum" -> AshValue(AshType.PHYLUM, monster?.phylum ?: "")
            "attack_element" -> AshValue(AshType.ELEMENT, monster?.attackElement ?: "")
            "defense_element" -> AshValue(AshType.ELEMENT, monster?.defenseElement ?: "")
            "physical_resistance" -> AshValue.of(
                CombatAdjustment.monsterPhysicalResistance(monster, expressionContext, ml).toLong(),
            )
            "elemental_resistance" -> AshValue.of(
                CombatAdjustment.monsterElementalResistance(monster, expressionContext).toLong(),
            )
            "hot_resistance" -> AshValue.of(
                CombatAdjustment.monsterElementResistance(monster, "hot", expressionContext).toLong(),
            )
            "cold_resistance" -> AshValue.of(
                CombatAdjustment.monsterElementResistance(monster, "cold", expressionContext).toLong(),
            )
            "stench_resistance" -> AshValue.of(
                CombatAdjustment.monsterElementResistance(monster, "stench", expressionContext).toLong(),
            )
            "spooky_resistance" -> AshValue.of(
                CombatAdjustment.monsterElementResistance(monster, "spooky", expressionContext).toLong(),
            )
            "sleaze_resistance" -> AshValue.of(
                CombatAdjustment.monsterElementResistance(monster, "sleaze", expressionContext).toLong(),
            )
            "boss" -> AshValue.of(monster?.isBoss ?: false)
            "ghost" -> AshValue.of(monster?.isGhost ?: false)
            "lucky" -> AshValue.of(monster?.isLucky ?: false)
            "copyable" -> AshValue.of(monster?.isCopyable ?: true)
            "wishable" -> AshValue.of(monster?.isWishable ?: true)
            "parts" -> partsAggregate(monster?.id ?: 0)
            else -> throw ScriptException("monster has no field '$fieldName'")
        }
    }

    private fun partsAggregate(monsterId: Int): AggregateValue {
        val result = AggregateValue(partsAggregateType)
        val parts = MonsterPartsDatabase.partsForId(monsterId)
        parts.forEachIndexed { i, part ->
            result[AshValue.of(i)] = AshValue.of(part)
        }
        return result
    }
}
