package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * AshP42 — monster defense-element ASH library.
 * Mirrors desktop [RuntimeLibrary.monster_element] via [MonsterDefinition.defenseElement].
 */
internal fun GameRuntimeLibrary.registerAshP42Batch(scope: AshScope) {
    fun lastMonster() =
        resolveMonsterDefinition(preferences?.getString(Preferences.LAST_MONSTER, "") ?: "")

    regFn(scope, "monster_element", AshType.ELEMENT, emptyList()) { _, _ ->
        AshValue(AshType.ELEMENT, CombatAdjustment.monsterDefenseElement(lastMonster()))
    }
    regFn(scope, "monster_element", AshType.ELEMENT, listOf("monster" to AshType.MONSTER)) { _, args ->
        AshValue(
            AshType.ELEMENT,
            CombatAdjustment.monsterDefenseElement(resolveMonsterDefinition(args[0].toString())),
        )
    }
}
