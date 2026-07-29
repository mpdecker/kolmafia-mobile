# Phase 210: AshP215 Shop Hook Corpus + AshP216 Cleanup/Validate v41

**Revision:** `phase210`  
**Follows:** Phase 209 (`REVISION = phase209`, AshP213/AshP214)

## AshP215 — Shop hook corpus v41

End-to-end `AshCompatibilityCorpusTest` coverage for AshP205–214 visit-hook shops:

| Corpus test | Hook / behavior |
|---|---|
| `corpus_arcadeLockedItemValidate_live` | `ArcadeShopSync` clears `lockedItem*` → `is_coinmaster_item(4637, true)` |
| `corpus_kiwiSpiritsValidate_live` | `KiwiShopSync` empty visit marks spirits bought → validate deny |
| `corpus_fiveDPrinterUnknownRecipeValidate_live` | `FiveDPrinterShopSync` descitem visit clears `unknownRecipe*` |
| `corpus_learnedShopRowsRestoreValidate_live` | `restoreLearnedRows` pref → `is_coinmaster_item` validate after relog |
| `corpus_septemberEmberVisitSync_live` | `SeptEmberSync` ember balance prefs |
| `corpus_chronerTowerVisitHook_live` | `ChronerShopSync`/`TimeTowerSync` `timeTowerAvailable` toggle |

Batch marker: `GameRuntimeLibrary.AshP215Batch.kt`

## AshP216 — Cleanup/validate v42

- Deleted dead `CoinmasterShopSync.apply()` (zero callers; visit flow uses `ShopInventorySync` hooks)
- Kept `CoinmasterShopSync.applyPurchasedItem()` for purchase checkpoint prefs
- `ShopRowDatabase.restoreLearnedRows()` rebuilds `CoinmasterVisitInventory` visit overlays on login

Batch marker: `GameRuntimeLibrary.AshP216Batch.kt`

## Verification

`.\gradlew.bat :shared:jvmTest` — 3,533 tests
