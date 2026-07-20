# Phase 92: AshP50 Expression Atk Evaluation

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase92`

## Goal

Evaluate monster `Atk: [expression]` for `monster_attack` / `jump_chance` using MUS/MOX/ML/BL/HP/STAT/MCD plus Phase 91 `pref`/`KW`/`KV`/`KC` — without outer ML double-counting. Def/HP deferred.

## Deliverables

| Area | Change |
|------|--------|
| Model | `MonsterDefinition.attackExpression` |
| Parse | `Atk: [expr]` → store inner expr |
| Engine | MUS/MOX/MYS/ML/MCD/BL/HP/STAT in `ExpressionContext` |
| Math | `CombatAdjustment.resolveBaseAttack` / `monsterAttack` (expr: max(1,eval), no outer ML) |
| Wire | AshP41 + AshP46 dodge; enriched `buildMonsterExpressionContext()` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- Expression Def / HP
- Scale / Cap / Floor; equipped(); beeosity; BIG-core; REDUCE_ENEMY_DEFENSE
- Live BL (basement)
- dad_sea / unusual_construct; Manuel; queue rates; MonsterStatusTracker
- Maximizer Evaluator; bastille/nonfilling
