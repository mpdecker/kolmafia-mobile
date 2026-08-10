# Phase 367 — Maximizer global loadout gates v1

## Context

Phase 366 delivered `MaximizerCheckedItem.validate()` with post-prefetch mall/meat gates. Desktop global loadout constraints (`totalMin`/`totalMax`, `exceeded`, failed-aware speculation, maximize rejection) remained unported.

## Goal

Match desktop `Evaluator` standalone `min`/`max` parse + global score gates, `MaximizerSpeculation` failed-aware comparison + `exceeded` early exit, and `MaximizerManager` rejection when the best loadout fails constraints.

## Deliverables

1. **`Evaluator.kt` v5** — `totalMin`/`totalMax` parse when no prior modifier term; `exceeded` flag; `getScore` sets `failed` when `score < totalMin` and `exceeded` when `score >= totalMax`
2. **`MaximizerSpeculation.kt`** — `isBetterLoadout` prefers non-`failed` loadouts; leaf handler skips failed candidates; `stopSearch` on `exceeded`
3. **`MaximizerManager.kt`** — `buildMaximizePlan` returns no-improvement plan when final `evaluator.failed`
4. **Tests** — `EvaluatorTest` standalone min/max + global gate flags; `MaximizerSpeculationTest` failed preference + exceeded early exit; `MaximizerManagerTest.maximize_rejectsFailedConstraintGoal`
5. **REVISION `phase367`**, AshP bulk update, parity-audit Phase 367 entry (~83%, ~4,831 tests)

## Deferred (Phase 368+)

- Full equipment DB scan (`EquipmentDatabase.nextEquipmentItemId`)
- Automatic outfit/synergy/hobo/brimstone/cloathing buckets
- `MaximizerPriceLevel.ALL` / `BUYABLE_ONLY` enumeration depth during candidate scan
- Familiar-carried full-evaluator scoring
- Desktop `checkEquipment` outfit/equip constraint gates on loadout
