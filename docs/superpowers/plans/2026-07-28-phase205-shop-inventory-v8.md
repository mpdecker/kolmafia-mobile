# Phase 205: AshP205 Shop Inventory v8 + AshP206 Validate v36

**Delivered:** 2026-07-28

## AshP205 — Shop inventory v8

- `CoinmasterData`: optional `visitShop(html, prefs, sessionLogger)` HTML hook
- `ShopInventorySync`: invokes `visitShop` after `visitShopRows`; optional `prefs` param; `classifyRow` skips legacy buy/sell when `isDisabled`
- `MerchTableSync`: `applyVisitShopRows` + `applyVisitShop` (TimeTower + Mr.A/Chroner token prefs); `syncFromShopHtml` delegates to helpers
- `CoinmasterDatabase.enrichWithVisitHooks`: conmerch registers both hooks
- `CoinmasterShopSync`: conmerch row parse removed (hook-driven via `ShopInventorySync`)

## AshP206 — Validate v36

- Conmerch bundled tattoo denied until visit overlay; passes after `ShopInventorySync` visit + `timeTowerAvailable` + Chroner tokens
- Empty overlay clears validate
- `GameRuntimeLibraryAshP206Test`, `corpus_conmerchVisitOverlayValidate_live`, AshP205/AshP206 batch markers
- `REVISION = phase205`

## Deferred (Phase 206+)

- Disk write-back of learned rows to `shoprows.txt`
- Public `is_coinmaster_skill` ASH
- `consequences.txt` wiring
- Migrating remaining desktop `withVisitShop` coinmasters (Trapper, SeptEmber, DripArmory, etc.)
