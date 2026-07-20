package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.CharacterStats
import net.sourceforge.kolmafia.data.MonsterDefinition
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * AshP43 — monster-focused jump_chance ASH library.
 * Mirrors desktop [RuntimeLibrary] / [MonsterData.getJumpChance]
 * (location overloads in AshP44; Init/Overclocked polish AshP48).
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

    fun hasOverclocked(): Boolean =
        skillManager?.state?.value?.skills?.any {
            it.id == OVERCLOCKED_SKILL_ID || it.name.equals("Overclocked", ignoreCase = true)
        } == true

    fun jump(monster: MonsterDefinition?, initBonus: Int, initMl: Int): Int =
        CombatAdjustment.jumpChance(
            monster = monster,
            initBonus = initBonus,
            initMl = initMl,
            attackMl = currentMl(),
            baseMainstat = baseMainstat(),
            hasOverclocked = hasOverclocked(),
            expressionContext = buildMonsterExpressionContext(),
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

/** Desktop [SkillPool.OVERCLOCKED]. */
internal const val OVERCLOCKED_SKILL_ID = 21001
