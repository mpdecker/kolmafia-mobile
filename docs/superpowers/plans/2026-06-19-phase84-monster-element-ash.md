# Phase 84: AshP42 Monster Element Library

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase84`

## Goal

Parse `ED:` into `MonsterDefinition.defenseElement` and register `monster_element` (0+1-arg) plus `$monster[attack_element]` / `[defense_element]` brackets.

## Deliverables

| Area | Change |
|------|--------|
| Data | `defenseElement` + `ED:` parse (shared `ELEMENT_VALUES`) |
| ASH | AshP42 `monster_element` wrappers |
| Bracket | `MonsterEntityFields` attack/defense_element |
| Tests | Defense-element parse, AshP42, corpus |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- Live fight `MonsterStatusTracker`
- `jump_chance`, `buffed_hit_stat`, `will_usually_miss`/`dodge`
- `meat_drop` / `item_drops` / `item_drops_array`
- Queue-aware appearance rates, bastille/nonfilling, Maximizer Evaluator
