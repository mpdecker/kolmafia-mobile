# Phase 108: AshP66 `$monster[random_modifiers]` + `$monster[attributes]`

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase108`

## Goal

Wire MonsterProxy `attributes` (raw param string) and `random_modifiers` (empty from static template data) brackets.

## Deliverables

| Area | Change |
|------|--------|
| Model | `attributes: String`, `randomModifiers: List<String>` on `MonsterDefinition` |
| Parse | Store raw `paramStr`; `randomModifiers = emptyList()` |
| Bracket | `$monster[attributes]` → STRING; `$monster[random_modifiers]` → `string[int]` |
| REVISION | `phase108` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- `$monster[fact]` / `$monster[fact_type]` (FactDatabase)
- Runtime random-modifier population (MonsterStatusTracker)
- Beeosity + Beecore; Manuel manager; queue `appearance_rates`
- Maximizer Evaluator (Tier 2)
