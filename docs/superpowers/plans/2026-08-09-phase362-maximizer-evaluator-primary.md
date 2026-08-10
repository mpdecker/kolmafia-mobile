# Phase 362 — Maximizer Evaluator v2 (multi-weight primary scoring)

## Context

Phase 361 delivered `Evaluator.kt` with weighted parse, desktop TIEBREAKER tie-breaking, and `getScore(CurrentModifiers)` special cases. Primary loadout scoring still used single `MaximizeSpec.primary`.

## Goal

Wire multi-weight stat goals like `"2 item, 1 meat"` into primary Maximizer scoring throughout `MaximizerManager` and `MaximizerSpeculation`.

## Deliverables

1. **`MaximizeSpec.evaluator`** — stat vs constraint split in `MaximizeGoal.parseSpec`; `primary` = `highestWeightedStat()` for thrall/familiar helpers
2. **`Evaluator.getItemContribution`** — weighted item modifier sum for candidate ranking (no +100 meat/item baselines)
3. **`Evaluator.highestWeightedStat()`** — tie order ITEMDROP > MEATDROP > MUS > MYS > MOX
4. **`MaximizerSpeculation.scoreLoadout`** — uses `evaluator.getScore(CurrentModifiers)` instead of single-modifier path
5. **`MaximizerManager.scoreItem`** — uses `evaluator.getItemContribution(ModifierParser.parse(...))`
6. **Tests** — MaximizeGoal, Evaluator, MaximizerSpeculation, MaximizerManager multi-weight coverage
7. **REVISION `phase362`**, AshP bulk update, parity-audit Phase 362 entry

## Deferred (Phase 363+)

- Evaluator item-ranking pipeline (pseudo-slots, 1H weapon ranking, desktop `enumerateEquipment`)
- `addFudge` cross-modifier weight propagation
- Full familiar/thrall scoring via evaluator (not `spec.primary`)
- Dedicated `getCurrentML()` for experience precision
- Evaluator constraint flags (`failed`/`exceeded`, booleanMask min constraints)
