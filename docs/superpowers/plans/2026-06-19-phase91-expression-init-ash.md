# Phase 91: AshP49 Expression Init Evaluation

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase91`

## Goal

Evaluate monster `Init: [expression]` for `monster_initiative` / `jump_chance` using live `pref()` plus `KW`/`KV`/`KC` dread-kiss tokens — fixing Source Agent and other expression-Init monsters without porting full Atk/Def/HP `MonsterExpression`.

## Deliverables

| Area | Change |
|------|--------|
| Model | `MonsterDefinition.initiativeExpression` |
| Parse | `Init: [expr]` → store inner expr, `hasInitiative=true` |
| Engine | Live `pref()` + `KW`/`KV`/`KC` in `ModifierExpression` / `ExpressionContext` |
| Math | `CombatAdjustment.resolveBaseInitiative` before ML/jump |
| Wire | AshP41/43/44 via `buildMonsterExpressionContext()` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- Expression Atk/Def/HP (MUS/MOX/ML/…)
- `dad_sea_monkee_weakness` / `unusual_construct_disc`
- Manuel; queue-aware `appearance_rates`; full `MonsterStatusTracker`
- Maximizer Evaluator; bastille/nonfilling
