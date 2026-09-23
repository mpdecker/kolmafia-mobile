package net.sourceforge.kolmafia.ash

/**
 * Phases 7511–7570 — MANUAL concoction edges residual
 * (yield / SX3 tripleReagent / MANUAL loader skip / recipe CLI).
 * Parent wrap bumps REVISION to phase7570.
 */
internal fun GameRuntimeLibrary.registerPhase7511(scope: AshScope) {
    // Behavioral patches live in ConcoctionYield / ConcoctionDatabase parse /
    // ConcoctionCreatable / CreateItemIngredients / RetrievePricing / recipe CLI.
}
