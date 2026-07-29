# Phase 220: AshP235 BastilleDatabase + AshP236 choice pref sync

**Delivered:** 2026-07-29  
**Revision:** `phase220`  
**Tests:** 3,622

## AshP235 — BastilleDatabase loader

- Added `BastilleDatabase.kt` — loads bundled `bastille.txt` (81 style-set rows), base-3 style key math (`Upgrade`/`Style` enums), `statsForKey`/`predictedStats` API
- Wired `BastilleDatabase.load()` into `GameDatabase.kt`
- Batch marker: `GameRuntimeLibrary.AshP235Batch.kt`

## AshP236 — BastilleBattalionSync choice pref sync

- Added `BastilleBattalionSync.kt` — visit/pre/post hooks for choices 1313–1319
- Parsers: styles/needles, castle, turn, choices, cheese gain, battle results, game start/end
- Writes desktop `_bastille*` prefs (`_bastilleStats`, `_bastilleGameTurn`, `_bastilleCheese`, `_bastilleChoice1–3`, `_bastilleEnemyCastle`, etc.)
- Integrated into `AdventureManager.resolveChoice()`, `GameRuntimeLibrary.processVisitQuestHooks()`, `GameRuntimeLibrary.cliChoice()`
- Batch marker: `GameRuntimeLibrary.AshP236Batch.kt`

## Tests

- `BastilleDatabaseLoaderTest` — 81 rows, style key round-trip, predicted stats
- `BastilleBattalionSyncTest` — castle/turn/cheese/choices/battle/boost fixture HTML
- `GameRuntimeLibraryAshP236Test` — revision pin + visit hook smoke

## Deferred (Phase 221+)

- Cheese/battle CSV file logging (`logBastilleBattalionBattles`)
- Full battle automation / optimal solver
- `checkPredictions` session-log diagnostics
- `nonfilling.txt` loader
- `buffbots.txt` / managestore audit
