# Phase 83: AshP41 Monster Combat Stat Library

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase83`

## Goal

Register and implement `monster_attack` / `monster_defense` / `monster_hp` / `monster_initiative` / `monster_phylum` (0-arg + 1-arg) from `MonsterDefinition`, with desktop-lite ML adjustment for Atk/Def/HP.

## Deliverables

| Area | Change |
|------|--------|
| Math | `CombatAdjustment.monsterStatWithMl` / attack / defense / hp / initiative / phylum |
| ASH | AshP41 0+1-arg wrappers; 0-arg via `LAST_MONSTER` |
| Tests | `GameRuntimeLibraryAshP41Test`, formula cases, corpus |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- `monster_element` / `ED:` parse
- Live fight `MonsterStatusTracker`
- `buffed_hit_stat`, `will_usually_miss`/`dodge`, `jump_chance`
- `item_drops` / `meat_drop` aggregates
- Queue-aware appearance rates, bastille/nonfilling, Maximizer Evaluator
