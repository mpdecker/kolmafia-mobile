package net.sourceforge.kolmafia.ash

/**
 * AshP203 — shop inventory v7 (visitShopRows hooks + disabled coinmaster + logVisits).
 */
internal fun GameRuntimeLibrary.registerAshP203Batch(scope: AshScope) {
    // Batch marker for Phase 204 CoinmasterData.visitShopRows/isDisabled + ShopInventorySync hooks
    // + ShopRowDatabase.logVisits + armory/flowertradein/crimbo25_sammy applyVisitShopRows dedupe.
}
