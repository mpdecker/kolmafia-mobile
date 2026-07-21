# Phase 109: AshP67 `$monster[fact]` + `$monster[fact_type]`

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase109`

## Goal

Port desktop FactDatabase + PHPMTRandom; wire MonsterProxy `fact` and `fact_type` brackets with class/path-seeded selection.

## Deliverables

| Area | Change |
|------|--------|
| Utility | `PHPMTRandom.kt` — PHP-compatible Mersenne Twister |
| Data | `FactDatabase.kt` — load `bookoffacts.txt`, `getFact()` resolution |
| Expression | `ModifierExpression` — `lt/gte/gt/lte/eq` for conditional facts |
| Bracket | `$monster[fact]` / `$monster[fact_type]` via class + path from `CharacterState` |
| REVISION | `phase109` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- Runtime `random_modifiers` (MonsterStatusTracker)
- `$monster[min_sprinkles|max_sprinkles]`
- Beeosity + Beecore; Manuel manager; queue `appearance_rates` depth
