# Phase 207: AshP209 Shop Inventory v10 + AshP210 Validate v38

**Delivered:** 2026-07-28

## AshP209 — Shop inventory v10

- Extracted `applyVisitShop` on `BaconShopSync`, `ArcadeShopSync`, `KiwiShopSync`, `MysticShopSync`, `ShoreShopSync`, `FiveDPrinterShopSync`
- Moved `MYSTIC_PSYCHOSIS_ITEMS_UNLOCKED` to `MysticShopSync`; updated `CoinmasterPurchaseAccessibility`
- `CoinmasterDatabase.enrichWithVisitHooks`: registered all six shop IDs
- `CoinmasterShopSync`: deduped 5dprinter/bacon/arcade/kiwi/mystic/shore branches (purchase checkpoints remain)

## AshP210 — Validate v38

- Hook-driven mystic pixel pill (5906) validate via `processVisitResponseHooks`
- Hook-driven shore cheap toaster (637) validate via visit hook clearing `itemBoughtPerAscension637`
- Updated `corpus_mysticPsychosisPixelValidate_live` and `corpus_shoreToasterCoinmasterValidate_live` to unified hook path
- `GameRuntimeLibraryAshP210Test`, AshP209/AshP210 batch markers
- `REVISION = phase207` (3,507 tests)

## Deferred (Phase 208+)

- Disk write-back of learned rows to `shoprows.txt`
- Public `is_coinmaster_skill` ASH
- `consequences.txt` wiring
- Remaining `CoinmasterShopSync` shops: `mrreplica`, `blackmarket`, `piraterealm`, chroner tower shop IDs, `crimbo23_*`, swagger overlay (`applySwaggerVisit`)
