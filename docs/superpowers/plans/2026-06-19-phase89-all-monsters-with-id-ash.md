# Phase 89: AshP47 All Monsters With ID

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase89`

## Goal

Live `all_monsters_with_id()` → `boolean[monster]` for every `MonsterDatabase` entry with non-zero id.

## Deliverables

| Area | Change |
|------|--------|
| ASH | AshP47 `all_monsters_with_id()` |
| Tests | Count (unique names), mosquito present, none absent |
| Note | Map keyed by name; duplicate names collapse (desktop keys by id Value) |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- `dad_sea_monkee_weakness` / `unusual_construct_disc`
- Manuel APIs
- Expression `Init:` / Overclocked; missing-Init → −1
- Queue-aware `appearance_rates`; full `MonsterStatusTracker`
- Maximizer Evaluator; bastille/nonfilling
