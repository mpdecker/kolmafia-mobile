package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.CharacterStats

/**
 * AshP44 — location jump_chance ASH library (min over positive-weight zone monsters).
 * Mirrors desktop [RuntimeLibrary.jump_chance] / [AreaCombatData.getJumpChance].
 */
internal fun GameRuntimeLibrary.registerAshP44Batch(scope: AshScope) {
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

    fun locationJump(location: String, initBonus: Int, initMl: Int): Int =
        CombatAdjustment.locationJumpChance(
            locationName = resolveLocationQueryName(location),
            initBonus = initBonus,
            initMl = initMl,
            attackMl = currentMl(),
            baseMainstat = baseMainstat(),
            hasOverclocked = skillManager?.state?.value?.skills?.any {
                it.id == OVERCLOCKED_SKILL_ID || it.name.equals("Overclocked", ignoreCase = true)
            } == true,
            expressionContext = buildMonsterExpressionContext(),
            resolveMonster = { resolveMonsterDefinition(it) },
        )

    regFn(scope, "jump_chance", AshType.INT, listOf("location" to AshType.LOCATION)) { _, args ->
        val ml = currentMl()
        AshValue.of(locationJump(args[0].toString(), currentInitBonus(), ml).toLong())
    }

    regFn(
        scope,
        "jump_chance",
        AshType.INT,
        listOf("location" to AshType.LOCATION, "init" to AshType.INT),
    ) { _, args ->
        val ml = currentMl()
        AshValue.of(
            locationJump(args[0].toString(), args[1].toLong().toInt(), ml).toLong(),
        )
    }

    regFn(
        scope,
        "jump_chance",
        AshType.INT,
        listOf("location" to AshType.LOCATION, "init" to AshType.INT, "ml" to AshType.INT),
    ) { _, args ->
        AshValue.of(
            locationJump(
                args[0].toString(),
                args[1].toLong().toInt(),
                args[2].toLong().toInt(),
            ).toLong(),
        )
    }
}
