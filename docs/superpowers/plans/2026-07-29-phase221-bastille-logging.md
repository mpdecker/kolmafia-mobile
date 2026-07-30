# Phase 221: AshP237 Bastille Session Log + AshP238 Battle/Cheese File Logging

**Delivered:** 2026-07-29  
**Revision:** `phase221`  
**Tests:** 3,631 (+9 from Phase 220)

## Context

Phase 220 delivered `BastilleDatabase` + `BastilleBattalionSync` pref sync for choices 1313–1319. Phase 221 closes the deferred logging gaps from that phase.

## AshP237 — Session-log parity + battle/cheese record tracking

- Added `BastilleBattalionModels.kt` — `BastilleCheeseEncounter`, `BastilleStance`, `BastilleBoosts`, `BastilleBattle`, `BastilleBattleResults`, `BastilleCheeseRecord`
- Extended `BastilleBattalionSync.kt`:
  - `BastilleSyncContext` (optional `SessionLogger`, `playerId`)
  - `registerRequest()` — choice-action logging for 1313–1319
  - `logLine()` / `logStrength()` / `checkPredictions()` — session-log diagnostics via `SessionLogger.appendRawLine`
  - Completed `startBattle()` (stance from option 1–3), `endBattle()` (win cheese attach), `collectCheese()` (cheese record build; wishing-well skip)
  - Post-rig sync: style name log + `checkPredictions()` + `logStrength()`
- Added `BastilleDatabase.Stats.toStrengthString()` + `Stat.NONE`
- Wired `registerRequest` before `syncPreChoice` in `GameRuntimeLibrary.cliChoice` and `AdventureManager.resolveChoice`
- Batch marker: `GameRuntimeLibrary.AshP237Batch.kt`

## AshP238 — Tab-delimited spading file append

- Added `UserDataFileAppender` expect/actual (JVM temp dir for tests, Android `filesDir/data/`, iOS documents)
- Added `UserDataFilePaths` for JVM test base path override
- Added `BastilleBattalionFileLog.kt`:
  - `saveBattle()` → `data/Bastille.battles.txt` (key = `YYYYMMDD.playerId.game.round`)
  - `saveCheese()` → `data/Bastille.cheese.txt`
  - Gated by `logBastilleBattalionBattles` pref (default in `defaults.txt`)
- `AndroidUserDataContext.init()` in `KoLMafiaApp.kt`
- Batch marker: `GameRuntimeLibrary.AshP238Batch.kt`

## Wiring

- `GameRuntimeLibrary.REVISION = "phase221"`
- `bastilleSyncContext()` helper; session logger + player id passed into Bastille sync hooks
- `SharedModule.kt` injects `sessionLogger` into `AdventureManager`

## Tests

- `BastilleBattalionSyncTest` — `registerRequest` action strings, `checkPredictions` mismatch logs, cheese turn strength log, wishing-well skip, battle win cheese record
- `BastilleBattalionFileLogTest` — append gated/off, tab line format, battle + cheese rows
- `UserDataFileAppenderTest` — append creates file, second append adds line
- `GameRuntimeLibraryAshP238Test` — revision pin + registerRequest smoke via cliChoice hook
- Bulk revision pins: `phase220` → `phase221` across Ash test files

## Deferred (Phase 222+)

- Full Bastille automation / optimal solver CLI
- `nonfilling.txt` loader → `ConsumableDatabase`
- `buffbots.txt` / managestore audit
- AshP8–P18 interactive/PvP stubs
