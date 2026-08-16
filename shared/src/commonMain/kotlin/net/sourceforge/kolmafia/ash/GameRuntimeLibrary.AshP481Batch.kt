package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.session.PvpManager

/** AshP481/AshP482 — PvP query ASH: attacks left, hippy stone, stance map with fight-page prefetch. */
internal fun GameRuntimeLibrary.registerAshP481Batch(scope: AshScope) {
    val stanceMapType = AggregateType(AshType.STRING, AshType.INT)

    regFn(scope, "pvp_attacks_left", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((character?.state?.value?.pvpFightsLeft ?: 0).toLong())
    }

    regFn(scope, "hippy_stone_broken", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(character?.state?.value?.hippyStoneBroken ?: false)
    }

    regFn(scope, "current_pvp_stances", stanceMapType, emptyList()) { _, _ ->
        if (!PvpManager.stancesKnown && httpClient != null) {
            runBlocking {
                PvpManager.checkStances(httpClient, character, preferences, sessionLogger)
            }
        }
        val result = AggregateValue(stanceMapType)
        for ((name, option) in PvpManager.stanceToOption) {
            result[AshValue.of(name)] = AshValue.of(option.toLong())
        }
        result
    }
}
