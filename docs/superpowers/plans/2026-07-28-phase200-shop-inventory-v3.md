# Phase 200: AshP195 Shop Inventory v3 + AshP196 Validate v31

## Summary

Closed the last Phase 199 shop-parser dedupe deferral: `FlowerTradeinSync` now uses shared `ShopRowParser.parseSingleCostRows` with inverted Chroner-for-flowers mapping, plus explicit flowertradein visit-overlay validate regression and corpus coverage.

## Delivered

- **`FlowerTradeinSync` refactor** — `ShopRowParser.parseSingleCostRows()` + `mapRow()` inverted Chroner/flower mapping; `Chroner (N)` stack counts from item-name parens
- **`FlowerTradeinSyncTest`** — rose×2 cost, Chroner (16) stack, AshP167 regression, unknown currency null
- **`GameRuntimeLibraryAshP196Test`** — visit-overlay authority over bundled rows, visit hook validate, `hasTradeFlower` gate preserved
- **Corpus** — `corpus_flowertradeinVisitOverlayValidate_live`
- **`GameRuntimeLibrary.AshP195Batch.kt`** / **`AshP196Batch.kt`** — batch markers; `REVISION = phase200`

## Tests

- `FlowerTradeinSyncTest` (4 tests)
- `GameRuntimeLibraryAshP196Test` (4 tests)
- Corpus: `corpus_flowertradeinVisitOverlayValidate_live`
- **3,458 tests** (was 3,449 at Phase 199)

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
```

## Reference

Plan: `docs/superpowers/plans/2026-07-28-phase200-shop-inventory-v3.md` (from Cursor plan Phase 200)
