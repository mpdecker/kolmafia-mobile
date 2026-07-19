# Phase 85: AshP43 Jump Chance Library

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase85`

## Goal

Register monster-focused `jump_chance` (4 overloads) with desktop `initPenalty` + jump formula on live initiative/ML/mainstat.

## Deliverables

| Area | Change |
|------|--------|
| Math | `CombatAdjustment.initPenalty` / `monsterInitiativeWithMl` / `jumpChance` |
| ASH | AshP43 `jump_chance()` / `(monster)` / `(monster, init)` / `(monster, init, ml)` |
| Tests | Formula cases, AshP43, corpus |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- Location `jump_chance` overloads (min over zone)
- `MonsterStatusTracker` / Overclocked +200
- Expression `Init:` / missing-Init → −1
- `meat_drop` / `item_drops`, hit/miss APIs
- Queue-aware appearance rates, bastille/nonfilling, Maximizer Evaluator
