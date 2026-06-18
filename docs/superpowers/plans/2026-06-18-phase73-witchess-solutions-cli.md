# Phase 73: Witchess solutions loader + `witchess` CLI

**Date:** 2026-06-18  
**Track:** Tier 3 data wiring + CLI long-tail  
**REVISION:** `phase73`

## Goal

Wire bundled `witchess_solutions.txt` and port desktop Witchess daily puzzle automation: solution coordinate encoding, HTTP puzzle fetch/solve, and `witchess buff|solve` CLI.

## Deliverables

| Area | Change |
|------|--------|
| Data | `WitchessSolutionDatabase` — load 150 puzzles, `solvePath()` coord encoding |
| Load | `GameDatabase.load()` calls `WitchessSolutionDatabase.load()` |
| Request | `WitchessRequest` — puzzle fetch, buff claim, solution POST |
| Manager | `WitchessManager.solveDailyPuzzles()` — campground check + 5-slot loop |
| CLI | `GameRuntimeLibrary.Witchess.kt` — `witchess`, `witchess buff`, `witchess solve` |
| Tests | `WitchessSolutionDatabaseTest` (desktop golden vectors); CLI witchess cases |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred (Phase 74+)

- `volcanomaze.txt` + full `volcano clear|step|solve`
- Ash corpus expansion batch (Tier 1 #1)
- `StandardRequest.isAllowed` path gating for Witchess Set
- BreakfastManager `checkWitchess` integration
