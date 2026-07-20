package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.MonsterDatabase

/**
 * AshP47 — all_monsters_with_id ASH library.
 * Mirrors desktop [RuntimeLibrary.all_monsters_with_id]: boolean[monster] for every
 * monster with a non-zero id.
 */
internal fun GameRuntimeLibrary.registerAshP47Batch(scope: AshScope) {
    val booleanMonsterType = AggregateType(AshType.MONSTER, AshType.BOOLEAN)

    regFn(scope, "all_monsters_with_id", booleanMonsterType, emptyList()) { _, _ ->
        buildAllMonstersWithId(booleanMonsterType)
    }
}

private fun buildAllMonstersWithId(type: AggregateType): AggregateValue {
    val result = AggregateValue(type)
    for (monster in MonsterDatabase.byId.values) {
        if (monster.id == 0) continue
        result[AshValue(AshType.MONSTER, monster.name)] = AshValue.TRUE
    }
    return result
}
