# Phase 75: Volcano maze solve/step automation

**Date:** 2026-06-18  
**Track:** Tier 1 CLI long-tail + Tier 3 volcanomaze automation  
**REVISION:** `phase75`

## Goal

Complete volcano nemesis maze automation deferred from Phase 74: BFS pathfinding, JSON/HTML response parsing, HTTP requests, and `volcano solve|step|jump|move|test` CLI wired through the existing `VolcanoMazeManager` foundation.

## Deliverables

| Area | Change |
|------|--------|
| Path | `VolcanoPath` — BFS prefix chain (`size`, `get`, `getLast`, iteration) |
| Request | `VolcanoMazeRequest` — HTTP GET to `volcanomaze.php`, LEW guard, `parseResponse` |
| Manager | `VolcanoMazeManager` — `parseResult`, `parseJsonCoords`/`parseHtmlCoords`, `useCachedVolcanoMaps`, `generateNeighbors`, `solve`, `atGoal`, `discoverMaps`, suspend `step`/`solve`/`jump`/`moveTo`/`visitMaze`, `test` |
| CLI | `GameRuntimeLibrary.Volcano.kt` — `volcano solve|step|jump|move|movep|test` (keeps Phase 74 `visit|clear|map|platforms`) |
| Tests | `VolcanoMazeManagerTest` (6 golden BFS paths + parse/cache tests), CLI `volcano test`/`step` cases, `VolcanoMapTest` RNG reset |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- `volcano slime` (`VolcanoIslandRequest.getSlime()`)
- Relay browser hooks (`decorate`, `replaceVolcanoMazeJavaScript`)
- Ash corpus expansion batch (Tier 1 #1 — Phase 76 candidate)
- Next Tier 3 unwired file (`bastille.txt`, `bookoffacts.txt`, etc.)
