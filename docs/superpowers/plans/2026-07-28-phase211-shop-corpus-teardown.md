# Phase 211: AshP217 Shop Hook Corpus v43 + AshP218 CoinmasterShopSync Teardown v44

**Revision:** `phase211`  
**Follows:** Phase 210 (`REVISION = phase210`, AshP215/AshP216)

## AshP217 — Shop hook corpus v43

End-to-end `AshCompatibilityCorpusTest` coverage for remaining AshP207 visit-hook shops:

| Corpus test | Hook / behavior |
|---|---|
| `corpus_trapperYakSkinValidate_live` | `TrapperSync` yeti-furs visit → `is_coinmaster_item(394, true)` |
| `corpus_latheVisitPref_live` | `SpinMasterLatheSync` sets `_spinmasterLatheVisited` |
| `corpus_junkmagazineHippyQuest_live` | `JunkMagazineSync` bumps HIPPY quest to `step2` |

Batch marker: `GameRuntimeLibrary.AshP217Batch.kt`

## AshP218 — CoinmasterShopSync teardown v44

- Extracted `CoinmasterPurchasePrefs.applyPurchasedItem()` from deleted `CoinmasterShopSync`
- `CoinmasterManager.buy()` calls `CoinmasterPurchasePrefs`
- Split tests: `CoinmasterPurchasePrefsTest.kt` (purchase prefs) + `ShopVisitHookTest.kt` (visit hooks)

Batch marker: `GameRuntimeLibrary.AshP218Batch.kt`

## Verification

`.\gradlew.bat :shared:jvmTest` — 3,538 tests
