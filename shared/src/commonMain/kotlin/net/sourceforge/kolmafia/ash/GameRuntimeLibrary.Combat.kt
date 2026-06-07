package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.preferences.Preferences

internal fun GameRuntimeLibrary.registerCombatStubs(scope: AshScope) {

    // in_multi_fight() → boolean  (stub — always false on mobile)
    regFn(scope, "in_multi_fight", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(false)
    }

    // fight_follows_choice() → boolean  (stub — always false on mobile)
    regFn(scope, "fight_follows_choice", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(false)
    }

    // last_monster() → monster  — reads _lastMonster preference
    regFn(scope, "last_monster", AshType.MONSTER, emptyList()) { _, _ ->
        val name = preferences?.getString(Preferences.LAST_MONSTER, "") ?: ""
        AshValue(AshType.MONSTER, name)
    }

    // copiers_used(skill) → int  (stub — always 0 on mobile)
    regFn(scope, "copiers_used", AshType.INT,
        listOf("sk" to AshType.SKILL)) { _, _ ->
        AshValue.of(0L)
    }
}
