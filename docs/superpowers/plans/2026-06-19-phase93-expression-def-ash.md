# Phase 93: AshP51 Expression Def Evaluation

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase93`

## Goal

Evaluate monster `Def: [expression]` for `monster_defense` / `will_usually_miss` using the Phase 91–92 expression engine (no new tokens). No outer ML for expression path.

## Deliverables

| Area | Change |
|------|--------|
| Model | `MonsterDefinition.defenseExpression` |
| Parse | `Def: [expr]` → store inner expr |
| Math | `CombatAdjustment.resolveBaseDefense` / `monsterDefense` |
| Wire | AshP41 `monster_defense` + AshP46 `will_usually_miss` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- Expression HP
- Scale / Cap / Floor; REDUCE_ENEMY_DEFENSE; beeosity; BIG-core
- Live BL; dad_sea / unusual_construct; Manuel; queue rates; MonsterStatusTracker
- Maximizer Evaluator; bastille/nonfilling
