package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.adventure.runHedgeMaze

/**
 * ASH-P20 behavioral batch — Naughty Sorceress hedge maze automation prefs.
 */
internal fun GameRuntimeLibrary.registerAshP20Batch(scope: AshScope) {
    regFn(scope, "hedge_maze", AshType.BOOLEAN, listOf("tag" to AshType.STRING)) { rt, args ->
        val mode = hedgeMazeModeFromTag(args[0].toString()) ?: return@regFn AshValue.FALSE
        val configured = applyHedgeMazeMode(mode)
        if (!configured) return@regFn AshValue.FALSE
        val canAutomate = httpClient != null && adventureManager != null && character != null
        AshValue.of(
            if (canAutomate) runHedgeMaze(mode) { message -> rt.print(message) }
            else true,
        )
    }
}
