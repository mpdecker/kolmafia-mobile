package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.session.LimitModeNames

/**
 * ASH-P430 behavioral batch — ascension/run-mode status accessors.
 */
internal fun GameRuntimeLibrary.registerAshP430Batch(scope: AshScope) {
    regFn(scope, "limit_mode", AshType.STRING, emptyList()) { _, _ ->
        val state = character?.state?.value
        AshValue.of(LimitModeNames.ashName(state?.limitMode ?: ""))
    }

    regFn(scope, "in_casual", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(character?.state?.value?.isCasual ?: false)
    }

    regFn(scope, "turns_played", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((character?.state?.value?.currentRun ?: 0).toLong())
    }

    regFn(scope, "my_turncount", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((character?.state?.value?.currentRun ?: 0).toLong())
    }

    regFn(scope, "total_turns_played", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((character?.state?.value?.turnsPlayed ?: 0).toLong())
    }

    regFn(scope, "daycount", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((character?.state?.value?.globalDaycount ?: 0).toLong())
    }
}
