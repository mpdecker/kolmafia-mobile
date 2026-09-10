package net.sourceforge.kolmafia.ash

/**
 * Phases 6571–6590 — ASH behavioral deepen XLV Track B (collections / shop).
 *
 * - get_no_pulls → desktop KoLConstants.nopulls via CACHED_NOPULLS
 * - *_amount lazy refresh when collection never retrieved
 * - available_amount AccessibleItemCount gates + INT id resolve
 * - put_shop / get_shop / refresh_shop ManageStore coupling
 * - put/take display & stash batch cache edges
 * - daily_special live cafe visit sync
 * - well_stocked mall-only listing depth
 *
 * REVISION stays phase6670 (do not bump).
 */
internal fun GameRuntimeLibrary.registerPhase6571(scope: AshScope) {
    // Behavioral patches live in Phase4490 / Collections / AccessibleItemCount /
    // ItemActions / AshP911TrackD / AshP985TrackQ / Shop / CafeDailySpecialSync /
    // StoragePullRules / CollectionCacheSync.
}
