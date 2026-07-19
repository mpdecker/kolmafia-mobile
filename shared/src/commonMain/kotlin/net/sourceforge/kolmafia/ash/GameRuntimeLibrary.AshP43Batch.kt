package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.CharacterStats
import net.sourceforge.kolmafia.data.MonsterDefinition
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * AshP43 — monster-focused jump_chance ASH library.
 * Mirrors desktop [RuntimeLibrary] / [MonsterData.getJumpChance] (location overloads deferred).
 */
internal fun GameRuntimeLibrary.registerAshP43Batch(scope: AshScope) {
    fun currentMl(): Int =
        CombatAdjustment.monsterLevelAdjustment(
            buildCurrentModifiers(),
            character?.state?.value,
            lastLocationName(),
        )

    fun currentInitBonus(): Int =
        CombatAdjustment.initiativeModifier(buildCurrentModifiers()).toInt()

    fun baseMainstat(): Int {
        val state = character?.state?.value ?: return 0
        return CharacterStats.mainStatBase(state)
    }

    fun lastMonster() =
        resolveMonsterDefinition(preferences?.getString(Preferences.LAST_MONSTER, "") ?: "")

    fun jump(monster: MonsterDefinition?, initBonus: Int, initMl: Int): Int =
        CombatAdjustment.jumpChance(
            monster = monster,
            initBonus = initBonus,
            initMl = initMl,
            attackMl = currentMl(),
            baseMainstat = baseMainstat(),
        )

    regFn(scope, "jump_chance", AshType.INT, emptyList()) { _, _ ->
        val ml = currentMl()
        AshValue.of(jump(lastMonster(), currentInitBonus(), ml).toLong())
    }

    regFn(scope, "jump_chance", AshType.INT, listOf("monster" to AshType.MONSTER)) { _, args ->
        val ml = currentMl()
        AshValue.of(
            jump(resolveMonsterDefinition(args[0].toString()), currentInitBonus(), ml).toLong(),
        )
    }

    regFn(
        scope,
        "jump_chance",
        AshType.INT,
        listOf("monster" to AshType.MONSTER, "init" to AshType.INT),
    ) { _, args ->
        val ml = currentMl()
        AshValue.of(
            jump(
                resolveMonsterDefinition(args[0].toString()),
                args[1].toLong().toInt(),
                ml,
            ).toLong(),
        )
    }

    regFn(
        scope,
        "jump_chance",
        AshType.INT,
        listOf("monster" to AshType.MONSTER, "init" to AshType.INT, "ml" to AshType.INT),
    ) { _, args ->
        AshValue.of(
            jump(
                resolveMonsterDefinition(args[0].toString()),
                args[1].toLong().toInt(),
                args[2].toLong().toInt(),
            ).toLong(),
        )
    }
}
