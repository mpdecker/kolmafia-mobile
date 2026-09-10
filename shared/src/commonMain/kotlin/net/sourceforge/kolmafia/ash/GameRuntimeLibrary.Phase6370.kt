package net.sourceforge.kolmafia.ash

/**
 * Phases 6311–6370 — ASH behavioral deepen XLI (place-sync deepen + misc shop tokens).
 *
 * 6311–6330 CampAwaySync / FalloutShelterSync desktop deepen ·
 * 6331–6350 MiscShopTokenResponseParse (arcade / snack / FDKOL / Rubee / Beach Buck /
 * FunFunds / Wal-Mart) ·
 * 6351–6360 expected_damage / elemental_resistance MonsterStatusTracker overlay +
 * freeRestsRemaining desktop formula ·
 * 6361–6370 visit-hook wiring + corpus + revision bump
 */
internal fun GameRuntimeLibrary.registerPhase6370(scope: AshScope) {
    // Behavioral patches live in CampAwaySync / FalloutShelterSync /
    // MiscShopTokenResponseParse / AshP39 / CampgroundSync / processVisitResponseHooks.
}
