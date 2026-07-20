# Phase 100: AshP58 MLMult Effective Monster Level

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase100`

## Goal

Parse `MLMult:` and apply desktop `ML() = globalMl × evaluate(mlMult, 1)` inside combat Atk/Def/HP/XP paths so cave bars ×5, Thanksgiving `MLMult:0`, and Dinsey expression multipliers match desktop.

## Deliverables

| Area | Change |
|------|--------|
| Model | `mlMult` / `mlMultExpression` / `hasMlMult` on `MonsterDefinition` |
| Parse | `MLMult:` via `readNumOrExpr` |
| Eval | `effectiveMonsterLevel` wired into `monsterAttack`/`Defense`/`Hp`/`Experience` |
| ASH | Existing combat APIs pick up change; no new brackets |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- Beeosity (non-1) + Beecore; BIG-core
- Fight-time ML phys-res boost (`min(floor(ML()/2.5), 50)`)
- Manuel; queue-aware `appearance_rates`; full `MonsterStatusTracker`
- Maximizer Evaluator
- Missing raw_*/manuel `$monster` brackets
