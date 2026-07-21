# Phase 110: AshP68 `$monster[min_sprinkles]` + `$monster[max_sprinkles]`

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase110`

## Goal

Parse `SprinkleMin:` / `SprinkleMax:` from monsters.txt and wire MonsterProxy `min_sprinkles` and `max_sprinkles` brackets, completing MonsterProxy scalar INT field parity.

## Deliverables

| Area | Change |
|------|--------|
| Data model | `MonsterDefinition` min/max sprinkle fields + expressions |
| Parse | `MonsterDatabase` `SprinkleMin:` / `SprinkleMax:` via `readNumOrExpr()` |
| Bracket | `$monster[min_sprinkles]` / `$monster[max_sprinkles]` via `CombatAdjustment` |
| REVISION | `phase110` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- Runtime `random_modifiers` population (MonsterStatusTracker)
- AshP8–P18 interactive/PvP stub batches
- Maximizer Evaluator (Tier 2)
