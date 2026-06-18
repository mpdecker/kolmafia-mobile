# Phase 74: Volcano maze loader + `volcano map|clear|platforms` CLI

**Date:** 2026-06-18  
**Track:** Tier 3 data wiring + CLI long-tail  
**REVISION:** `phase74`

## Goal

Wire bundled `volcanomaze.txt` and port core volcano maze data structures plus read-only/diagnostic CLI (`clear`, `map`, `platforms`). Full solve/step/jump automation defers to Phase 75.

## Deliverables

| Area | Change |
|------|--------|
| Data | `VolcanoMazeDatabase` — 6 map sequences × 5 levels |
| Models | `VolcanoMap`, `Neighbors`, `VolcanoMazeConstants`, `VolcanoMapRng` |
| Manager | `VolcanoMazeManager` — reset, clear, loadCurrentMaps, displayMap, platforms |
| Load | `GameDatabase.load()` calls `VolcanoMazeDatabase.load()` |
| CLI | `GameRuntimeLibrary.Volcano.kt` — `volcano visit|clear|map|platforms` |
| Tests | `VolcanoMazeDatabaseTest`, `VolcanoMapTest`, CLI volcano cases |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred (Phase 75)

- `volcano solve|step|jump|move|movep|test|slime` CLI
- `VolcanoMazeRequest` HTTP + JSON/HTML response parsing
- BFS pathfinding (`Path`, `generateNeighbors`, `solve()`)
- Ash corpus expansion batch (Tier 1 #1)
