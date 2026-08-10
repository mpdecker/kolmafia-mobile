# Phase 370 — Maximizer synergy post-compare v1

## Context

Phase 368 pins synergy/outfit/category items via `MaximizerAutoContext` + `mergeBuckets`. Phase 369 expanded candidates via full equipment DB scan. Desktop still post-compares two-item synergies (and reinstates triple-accessory MMMM sets) against best individual slot items before ranked buckets are consumed.

## Goal

Match desktop behavior where synergy items are initially pinned automatic, then un-pinned if wearing the pair together does not beat the best individual items in those slots. Reinstates automatic pins for the two hardcoded 3-item accessory sets when the trio beats the top three accessories.

## Deliverables

1. **`MaximizerRankedItem.automatic`** → mutable `var`
2. **`MaximizerEquipmentEnumerator.setAutomaticByName`** — bucket-wide automatic flag updates
3. **`MaximizerSynergyAdjustments.kt`** + **`MaximizerSynergyItemIds.kt`** — two-item clear + triple-accessory reinstate via `MaximizerSpeculation.scoreLoadout`
4. **`MaximizerManager.buildRankedBuckets`** — call `MaximizerSynergyAdjustments.apply` after enumerate
5. **Tests** — `MaximizerSynergyAdjustmentsTest` + `MaximizerEquipmentEnumeratorTest` integration
6. **REVISION `phase370`**, AshP bulk update, parity-audit Phase 370 entry (~86%, ~4,852 tests)

## Deferred (Phase 371+)

- Outfit post-compare automatic clearing (`Evaluator.java:2064–2130`)
- Desktop `tryOutfits` speculation pass + outfit combo scoring
- Familiar-carried full-evaluator scoring during enumeration
- Desktop `checkEquipment` outfit/equip constraint gates on final loadout
- `PriceLevel.ALL` emit-time owned-tradeable mall re-check
- Special automatic cases: modeables, card sleeve, garbage shirt/champagne, `-current` handling
- Full `conditionalFlag` parity in synergy compare baselines
