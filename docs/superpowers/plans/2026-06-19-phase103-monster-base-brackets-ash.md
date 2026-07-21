# Phase 103: AshP61 `$monster[base_*]` Effective Brackets

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase103`

## Goal

Wire `$monster[base_hp|base_attack|base_defense|base_initiative]` to desktop-effective CombatAdjustment helpers (ML, REDUCE_ENEMY_DEFENSE, initPenalty), completing the raw/base pair from AshP60.

## Deliverables

| Area | Change |
|------|--------|
| Bracket | `base_*` → `monsterHp`/`Attack`/`Defense`/`InitiativeWithMl` |
| Call site | Pass `reduceEnemyDefensePercent` into `MonsterEntityFields.resolve` |
| REVISION | `phase103` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- Beeosity + Beecore / BIG-core
- AshP41 `monster_initiative()` initPenalty alignment
- Manuel; queue `appearance_rates`; MonsterStatusTracker
- Maximizer Evaluator (Tier 2)
