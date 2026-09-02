package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.session.GreyYouManager

/** AshP865 — minimal live Grey You absorption query surface. */
internal fun GameRuntimeLibrary.registerAshP865Batch(scope: AshScope) {
    regFn(scope, "grey_you_absorbed", AshType.BOOLEAN, listOf("monster_id" to AshType.INT)) { _, args ->
        AshValue.of(GreyYouManager.haveAbsorbed(args[0].toLong().toInt()))
    }
    regFn(scope, "grey_you_skill", AshType.BOOLEAN, listOf("skill_id" to AshType.INT)) { _, args ->
        AshValue.of(GreyYouManager.haveLearned(args[0].toLong().toInt()))
    }
    regFn(scope, "grey_you_unknown", AshType.STRING, listOf("monster_id" to AshType.INT)) { _, args ->
        AshValue.of(GreyYouManager.unknownDescription(args[0].toLong().toInt()).orEmpty())
    }
    regFn(scope, "grey_you_absorption_count", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(GreyYouManager.absorbedMonsters.size)
    }
    regFn(scope, "absorbed_monsters", AshType.BOOLEAN, listOf("monster_id" to AshType.MONSTER)) { _, args ->
        AshValue.of(GreyYouManager.haveAbsorbed(args[0].toLong().toInt()))
    }
    // Phase 4470: desktop no-arg absorbed_monsters → boolean[monster] map
    val absorbedMapType = AggregateType(AshType.MONSTER, AshType.BOOLEAN)
    regFn(scope, "absorbed_monsters", absorbedMapType, emptyList()) { _, _ ->
        val result = AggregateValue(absorbedMapType)
        for (id in GreyYouManager.absorbedMonsters) {
            val name = MonsterDatabase.getById(id)?.name ?: id.toString()
            result[AshValue(AshType.MONSTER, name)] = AshValue.TRUE
        }
        result
    }
}
