# Phase 364 — Maximizer Evaluator v4 (addFudge + constraint flags)

## Context

Phase 363 delivered ranked equipment buckets. Scoring still lacked desktop `Evaluator.addFudge` cross-modifier weight propagation and boolean/min constraint checking.

## Goal

Port desktop `addFudge`, `booleanMask`/`booleanValue`, `checkConstraints`, and `failed` into `Evaluator.kt`; wire constraint checks into `MaximizerEquipmentEnumerator`.

## Deliverables

1. **`Evaluator.addFudge`** — four desktop fudge groups at parse tail (`weight * 0.0001` propagation)
2. **Constraint flags** — `booleanMask`/`booleanValue`, `Constraint` enum, `applyBooleanConstraints`, `checkConstraints`, `failed` on min violation in `getScore`
3. **`MaximizeGoal.parseSpec`** — sync boolean constraints into evaluator before returning spec
4. **`MaximizerEquipmentEnumerator`** — skip items where `checkConstraints == VIOLATES`
5. **Tests** — `EvaluatorTest` fudge/constraint/failed cases; `MaximizerEquipmentEnumeratorTest` forbidden-boolean skip
6. **REVISION `phase364`**, AshP bulk update, parity-audit Phase 364 entry

## Deferred (Phase 365+)

- `MaximizerCheckedItem` acquisition channels (mall buy, fold, pull, NPC buyable, creatable counts)
- Full equipment-DB iteration + automatic outfit/synergy/hobo buckets
- `exceeded` / global `totalMin`/`totalMax` loadout gates
- Familiar-carried scoring via full evaluator (not `spec.primary`)
- Dedicated `getCurrentML()` for experience precision
