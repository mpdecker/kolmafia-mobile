# Phase 204: AshP203 Shop Inventory v7 + AshP204 Validate v35

**Delivered:** 2026-07-28

## AshP203 — Shop inventory v7

- `CoinmasterData`: optional `visitShopRows` hook, `isDisabled` + `setDisabledForTest`, `hasShopRowInventory()`
- `ShopInventorySync`: invokes `visitShopRows` before row loop; disabled skips bundled row dedupe; `newStyle` includes disabled + shop-row coinmasters; `isVisitOnly` + `"Visiting $shopName"` session log when `ShopRowDatabase.logVisits`
- `ShopRowDatabase`: `setLogVisits` / `logVisits`; auto-registered from `CoinmasterDatabase.register` for shop.php coinmasters
- Refactored `ArmoryAndLeggerySync`, `FlowerTradeinSync`, `Crimbo25SammySync` → `applyVisitShopRows`; removed duplicate row-parse paths from `CoinmasterShopSync`
- `CoinmasterDatabase.findBuyRowForItem` / `findBuyRowForSkill` skip bundled rows when `isDisabled`

## AshP204 — Validate v35

- Disabled coinmaster: bundled `is_coinmaster_item(validate=true)` denied until visit overlay populated
- `flowertradein` dynamic overlay authority via unified `ShopInventorySync` `visitShopRows` hook (no `CoinmasterShopSync` row path)
- `GameRuntimeLibraryAshP204Test`, `corpus_disabledCoinmasterVisitOverlay_live`, AshP203/AshP204 batch markers
- `REVISION = phase204`

## Deferred (Phase 205+)

- Disk write-back of learned rows to `shoprows.txt`
- Public `is_coinmaster_skill` ASH
- `consequences.txt` wiring
- Full `CoinmasterData.visitShop(responseText)` HTML-only hook surface
