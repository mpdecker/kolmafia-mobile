package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.request.SpelunkyRequest

/** Phases 3051–3110: headless Spelunky and Bastille residual helpers. */
internal fun GameRuntimeLibrary.registerPhase3110(scope: AshScope) {
    regFn(scope, "spelunky_gold", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(SpelunkyRequest.getGold(preferences).toLong())
    }
    regFn(scope, "spelunky_bombs", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(SpelunkyRequest.getBombs(preferences).toLong())
    }
    regFn(scope, "spelunky_ropes", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(SpelunkyRequest.getRopes(preferences).toLong())
    }
    regFn(scope, "spelunky_keys", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(SpelunkyRequest.getKeys(preferences).toLong())
    }
    regFn(scope, "spelunky_turns", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(SpelunkyRequest.getTurnsLeft(preferences).toLong())
    }
    regFn(scope, "spelunky_buddy", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(SpelunkyRequest.getBuddyName(preferences))
    }
    regFn(scope, "spelunky_noncombat_due", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(SpelunkyRequest.spelunkyNoncombatDue(preferences))
    }
}
