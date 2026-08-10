# Phase 365 — Maximizer CheckedItem v1 (acquisition channels)

## Context

Phase 364 delivered `Evaluator.addFudge` and boolean/min constraint gates in ranked enumeration. Candidates were still filtered by flat `accessibleCount`, so creatable/fold/pull/buy-only equipment could not enter ranked buckets.

## Goal

Introduce desktop-shaped `MaximizerCheckedItem` acquisition channels and wire them into ranked-bucket enumeration so items with zero on-hand copies can rank when creatable, foldable, pullable, or buyable.

## Deliverables

1. **`MaximizerCheckedItem`** — data class with `initial`, `creatable`, `npcBuyable`, `mallBuyable`, `foldable`, `pullable`, `pullfoldable`, `pullBuyable`, optional `foldItemId`/`buyableFlag`, and desktop `totalCount()` sum
2. **`MaximizerCheckedItemBuilder`** — builds checked items from inventory snapshots, `MaximizeSpec`, prefs (`maximizerFoldables`, `maximizerNoAdventures`), `CreatableAmount`, `FoldGroupDatabase`, pull rules, NPC/mall heuristics; early exit when `initial >= 3`
3. **Ranked enumeration wiring** — `MaximizerRankedItem.checked`, `MaximizerEquipmentEnumerator` uses `checkedItem: (Int) -> MaximizerCheckedItem` and includes items when `totalCount() > 0`; `MaximizerManager.buildRankedBuckets` + creatable candidate scan + fold peer IDs; `MaximizerSpeculation` flat-ID overload updated
4. **Tests** — `MaximizerCheckedItemTest` (physical/creatable/fold/noAdventures/pullable); `MaximizerEquipmentEnumeratorTest` creatable-only bucket; `MaximizerManagerTest` speculate integration for creatable goal ranking
5. **REVISION `phase365`**, AshP bulk update, parity-audit Phase 365 entry (4,819 tests, Maximizer ~81%)

## Deferred (Phase 366+)

- Automatic outfit/synergy/hobo/brimstone/cloathing buckets
- Full equipment-DB iteration (`EquipmentDatabase.nextEquipmentItemId`)
- `CheckedItem.validate()` live mall-price refresh + pull-buy depth
- Global loadout gates (`exceeded`, `totalMin`/`totalMax`)
- Familiar-carried full-evaluator scoring
