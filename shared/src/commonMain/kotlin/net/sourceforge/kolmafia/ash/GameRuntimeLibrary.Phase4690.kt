package net.sourceforge.kolmafia.ash

/**
 * Phases 4631–4690 — ASH behavioral deepen XIII + HTTP request residual hubs.
 *
 * 4631–4640 buy/retrieve_item overload floor (boolean 2-arg, int 3-arg, item-first) ·
 * 4641–4650 retrieve_price count/exact · closet meat put/take ·
 * 4651–4660 put_shop full-inventory 3-arg · take_shop(item) · reprice 2-arg · shop auto-refresh ·
 * 4661–4670 consume/transfer overload floor (use/eat/drink/chew/autosell/closet/display/stash) ·
 * 4671–4690 UseSkill/WildfireCamp/Artist/AltarOfLiteracy/Dreadsylvania/
 * Pantogram/Mummery/BurningLeaves/SausageOMatic/FantasyRealm/GnomePart hubs
 */
internal fun GameRuntimeLibrary.registerPhase4690(scope: AshScope) {
    // Behavioral patches live in Mall/ItemActions/Pricing/Shop/ClosetRequest.
    // Request hubs are typed objects wired from processVisitResponseHooks.
}
