# Phase 97: AshP55 Expression Exp / base_mainstat_exp

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase97`

## Goal

Parse `Exp:` (numeric + expression), implement desktop-lite `MonsterData.getExperience()` (scale-implied / default / explicit), and expose `$monster[base_mainstat_exp]`. Include makeshift garbage shirt ×2 when charge remains.

## Deliverables

| Area | Change |
|------|--------|
| Model | `experience` / `experienceExpression` / `hasExperience` |
| Parse | `Exp:` via bracket-aware reader |
| Math | `CombatAdjustment.monsterExperience` |
| Bracket | `$monster[base_mainstat_exp]` FLOAT |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- Beeosity (non-1); BIG-core; `REDUCE_ENEMY_DEFENSE`
- Expression `MLMult:` / Phys/Elem resistance exprs
- Manuel; queue-aware `appearance_rates`; full `MonsterStatusTracker`
- Maximizer Evaluator
