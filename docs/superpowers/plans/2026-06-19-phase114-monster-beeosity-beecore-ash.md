# Phase 114: AshP72 monster beeosity (Beecore)

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase114`

## Goal

Port desktop monster beeosity (Bees Hate You path stat multiplier from name b-count) into `CombatAdjustment` effective HP/Atk/Def/Exp paths, without touching `raw_*` brackets or equipment beeosity limits.

## Deliverables

| Area | Change |
|------|--------|
| `MonsterDefinition` / `MonsterDatabase` | `beeCount` at parse (skip ids 1075–1083) |
| `AscensionPath` / `CharacterState` | `BEES_HATE_YOU` + `inBeecore` gate |
| `ExpressionContext` | `inBeecore` threaded via `buildMonsterExpressionContext()` |
| `CombatAdjustment` | `monsterBeeosity()` on effective HP/Atk/Def/expr paths only |
| REVISION | `phase114` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred (Phase 115+)

- Intergnat demon name/contact sync from fight HTML
- Equipment beeosity for Maximizer / BHY gear limits
- AshP8–P18 remaining stubs / Maximizer Evaluator
