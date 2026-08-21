package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ChoiceCombatAshState

internal fun GameRuntimeLibrary.registerCombatStubs(scope: AshScope) {

    regFn(scope, "in_multi_fight", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(
            ChoiceCombatAshState.inMultiFight ||
                adventureManager?.inMultiFight == true,
        )
    }

    regFn(scope, "fight_follows_choice", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(
            ChoiceCombatAshState.fightFollowsChoice ||
                adventureManager?.fightFollowsChoice == true,
        )
    }

    // last_monster() → monster  — reads tracker instance or _lastMonster preference
    regFn(scope, "last_monster", AshType.MONSTER, emptyList()) { _, _ ->
        val name = MonsterStatusTracker.getLastMonsterName().ifEmpty {
            preferences?.getString(Preferences.LAST_MONSTER, "") ?: ""
        }
        AshValue(AshType.MONSTER, MonsterAshRef(name, useInstance = true))
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
