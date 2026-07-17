# Phase 80: AshP38 Location Monster Queries + Monster Parts

**Date:** 2026-06-18  
**Track:** Tier 1 ASH behavioral parity (+ Tier 3 data wiring)  
**REVISION:** `phase80`

## Goal

Register and implement live `get_monsters` / `appearance_rates` from `CombatDatabase`, and wire bundled `monsterparts.txt` into `$monster["parts"]`.

## Deliverables

| Area | Change |
|------|--------|
| Data | `MonsterPartsDatabase` loader from `monsterparts.txt` via `GameDatabase.load()` |
| Bracket | `MonsterEntityFields` live `parts` → `string[int]` aggregate |
| ASH | `get_monsters(loc)`, `appearance_rates(loc[, includeQueue])`, stretch `get_location_monsters(loc)` |
| Queue | `includeQueue=true` currently matches false (no adventure-queue tracker) |
| Tests | `GameRuntimeLibraryAshP38Test`, `MonsterPartsDatabaseTest`, corpus |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- Combat-queue-aware `appearance_rates(..., true)`
- Class/path monster remaps
- `bastille.txt` / BastilleBattalionManager
- `numeric_modifier(monster, …)`
- `expected_damage` / full combat prediction
