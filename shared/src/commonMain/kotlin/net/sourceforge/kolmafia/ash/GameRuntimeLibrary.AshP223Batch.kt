package net.sourceforge.kolmafia.ash

/**
 * AshP223 — get_zap_wand ASH (inventory wand discovery via WandDiscovery.findWand).
 */
internal fun GameRuntimeLibrary.registerAshP223Batch(scope: AshScope) {
    registerZapWandFunctions(scope)
}
