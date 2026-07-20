# Phase 86: AshP44 Location Jump Chance

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase86`

## Goal

Complete desktop `jump_chance` with three location overloads: min over positive-weight zone monsters via AshP43 formula + AshP38 combat data.

## Deliverables

| Area | Change |
|------|--------|
| Math | `CombatAdjustment.locationJumpChance` |
| ASH | AshP44 `jump_chance(location)` / `(location, init)` / `(location, init, ml)` |
| Tests | Zone-min cases, AshP44, corpus |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- `MonsterStatusTracker` / Overclocked +200
- Expression `Init:` / missing-Init → −1
- `meat_drop` / `item_drops` / `item_drops_array`
- `will_usually_miss` / `will_usually_dodge`
- Queue-aware appearance rates, bastille/nonfilling, Maximizer Evaluator
