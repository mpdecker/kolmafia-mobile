package net.sourceforge.kolmafia.ash

/**
 * Phases 4811–4870 — ASH behavioral deepen XVI + thin HTTP residual hubs.
 *
 * 4811–4825 mall buy inventory-delta / mall_prices polish ·
 * 4826–4840 display/stash item↔count + put_shop refresh + collection cache write-back ·
 * 4841–4855 coinmaster sell BOOLEAN + buy delta + craft qty≤0 ·
 * 4856–4870 Crimbo09/10 + CombineMeat hubs + revision/audit
 */
internal fun GameRuntimeLibrary.registerPhase4870(scope: AshScope) {
    // Behavioral patches live in Mall / Coinmaster / ItemActions / Collections / AshP985.
    // Request hubs are typed objects wired from processVisitResponseHooks.
}
