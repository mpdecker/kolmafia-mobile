# Phase 366 — Maximizer CheckedItem validate v1

## Context

Phase 365 delivered `MaximizerCheckedItem` acquisition channels wired into ranked enumeration. Buy candidates still used historical-price heuristics only; `pullBuyable` was stubbed at 0; no post-prefetch mall price or meat budget validation.

## Goal

Add desktop-shaped `CheckedItem.validate()` with post-prefetch mall price checks, populate `pullBuyable` in the builder, gate mall channels via `ItemAvailability`, and fix prefetch ordering so ranked buckets see cached mall prices before greedy slot search.

## Deliverables

1. **`MaximizerPriceLevel`** — `DONT_CHECK`/`BUYABLE_ONLY`/`ALL` + `byIndex(maximizerPriceLevel pref)`
2. **`MaximizerCheckedItem.validate()`** — clears `mallBuyable`/`pullBuyable` when cached price invalid, above max, or exceeds available/storage meat
3. **`MallPriceManager.getMallPrice()`** — alias for cached post-prefetch price
4. **`ItemAvailability.canUseMallToStorage`** — tradeable + `autoSatisfyWithMall` + limit-mode gate
5. **`MaximizerCheckedItemBuilder` v2** — mall buy gated by `canUseMall`; mall heuristic uses `min(maxPrice, meat)`; `pullBuyable` when pull path + `canUseMallToStorage` + zero other channels + storage-meat budget heuristic
6. **`MaximizerManager` wiring** — `prefetchMallPrices` before `findBestPerSlot`; `validate()` in `buildRankedBuckets` checked-item lambda
7. **Tests** — validate meat/price gates, pullBuyable builder, mall pref gate (`MaximizerCheckedItemTest`)
8. **REVISION `phase366`**, AshP bulk update, parity-audit Phase 366 entry (4,825 tests, Maximizer ~82%)

## Deferred (Phase 367+)

- Global loadout gates (`exceeded`, `totalMin`/`totalMax`)
- Full equipment DB scan (`EquipmentDatabase.nextEquipmentItemId`)
- Automatic outfit/synergy/hobo/brimstone/cloathing buckets
- Familiar-carried full-evaluator scoring
- `PriceLevel.ALL` vs `BUYABLE_ONLY` enumeration depth during candidate scan
