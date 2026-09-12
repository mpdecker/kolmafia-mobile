package net.sourceforge.kolmafia.ash

/**
 * Phases 6671–6690 — ASH behavioral deepen XLVII Track A (mall / retrieve / buy / sell).
 *
 * - mall_price — validMallItem + fifth-cheapest / age / forceUpdate edges
 * - historical_price / historical_age — mallprices.txt day-gate + DB-only price
 * - retrieve_price / retrieve_item — specialty craft + meat-paste + mallPriceManager wire
 * - buy / buy_using_storage / sell — using-storage BOOLEAN residual
 * - npc_price — speakeasy / validate residual
 *
 * REVISION stays phase6850 until parent wraps to phase6850.
 */
internal fun GameRuntimeLibrary.registerPhase6671(scope: AshScope) {
    // Behavioral patches live in Pricing / Mall / MallPriceManager / MallPriceDatabase /
    // MallPurchaseRequest / MallManager / RetrievePricing / RetrieveItemService /
    // ItemActions autosell/sell.
}
