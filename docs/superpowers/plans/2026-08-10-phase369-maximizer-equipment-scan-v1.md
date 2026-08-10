# Phase 369 — Maximizer equipment DB scan v1

## Context

Phase 368 delivered automatic bucket pins (`MaximizerAutoContext` + `mergeBuckets`). Mobile still built maximizer candidates only from owned locations + creatable concoctions + fold peers. Desktop `Evaluator.enumerateEquipment` walks every equipment item via `EquipmentDatabase.nextEquipmentItemId`, allowing mall/NPC/creatable channels to surface unowned buyable gear when `maximizerPriceLevel` is active.

## Goal

Match desktop candidate breadth on the current mobile acquisition stack: unowned equipment with mall/NPC/creatable/pull channels (and Phase 368 automatic pins) can appear in ranked buckets when a maximize spec is present.

## Deliverables

1. **`ItemDatabase.maxItemId()`** + **`EquipmentDatabase.nextEquipmentItemId`/`allEquipmentItemIds`/`contains`**
2. **`MaximizerManager.buildCandidateIds`** — union full equipment DB scan when `spec != null`
3. **`MaximizerCheckedItemBuilder`** — thread `MaximizerPriceLevel` into mall historical 2× budget gate (`DONT_CHECK` skips gate; `BUYABLE_ONLY`/`ALL` require historical `< budget * 2`)
4. **`prefetchMallPrices`** — prefetch buyable equipment-scan subset when `priceLevel != DONT_CHECK`
5. **Tests** — `EquipmentDatabaseTest`, `MaximizerManagerTest`, `MaximizerCheckedItemTest` priceLevel cases, `MaximizerEquipmentEnumeratorTest` mall-buyable unowned hat
6. **REVISION `phase369`**, AshP bulk update, parity-audit Phase 369 entry (~85%, ~4,847 tests)

## Deferred (Phase 370+)

- Synergy pair / triple-accessory post-compare replacement
- Desktop `tryOutfits` speculation pass + outfit combo scoring
- Familiar-carried full-evaluator scoring during enumeration
- Desktop `checkEquipment` outfit/equip constraint gates on final loadout
- `PriceLevel.ALL` emit-time owned-tradeable mall re-check
- Special automatic cases: modeables, card sleeve, garbage shirt/champagne, `-current` handling
- Full `EquipmentManager.canEquip` path/hardcore gates during scan
