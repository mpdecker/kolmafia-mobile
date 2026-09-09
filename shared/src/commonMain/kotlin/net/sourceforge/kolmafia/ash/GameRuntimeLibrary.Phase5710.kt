package net.sourceforge.kolmafia.ash

/**
 * Phases 5651–5710 — ASH behavioral deepen XXX + liberateKing deepen + place.php residual.
 *
 * 5651–5695 DynamicChoiceSpoilers full matrix (remaining dynamicChoiceOptions) ·
 * 5696–5706 liberateKing deferred-refresh pref flags ·
 * 5707–5709 spoiler helper DI (bonus/prismatic damage, pool skill, Rufus labyrinth) ·
 * 5710 RequestLogger place.php residual + parity recount
 */
internal fun GameRuntimeLibrary.registerPhase5710(scope: AshScope) {
    // Behavioral patches live in DynamicChoiceSpoilers / KoLCharacter.liberateKing /
    // RequestLogger.placeVisitMessage; HTTP shop residual closed at 5650.
}
