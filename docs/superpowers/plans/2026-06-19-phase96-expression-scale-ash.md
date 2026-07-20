# Phase 96: AshP54 Expression Scale / Cap / Floor

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase96`

## Goal

Evaluate `Scale:` / `Cap:` / `Floor:` bracket expressions for the ~41 monsters that still resolved Scale as 0 after AshP53. Add `equipped()` to the monster expression engine and fix space-spanning bracket parse (God Lobster / PARTY HARD).

## Deliverables

| Area | Change |
|------|--------|
| Model | `scaleExpression` / `capExpression` / `floorExpression` |
| Parse | Bracket-aware `readBracketOrToken` for Scale/Cap/Floor/Atk/Def/HP/Init |
| Expr | `equipped(item)` via `equippedItemNames` |
| Math | `resolveScaleParams` before `scaledAttack`/`Defense`/`Hp` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- Expression `Exp:` / `$monster[base_mainstat_exp]`
- Beeosity; BIG-core; `REDUCE_ENEMY_DEFENSE`
- Expression `MLMult:` / Phys/Elem resistance exprs
- Manuel; queue-aware `appearance_rates`; full `MonsterStatusTracker`
- Maximizer Evaluator
