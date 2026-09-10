package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.CharacterStats
import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.combat.RandomModifierStats
import net.sourceforge.kolmafia.data.MonsterDefinition
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * AshP43 — monster-focused jump_chance ASH library.
 * Mirrors desktop [RuntimeLibrary] / [MonsterData.getJumpChance]
 * (location overloads in AshP44; Init/Overclocked polish AshP48).
 *
 * Phase 6441–6450: RandomModifierStats on all monster paths; initMl vs attackMl
 * desktop quirk preserved (3-arg ml only feeds initiative).
 *
 * Phase 6611–6630 (XLVI Track A): OCRS init overlay on all monster overloads
 * (ninja mask → 0; askew/bouncing attack vs mainstat residual).
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

    fun effectiveMonster(raw: MonsterDefinition?): MonsterDefinition? {
        if (raw == null) return null
        return RandomModifierStats.apply(raw, raw.randomModifiers, buildMonsterExpressionContext())
    }

    fun lastMonster(): MonsterDefinition? {
        val raw = MonsterStatusTracker.getLastMonster()
            ?: resolveMonsterDefinition(preferences?.getString(Preferences.LAST_MONSTER, "") ?: "")
            ?: return null
        return effectiveMonster(raw)
    }

    fun hasOverclocked(): Boolean =
        skillManager?.state?.value?.skills?.any {
            it.id == OVERCLOCKED_SKILL_ID || it.name.equals("Overclocked", ignoreCase = true)
        } == true

    fun jump(monster: MonsterDefinition?, initBonus: Int, initMl: Int): Int =
        CombatAdjustment.jumpChance(
            monster = monster,
            initBonus = initBonus,
            initMl = initMl,
            // Desktop quirk: overload ml only affects initiative; attack uses live character ML.
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
            jump(
                effectiveMonster(resolveMonsterDefinition(args[0].toString())),
                currentInitBonus(),
                ml,
            ).toLong(),
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
                effectiveMonster(resolveMonsterDefinition(args[0].toString())),
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
                effectiveMonster(resolveMonsterDefinition(args[0].toString())),
                args[1].toLong().toInt(),
                args[2].toLong().toInt(),
            ).toLong(),
        )
    }
}

/** Desktop [SkillPool.OVERCLOCKED]. */
internal const val OVERCLOCKED_SKILL_ID = 21001
