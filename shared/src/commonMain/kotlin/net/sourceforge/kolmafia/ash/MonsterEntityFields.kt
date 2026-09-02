package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.data.FactDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterPartsDatabase
import net.sourceforge.kolmafia.data.MonsterDefinition
import net.sourceforge.kolmafia.data.PoisonLevels
import net.sourceforge.kolmafia.data.canonicalElementOrder
import net.sourceforge.kolmafia.data.primaryAttackElement
import net.sourceforge.kolmafia.modifiers.ExpressionContext

/**
 * Resolves `$monster[field]` bracket access. Mirrors desktop [MonsterProxy].
 */
internal object MonsterEntityFields {

    private val partsAggregateType = AggregateType(AshType.INT, AshType.STRING)
    private val elementArrayType = AggregateType(AshType.INT, AshType.ELEMENT)

    fun resolve(
        monsterName: String,
        fieldName: String,
        gameDatabase: GameDatabase?,
        expressionContext: ExpressionContext? = null,
        ml: Int = 0,
        xpMultiplier: Int = 1,
        reduceEnemyDefensePercent: Double = 0.0,
        characterClass: CharacterClass = CharacterClass.UNKNOWN,
        ascensionPath: AscensionPath = AscensionPath.NONE,
        monsterOverride: MonsterDefinition? = null,
    ): AshValue {
        val monster = monsterOverride ?: gameDatabase?.monster(monsterName)
        return when (fieldName.lowercase()) {
            "id" -> AshValue.of((monster?.id ?: 0).toLong())
            "name" -> AshValue.of(monster?.name ?: "")
            "article" -> AshValue.of(monster?.article ?: "")
            "blue_vs_red_team" -> AshValue.of(monster?.blueVsRedTeam?.teamName ?: "")
            "image" -> AshValue.of(monster?.image ?: "")
            "base_hp" -> AshValue.of(
                CombatAdjustment.monsterHp(monster, ml, expressionContext).toLong(),
            )
            "base_attack" -> AshValue.of(
                CombatAdjustment.monsterAttack(monster, ml, expressionContext).toLong(),
            )
            "base_defense" -> AshValue.of(
                CombatAdjustment.monsterDefense(
                    monster,
                    ml,
                    expressionContext,
                    reduceEnemyDefensePercent,
                ).toLong(),
            )
            "base_initiative" -> AshValue.of(
                CombatAdjustment.monsterInitiativeWithMl(monster, ml, expressionContext).toLong(),
            )
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
            "attack_element" -> AshValue(
                AshType.ELEMENT,
                primaryAttackElement(monster?.attackElements ?: emptyList()),
            )
            "attack_elements" -> attackElementsAggregate(monster?.attackElements ?: emptyList())
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
            "poison" -> AshValue.effect(
                PoisonLevels.effectNameForLevel(monster?.poison ?: Int.MAX_VALUE),
            )
            "group" -> AshValue.of((monster?.group ?: 1).toLong())
            "manuel_name" -> AshValue.of(monster?.manuelName ?: monster?.name ?: "")
            "wiki_name" -> AshValue.of(monster?.wikiName ?: monster?.name ?: "")
            "sub_types" -> stringListAggregate(monster?.subTypes ?: emptyList())
            "images" -> stringListAggregate(monster?.images ?: emptyList())
            "random_modifiers" -> stringListAggregate(monster?.randomModifiers ?: emptyList())
            "attributes" -> AshValue.of(monster?.attributes ?: "")
            "fact" -> AshValue.of(
                FactDatabase.factString(monster, characterClass, ascensionPath, expressionContext),
            )
            "fact_type" -> AshValue.of(
                FactDatabase.factTypeString(monster, characterClass, ascensionPath, expressionContext),
            )
            "min_sprinkles" -> AshValue.of(
                CombatAdjustment.monsterMinSprinkles(monster, expressionContext).toLong(),
            )
            "max_sprinkles" -> AshValue.of(
                CombatAdjustment.monsterMaxSprinkles(monster, expressionContext).toLong(),
            )
            "parts" -> partsAggregate(monster?.id ?: 0)
            else -> throw ScriptException("monster has no field '$fieldName'")
        }
    }

    private fun attackElementsAggregate(elements: List<String>): AggregateValue {
        val result = AggregateValue(elementArrayType)
        canonicalElementOrder(elements).forEachIndexed { i, elem ->
            result[AshValue.of(i)] = AshValue(AshType.ELEMENT, elem)
        }
        return result
    }

    private fun stringListAggregate(values: List<String>): AggregateValue {
        val result = AggregateValue(partsAggregateType)
        values.forEachIndexed { i, value ->
            result[AshValue.of(i)] = AshValue.of(value)
        }
        return result
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
