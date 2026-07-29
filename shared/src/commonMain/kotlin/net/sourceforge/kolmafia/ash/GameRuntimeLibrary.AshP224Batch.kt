package net.sourceforge.kolmafia.ash

/**
 * AshP224 — ZapRequest HTTP + zap(item) ASH + zap CLI (registered in AshP223 batch + cliDispatch).
 */
internal fun GameRuntimeLibrary.registerAshP224Batch(scope: AshScope) {
    registerZapActionFunctions(scope)
}
