package net.sourceforge.kolmafia.ash

/**
 * Phases 6191–6250 — ASH behavioral deepen XXXIX (Crimbo/craft residual + ASH depth).
 *
 * 6191–6205 item_drops_array MonsterStatusTracker overlay + autosell response prefs ·
 * 6206–6220 CrimboHubResponseParse seasonal token/craft prefs ·
 * 6221–6235 CraftThinHubResponseParse + AutoSellRequestHub.parseResponse ·
 * 6236–6250 visit-hook wiring + corpus + parity recount
 */
internal fun GameRuntimeLibrary.registerPhase6250(scope: AshScope) {
    // Behavioral patches live in AshP45 / HttpResidualRequestHubsPhase4990+6250 /
    // GameRuntimeLibrary.processVisitResponseHooks.
}
