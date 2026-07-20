# Phase 98: AshP56 REDUCE_ENEMY_DEFENSE on monsterDefense

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase98`

## Goal

Apply existing `Reduce Enemy Defense` modifier to all `monster_defense` paths (integer, Scale, expression) and `will_usually_miss`, matching desktop `MonsterData.getDefense`.

## Deliverables

| Area | Change |
|------|--------|
| Math | `applyEnemyDefenseReduce` / `reduceEnemyDefensePercent` in `CombatAdjustment` |
| Wire | AshP41 `monster_defense`, AshP46 `will_usually_miss` |
| REVISION | `phase98` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- Beeosity (non-1) + Beecore path
- BIG-core
- Expression/numeric `MLMult:` / Phys/Elem resistance parse + `$monster[physical_resistance|elemental_resistance|…]`
- Manuel; queue-aware `appearance_rates`; full `MonsterStatusTracker`
- Maximizer Evaluator
