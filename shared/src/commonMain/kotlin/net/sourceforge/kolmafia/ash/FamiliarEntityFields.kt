package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.PokefamTeamSlot
import net.sourceforge.kolmafia.data.FamiliarDefinitionProxy
import net.sourceforge.kolmafia.data.FamiliarDailyStats
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.PokefamDatabase
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Resolves `$familiar[field]` bracket access. Mirrors desktop FamiliarProxy metadata,
 * runtime ownership, daily caps, and pokefam stats.
 */
internal object FamiliarEntityFields {

    private val stringArrayType = AggregateType(AshType.INT, AshType.STRING)

    fun resolve(
        familiarRef: String,
        fieldName: String,
        gameDatabase: GameDatabase?,
        familiarManager: FamiliarManager?,
        preferences: Preferences?,
        pokeTeam: List<PokefamTeamSlot> = emptyList(),
    ): AshValue {
        val definition = FamiliarDefinitionProxy.getByIdOrName(familiarRef)
            ?: gameDatabase?.familiar(familiarRef)
        val familiarId = definition?.id ?: FamiliarDefinitionProxy.resolveFamiliarId(familiarRef)
        val owned = ownedFamiliar(familiarId, familiarRef, familiarManager)

        return when (fieldName.lowercase()) {
            "id" -> AshValue.of(familiarId.toLong())
            "name" -> AshValue.of(owned?.name ?: "")
            "image" -> AshValue.of(FamiliarDefinitionProxy.getImage(familiarId))
            "hatchling" -> hatchlingValue(familiarId)
            "owner" -> AshValue.of(owned?.modifiers?.get("owner") ?: "")
            "owner_id" -> AshValue.of((owned?.modifiers?.get("ownerId")?.toLongOrNull() ?: 0L))
            "experience" -> AshValue.of((owned?.experience ?: 0).toLong())
            "charges" -> AshValue.of(0L)
            "drop_name" -> AshValue.of(FamiliarDefinitionProxy.getDropName(familiarId))
            "drop_item" -> dropItemValue(familiarId)
            "drops_today" -> AshValue.of(FamiliarDailyStats.dropsToday(familiarId, preferences).toLong())
            "drops_limit" -> AshValue.of(FamiliarDailyStats.dropDailyCap(familiarId, preferences).toLong())
            "fights_today" -> AshValue.of(FamiliarDailyStats.fightsToday(familiarId, preferences).toLong())
            "fights_limit" -> AshValue.of(FamiliarDailyStats.fightDailyCap(familiarId).toLong())
            "combat" -> AshValue.of(definition?.isCombatType() ?: false)
            "physical_damage" -> AshValue.of(definition?.isCombat0Type() ?: false)
            "elemental_damage" -> AshValue.of(definition?.isCombat1Type() ?: false)
            "block" -> AshValue.of(definition?.isBlockType() ?: false)
            "delevel" -> AshValue.of(definition?.isDelevelType() ?: false)
            "hp_during_combat" -> AshValue.of(definition?.isHp0Type() ?: false)
            "mp_during_combat" -> AshValue.of(definition?.isMp0Type() ?: false)
            "other_action_during_combat" -> AshValue.of(definition?.isOther0Type() ?: false)
            "hp_after_combat" -> AshValue.of(definition?.isHp1Type() ?: false)
            "mp_after_combat" -> AshValue.of(definition?.isMp1Type() ?: false)
            "other_action_after_combat" -> AshValue.of(definition?.isOther1Type() ?: false)
            "passive" -> AshValue.of(definition?.isPassiveType() ?: false)
            "underwater" -> AshValue.of(definition?.isUnderwaterType() ?: false)
            "variable" -> AshValue.of(definition?.isVariableType() ?: false)
            "feasted" -> AshValue.of(owned?.feasted ?: false)
            "attributes" -> AshValue.of(FamiliarDefinitionProxy.getAttributesString(familiarId))
            "poke_level" -> AshValue.of(pokeLevelValue(familiarId, owned, pokeTeam))
            "poke_level_2_power" -> AshValue.of((PokefamDatabase.getById(familiarId)?.power2 ?: 0).toLong())
            "poke_level_2_hp" -> AshValue.of((PokefamDatabase.getById(familiarId)?.hp2 ?: 0).toLong())
            "poke_level_3_power" -> AshValue.of((PokefamDatabase.getById(familiarId)?.power3 ?: 0).toLong())
            "poke_level_3_hp" -> AshValue.of((PokefamDatabase.getById(familiarId)?.hp3 ?: 0).toLong())
            "poke_level_4_power" -> AshValue.of((PokefamDatabase.getById(familiarId)?.power4 ?: 0).toLong())
            "poke_level_4_hp" -> AshValue.of((PokefamDatabase.getById(familiarId)?.hp4 ?: 0).toLong())
            "poke_move_1" -> AshValue.of(PokefamDatabase.getById(familiarId)?.move1 ?: "")
            "poke_move_2" -> AshValue.of(PokefamDatabase.getById(familiarId)?.move2 ?: "")
            "poke_move_3" -> AshValue.of(PokefamDatabase.getById(familiarId)?.move3 ?: "")
            "poke_attribute" -> AshValue.of(PokefamDatabase.getById(familiarId)?.attribute ?: "")
            "soup_weight" -> AshValue.of((owned?.soupWeight ?: 0).toLong())
            "soup_attributes" -> stringListAggregate(owned?.soupAttributes?.sorted() ?: emptyList())
            else -> throw ScriptException("familiar has no field '$fieldName'")
        }
    }

    private fun pokeLevelValue(
        familiarId: Int,
        owned: FamiliarData?,
        pokeTeam: List<PokefamTeamSlot>,
    ): Long {
        pokeTeam.firstOrNull { it.familiarId == familiarId && !it.isEmpty }
            ?.let { return it.level.toLong() }
        return (owned?.pokeLevel ?: 0).toLong()
    }

    private fun ownedFamiliar(
        familiarId: Int,
        familiarRef: String,
        familiarManager: FamiliarManager?,
    ): FamiliarData? =
        familiarManager?.state?.value?.ownedFamiliars?.find { fam ->
            fam.id == familiarId ||
                fam.name.equals(familiarRef, ignoreCase = true) ||
                fam.race.equals(familiarRef, ignoreCase = true)
        }

    private fun hatchlingValue(familiarId: Int): AshValue {
        val itemId = FamiliarDefinitionProxy.getLarvaItemId(familiarId)
        val itemName = ItemDatabase.getById(itemId)?.name
            ?: FamiliarDefinitionProxy.getLarvaItemName(familiarId)
        return if (itemName.isBlank()) AshValue.item("") else AshValue.item(itemName)
    }

    private fun dropItemValue(familiarId: Int): AshValue {
        val itemId = FamiliarDefinitionProxy.getDropItemId(familiarId)
        val itemName = ItemDatabase.getById(itemId)?.name ?: ""
        return if (itemName.isBlank()) AshValue.item("") else AshValue.item(itemName)
    }

    private fun stringListAggregate(values: List<String>): AggregateValue {
        val result = AggregateValue(stringArrayType)
        values.forEachIndexed { i, value ->
            result[AshValue.of(i)] = AshValue.of(value)
        }
        return result
    }
}
