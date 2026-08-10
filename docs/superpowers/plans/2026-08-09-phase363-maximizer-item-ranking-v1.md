# Phase 363 — Maximizer item-ranking pipeline v1

## Context

Phase 362 delivered multi-weight primary scoring via `MaximizeSpec.evaluator`. Candidate discovery still scanned a flat `Set<Int>` per slot.

## Goal

Introduce desktop-style ranked equipment buckets with pseudo-slot routing for 1H/dual-wield weapons, wired into `MaximizerManager` and `MaximizerSpeculation`.

## Deliverables

1. **`MaximizerSlot` + `SlotList<T>`** — real + pseudo slots (`WEAPON_1H`, `OFFHAND_MELEE`, `OFFHAND_RANGED`)
2. **`MaximizerRankedItem` + `MaximizerEquipmentEnumerator`** — accessible-candidate enumerate v1 with sorted buckets
3. **`MaximizerManager`** — build buckets once; greedy/combo/speculate consume ranked lists
4. **`MaximizerSpeculation.topCandidatesPerSlot`** — ranked-bucket overload
5. **Tests** — slot/enumerator/manager +hands integration
6. **REVISION `phase363`**, AshP bulk update, parity-audit Phase 363 entry

## Deferred (Phase 364+)

- `Evaluator.addFudge()` cross-modifier weight propagation
- Evaluator constraint flags (`failed`/`exceeded`, `booleanMask`)
- Full desktop `CheckedItem` acquisition (mall/fold/pull/NPC buy)
- Full equipment-DB scan + automatic outfit/synergy buckets
- Familiar-carried scoring via full evaluator
- Dedicated `getCurrentML()` for experience precision
