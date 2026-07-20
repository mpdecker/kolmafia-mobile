# Phase 90: AshP48 Jump Chance Init Polish

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase90`

## Goal

Polish existing `jump_chance`: missing `Init:` → −1; Overclocked +200 vs Source Agent. No new ASH names.

## Deliverables

| Area | Change |
|------|--------|
| Model | `MonsterDefinition.hasInitiative` |
| Parse | `Init:` token tracking in `MonsterDatabase` |
| Math | `CombatAdjustment.jumpChance` / `locationJumpChance` |
| Wire | AshP43/44 pass Overclocked from skillManager |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- Expression `Init:` evaluation
- `dad_sea_monkee_weakness` / `unusual_construct_disc`
- Manuel APIs; queue-aware `appearance_rates`; full `MonsterStatusTracker`
- Maximizer Evaluator; bastille/nonfilling
