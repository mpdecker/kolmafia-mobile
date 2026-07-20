# Phase 94: AshP52 Expression HP Evaluation

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase94`

## Goal

Evaluate monster `HP: [expression]` for `monster_hp` using the Phase 91–93 expression engine (no new tokens; character `HP` already wired). Completes Init/Atk/Def/HP expression-stat quartet. No outer ML for expression path.

## Deliverables

| Area | Change |
|------|--------|
| Model | `MonsterDefinition.hpExpression` |
| Parse | `HP: [expr]` → store inner expr |
| Math | `CombatAdjustment.resolveBaseHp` / `monsterHp` |
| Wire | AshP41 `monster_hp` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- Scale / Cap / Floor; beeosity; BIG-core; REDUCE_ENEMY_DEFENSE
- Live BL; expression Exp:; dad_sea / unusual_construct
- Manuel; queue rates; MonsterStatusTracker; Maximizer Evaluator
