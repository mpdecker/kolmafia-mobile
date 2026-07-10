# Phase 78: AshP36 Location Session Bracket Fields

**Date:** 2026-06-18  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase78`

## Goal

Wire live location session/runtime bracket fields deferred from Phase 77: per-zone turn tracking, last-noncombat prefs, `my_total_turns_spent()`, stretch `poison` and `turns_until_forced_noncombat()`.

## Deliverables

| Area | Change |
|------|--------|
| Tracker | `AdventureSpentTracker` — prefs-backed per-zone turn map + `totalTrackedTurns` |
| Adventure hook | `AdventureManager.emitTurnConsumed` increments tracker + records `lastNoncombat{id}` on NC |
| Brackets | `LocationEntityFields` live `turns_spent`, `last_noncombat_turns_spent`, `poison` |
| ASH | `my_total_turns_spent()`, `turns_until_forced_noncombat(loc)` |
| Data | `MonsterDatabase` `Poison:` parser + `PoisonLevels` + `CombatDatabase.poisonForLocation()` |
| Tests | `GameRuntimeLibraryAshP36Test`, `corpus_locationSessionFields_live`, `AdventureManagerTest` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- `kisses`, `water_level`, `fire_level` — Dreadsylvania/Wildfire/Raincore subsystems
- `bastille.txt` loader/manager (Tier 3)
- `numeric_modifier(monster, …)` — no bundled monster modifier data
