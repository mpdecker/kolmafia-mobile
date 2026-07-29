# Phase 209: AshP213 Shoprows Write-back + Skill ASH + AshP214 Validate v40

**Delivered:** 2026-07-28 (3,525 tests)

## AshP213 — Shoprows write-back + skill ASH

- `ShopRowDatabase`: `LEARNED_SHOPROWS_KEY`, `restoreLearnedRows`, `persistLearnedRow` (cross-session visit-learned row persistence)
- `SessionManager.login`: restore learned rows after `gameDatabase.load()`
- `ShopInventorySync`: persist new visit-learned rows when `prefs != null`
- `GameRuntimeLibrary.AshP213Batch`: public `is_coinmaster_skill(sk|id[, validate])` delegating to `CoinmasterDatabase.containsBuySkill`
- Removed no-op `CoinmasterShopSync.apply` from `processVisitResponseHooks`; `CoinmasterManager.visit` uses `ShopInventorySync.parseAndLearn` for shop.php visits
- Moved `consequences.txt` and `cultshorts.txt` from unwired to wired in parity audit bundled-data table

## AshP214 — Validate v40

- `GameRuntimeLibraryAshP214Test`: revision pin, `is_coinmaster_skill` validate before/after visit + empty overlay, `hasSkill` gate, learnedShopRows restore
- Extended `corpus_skillShopVisitOverlay_live` with `is_coinmaster_skill(6027, true)` assertions
- `GameRuntimeLibrary.AshP214Batch` marker
- `REVISION = phase209`

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
```
