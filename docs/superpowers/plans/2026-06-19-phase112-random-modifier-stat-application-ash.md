# Phase 112: AshP70 random modifier stat application

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase112`

## Goal

Apply desktop `MonsterData.handleRandomModifiers()` stat mutations to last-fight monster instances so `last_monster()["base_*"]` and related brackets reflect OCRS-modified stats. Apply lazily at bracket time with `ExpressionContext` (scale monsters need buffed MUS/MOX).

## Deliverables

| Area | Change |
|------|--------|
| `RandomModifierStats.kt` | Port stat-changing `handleRandomModifiers` cases (HP/Atk/Def/element/resistances/meat/initiative) |
| `GameRuntimeLibrary.kt` | Lazy apply when `MonsterAshRef.useInstance == true` before `MonsterEntityFields.resolve()` |
| Tests | `RandomModifierStatsTest`, `GameRuntimeLibraryAshP70Test`, corpus smoke for modified `base_hp` |
| REVISION | `phase112` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred (Phase 113+)

- Intergnat / mask / dinosaur / hat modifier **pipelines** (populate modifier list pre-fight)
- Beeosity + Beecore stat multiplier
- `translateLeetMonsterName`
- AshP8–P18 remaining stubs / Maximizer Evaluator
