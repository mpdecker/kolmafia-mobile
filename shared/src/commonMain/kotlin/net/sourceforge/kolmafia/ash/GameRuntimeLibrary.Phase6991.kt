package net.sourceforge.kolmafia.ash

/**
 * Phases 6991–7010 — HTTP Residual LII Track B
 * (CoinmasterManager effect/quest DI + Traveling Trader visit inventory).
 *
 * - CoinmasterManager hasEffect / generatorQuestFinished wiring
 * - TravelingTraderRequest dynamic buy-row parse + Crimbo11/14/16/Cartel parseResponse
 *
 * Parent LII wrap bumped REVISION to phase7030; tip now phase7090.
 */
internal fun GameRuntimeLibrary.registerPhase6991(scope: AshScope) {
    // Behavioral patches live in CoinmasterManager / TravelingTraderRequest.
}
