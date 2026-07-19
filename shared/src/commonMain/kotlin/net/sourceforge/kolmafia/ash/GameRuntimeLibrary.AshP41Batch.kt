package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * AshP41 — monster combat-stat ASH library.
 * Mirrors desktop [RuntimeLibrary] monster_attack/defense/hp/initiative/phylum
 * using [MonsterDefinition] + desktop-lite ML adjustment (not MonsterStatusTracker).
 */
internal fun GameRuntimeLibrary.registerAshP41Batch(scope: AshScope) {
    fun currentMl(): Int =
        CombatAdjustment.monsterLevelAdjustment(
            buildCurrentModifiers(),
            character?.state?.value,
            lastLocationName(),
        )

    fun lastMonster() =
        resolveMonsterDefinition(preferences?.getString(Preferences.LAST_MONSTER, "") ?: "")

    regFn(scope, "monster_attack", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(CombatAdjustment.monsterAttack(lastMonster(), currentMl()).toLong())
    }
    regFn(scope, "monster_attack", AshType.INT, listOf("monster" to AshType.MONSTER)) { _, args ->
        AshValue.of(
            CombatAdjustment.monsterAttack(resolveMonsterDefinition(args[0].toString()), currentMl())
                .toLong(),
        )
    }

    regFn(scope, "monster_defense", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(CombatAdjustment.monsterDefense(lastMonster(), currentMl()).toLong())
    }
    regFn(scope, "monster_defense", AshType.INT, listOf("monster" to AshType.MONSTER)) { _, args ->
        AshValue.of(
            CombatAdjustment.monsterDefense(resolveMonsterDefinition(args[0].toString()), currentMl())
                .toLong(),
        )
    }

    regFn(scope, "monster_hp", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(CombatAdjustment.monsterHp(lastMonster(), currentMl()).toLong())
    }
    regFn(scope, "monster_hp", AshType.INT, listOf("monster" to AshType.MONSTER)) { _, args ->
        AshValue.of(
            CombatAdjustment.monsterHp(resolveMonsterDefinition(args[0].toString()), currentMl())
                .toLong(),
        )
    }

    regFn(scope, "monster_initiative", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(CombatAdjustment.monsterInitiative(lastMonster()).toLong())
    }
    regFn(scope, "monster_initiative", AshType.INT, listOf("monster" to AshType.MONSTER)) { _, args ->
        AshValue.of(
            CombatAdjustment.monsterInitiative(resolveMonsterDefinition(args[0].toString())).toLong(),
        )
    }

    regFn(scope, "monster_phylum", AshType.PHYLUM, emptyList()) { _, _ ->
        AshValue(AshType.PHYLUM, CombatAdjustment.monsterPhylum(lastMonster()))
    }
    regFn(scope, "monster_phylum", AshType.PHYLUM, listOf("monster" to AshType.MONSTER)) { _, args ->
        AshValue(
            AshType.PHYLUM,
            CombatAdjustment.monsterPhylum(resolveMonsterDefinition(args[0].toString())),
        )
    }
}
