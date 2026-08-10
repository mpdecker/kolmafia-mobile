# Phase 368 — Maximizer automatic bucket pins v1

## Context

Phase 367 delivered global loadout gates (`totalMin`/`totalMax`, `exceeded`, failed-aware speculation). Desktop `Evaluator.enumerateEquipment` still marks synergy/outfit/hobo/brimstone/cloathing items `automaticFlag = true` and merges them before TOP_* score trimming — mobile dropped these low-scoring but strategically required items.

## Goal

Match desktop automatic inclusion for items that should never be dropped by per-slot limits, on the current candidate ID set (inventory/closet/storage/display/stash/creatable/fold peers).

## Deliverables

1. **`ModifierDatabase.kt`** — `getMaxCat()`, `synergyMaskByName()` + `rebuildSynergyMasks()` on load/inject
2. **`ModifierParser.kt`** — bare bitmap tokens (e.g. `Synergetic`) set value `1`
3. **`MaximizerAutoContext.kt`** — MaxCat/synergy/outfit usefulness + `shouldPinAutomatic()`
4. **`MaximizerRankedItem.automatic`** — wired in `MaximizerEquipmentEnumerator.enumerate` + `MaximizerManager.buildRankedBuckets`
5. **`mergeBuckets`** — pinned automatic items kept in addition to scored TOP_* limit
6. **Tests** — `MaximizerAutoContextTest` + enumerator hobo/outfit/synergy pin tests; `OutfitDatabase.resetForTest()` for isolation
7. **REVISION `phase368`**, AshP bulk update, parity-audit Phase 368 entry (~84%, ~4,839 tests)

## Deferred (Phase 369+)

- Full equipment DB scan (`EquipmentDatabase.nextEquipmentItemId`)
- Desktop `tryOutfits` speculation pass + outfit combo scoring
- Synergy pair / triple-accessory post-compare replacement
- Familiar-carried full-evaluator scoring during enumeration
- Desktop `checkEquipment` outfit/equip constraint gates on final loadout
- Special automatic cases: modeables, card sleeve, garbage shirt/champagne, `-current` handling
