package net.sourceforge.kolmafia.ash

/**
 * Phases 6131–6190 — ASH behavioral deepen XXXVIII (HTTP residual + ASH long-tail).
 *
 * 6131–6145 ApiRequest what=item registerItem + form_fields URL decode ·
 * 6146–6160 set_auto_attack CLI/pref, eudora boolean, to_plural(INT), uneffect BOOLEAN ·
 * 6161–6175 shop validate overloads (npc/coinmaster/skill) ·
 * 6176–6190 craft thin-hub parseResponse (sugarsheets/starchart/nugglet/sewer/clipart +
 * wax/meteoroid/newspaper/wool) + corpus + parity recount
 */
internal fun GameRuntimeLibrary.registerPhase6190(scope: AshScope) {
    // Behavioral patches live in ApiRequest / ItemDatabase /
    // AshP943/950/121/132/138/213 / Uneffect / HttpResidualRequestHubsPhase4930+4990+5050 /
    // GameRuntimeLibrary.processVisitResponseHooks.
}
