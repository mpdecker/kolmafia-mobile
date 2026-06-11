package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.preferences.Preferences

internal fun GameRuntimeLibrary.registerCombatStubs(scope: AshScope) {

    regFn(scope, "in_multi_fight", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(adventureManager?.inMultiFight ?: false)
    }

    regFn(scope, "fight_follows_choice", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(adventureManager?.fightFollowsChoice ?: false)
    }

    // last_monster() → monster  — reads _lastMonster preference
    regFn(scope, "last_monster", AshType.MONSTER, emptyList()) { _, _ ->
        val name = preferences?.getString(Preferences.LAST_MONSTER, "") ?: ""
        AshValue(AshType.MONSTER, name)
    }

    // copiers_used(skill) → int — returns timesCast for the named skill
    regFn(scope, "copiers_used", AshType.INT,
        listOf("sk" to AshType.SKILL)) { _, args ->
        val skillName = args[0].toString()
        val times = skillManager?.state?.value?.skills
            ?.find { it.name.equals(skillName, ignoreCase = true) }
            ?.timesCast ?: 0
        AshValue.of(times.toLong())
    }
}
