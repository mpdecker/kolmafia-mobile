package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.request.ChateauRequest
import net.sourceforge.kolmafia.request.FloristRequest
import net.sourceforge.kolmafia.session.LocketManager

/**
 * IoTM manager residual sidecar — phases 3711–3770.
 *
 * Live `get_florist_plants` / corrected `get_chateau` furniture inventory, plus headless
 * `git_*` / `svn_*` stubs so scripts that declare those names parse on mobile.
 */
internal fun GameRuntimeLibrary.registerPhase3770(scope: AshScope) {
    val plantRow = AggregateType(AshType.INT, AshType.STRING, fixedSize = 3)
    val plantsByLocation = AggregateType(AshType.LOCATION, plantRow)
    regFn(scope, "get_florist_plants", plantsByLocation, emptyList()) { _, _ ->
        val result = AggregateValue(plantsByLocation)
        for ((location, plants) in FloristRequest.floristPlants) {
            if (plants.isEmpty()) continue
            val row = AggregateValue(plantRow)
            plants.forEachIndexed { index, plant ->
                row[AshValue.of(index)] = AshValue.of(plant.plantName)
            }
            result[AshValue.location(location)] = row
        }
        result
    }

    val svnInfo = RecordType(
        "svn_info_type",
        listOf("url", "revision", "last_changed_author", "last_changed_rev", "last_changed_date")
            .mapIndexed { index, name ->
                RecordField(name, if (name == "revision" || name == "last_changed_rev") AshType.INT else AshType.STRING, index)
            },
    )
    val gitInfo = RecordType(
        "git_info_type",
        listOf("url", "branch", "commit", "last_changed_author", "last_changed_date")
            .mapIndexed { index, name -> RecordField(name, AshType.STRING, index) },
    )
    val stringArray = AggregateType(AshType.INT, AshType.STRING)

    regFn(scope, "svn_exists", AshType.BOOLEAN, listOf("project" to AshType.STRING)) { _, _ ->
        AshValue.FALSE
    }
    regFn(scope, "svn_at_head", AshType.BOOLEAN, listOf("project" to AshType.STRING)) { _, _ ->
        AshValue.FALSE
    }
    regFn(scope, "svn_list", stringArray, emptyList()) { _, _ ->
        AggregateValue(stringArray)
    }
    regFn(scope, "svn_info", svnInfo, listOf("script" to AshType.STRING)) { _, _ ->
        RecordValue(svnInfo)
    }
    regFn(scope, "git_exists", AshType.BOOLEAN, listOf("project" to AshType.STRING)) { _, _ ->
        AshValue.FALSE
    }
    regFn(scope, "git_at_head", AshType.BOOLEAN, listOf("project" to AshType.STRING)) { _, _ ->
        AshValue.FALSE
    }
    regFn(scope, "git_list", stringArray, emptyList()) { _, _ ->
        AggregateValue(stringArray)
    }
    regFn(scope, "git_info", gitInfo, listOf("script" to AshType.STRING)) { _, _ ->
        RecordValue(gitInfo)
    }
}

internal fun locketMonsterMap(preferences: net.sourceforge.kolmafia.preferences.Preferences?): AggregateValue {
    val monsterArray = AggregateType(AshType.MONSTER, AshType.BOOLEAN)
    val result = AggregateValue(monsterArray)
    for (id in LocketManager.getMonsters()) {
        val name = MonsterDatabase.getById(id)?.name ?: "monster #$id"
        result[AshValue(AshType.MONSTER, name)] =
            AshValue.of(!LocketManager.foughtMonster(preferences, id))
    }
    return result
}

internal fun chateauFurnitureMap(): AggregateValue {
    val itemToInt = AggregateType(AshType.ITEM, AshType.INT)
    val result = AggregateValue(itemToInt)
    for (id in ChateauRequest.furnitureIds()) {
        val name = ItemDatabase.getItemName(id).ifBlank { "item #$id" }
        result[AshValue.item(name)] = AshValue.of(1L)
    }
    return result
}
