package net.sourceforge.kolmafia.ash

/**
 * ASH-P20 behavioral batch — Naughty Sorceress hedge maze automation prefs.
 */
internal fun GameRuntimeLibrary.registerAshP20Batch(scope: AshScope) {
    regFn(scope, "hedge_maze", AshType.BOOLEAN, listOf("tag" to AshType.STRING)) { _, args ->
        val mode = hedgeMazeModeFromTag(args[0].toString()) ?: return@regFn AshValue.FALSE
        AshValue.of(applyHedgeMazeMode(mode))
    }
}
