# Phase 102: AshP60 `$monster[raw_*]` Brackets

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase102`

## Goal

Wire `$monster[raw_hp|raw_attack|raw_defense|raw_initiative]` matching desktop `MonsterData.getRaw*` (pre-ML, scale-without-ML, missing Atk/Def/HP → −1).

## Deliverables

| Area | Change |
|------|--------|
| Eval | `monsterRawHp/Attack/Defense/Initiative` + `rawScaled*` (floor before ×0.75 for HP) |
| Bracket | Four `raw_*` fields on `MonsterEntityFields` |
| REVISION | `phase102` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- Align `$monster[base_*]` with desktop effective-with-ML
- Beeosity + Beecore / BIG-core
- Manuel; queue `appearance_rates`; MonsterStatusTracker
- Maximizer Evaluator (Tier 2)
