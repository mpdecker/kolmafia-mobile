package net.sourceforge.kolmafia.ash

/**
 * Phases 6691–6710 — ASH behavioral deepen XLVII Track B (shop / create / creatable).
 *
 * - put_shop / take_shop / reprice_shop / get_shop / shop_amount — StoreManager batch/refresh
 * - sells_skill / sell_cost / sell_price — coinmaster skillBuyPrice residual
 * - daily_special / well_stocked — cafe item-id + mall listing polish
 * - create / craft — CreateItemRequest specialty fallthrough + mode/retrieve residual
 * - creatable_amount / creatable_turns / get_ingredients / concoction_price / craft_type —
 *   cache / free-craft / MANUAL residual
 *
 * REVISION stays phase6850 until parent wraps to phase6850.
 */
internal fun GameRuntimeLibrary.registerPhase6691(scope: AshScope) {
    // Behavioral patches live in ItemActions / Shop / AshP911 / AshP985 / Coinmaster /
    // CafeDailySpecialSync / CoinmasterData / CreatableTurns / CraftRequest /
    // ConcoctionQueue / AshP117 / AshP122.
}
