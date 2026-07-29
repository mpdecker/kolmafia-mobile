# Phase 208: AshP211 Shop Inventory v11 + AshP212 Validate v39

**Delivered:** 2026-07-28

## AshP211 — Shop inventory v11

- Extracted `applyVisitShop` on `ReplicaMrStoreSync`, `BlackMarketShopSync`, `PirateRealmShopSync`, `ChronerShopSync`, `SwaggerShopSync`
- Extended `Crimbo23ShopSync.applyVisitShop` (buy-action skip + shopId from URL)
- Moved swagger season/overlay constants from `CoinmasterShopSync` to `SwaggerShopSync`
- `CoinmasterDatabase.enrichWithVisitHooks`: mrreplica, blackmarket, piraterealm, chroner shops (except conmerch), crimbo23_* prefix, swagger
- `CoinmasterShopSync.apply`: all remaining pref-sync branches deduped to no-op comments (purchase checkpoints unchanged)
- Wired `SwaggerShopSync.applyVisitShop` from `GameRuntimeLibrary.processVisitResponseHooks` (peevpee) and `CoinmasterManager.visit`

## AshP212 — Validate v39

- Hook-driven validate regression for mrreplica year gate (11325 vs 11190), blackmarket MACGUFFIN, piraterealm Fun-a-log, swagger booty, alliedhq flak shield
- Updated five corpus probes to `processVisitResponseHooks`
- Updated AshP154/156/160 and AshP153/155/159 hook tests for coinmaster DB registration
- `GameRuntimeLibraryAshP212Test`, AshP211/AshP212 batch markers
- `REVISION = phase208` (3,518 tests)

## Deferred (Phase 209+)

- Disk write-back of learned rows to `shoprows.txt`
- Public `is_coinmaster_skill` ASH
- `consequences.txt` wiring
- Removing redundant trailing `CoinmasterShopSync.apply()` from `processVisitResponseHooks`
