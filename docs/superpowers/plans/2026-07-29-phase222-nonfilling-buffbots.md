# Phase 222: AshP239 nonfilling.txt + AshP240 buffbots.txt

**Delivered:** 2026-07-29  
**Revision:** `phase222`  
**Tests:** 3,646 (+15 from Phase 221)

## Context

Phase 221 deferred bundled-data wiring for `nonfilling.txt` and `buffbots.txt`. Phase 222 closes both Tier 3 gaps.

## AshP239 — nonfilling.txt loader

- Added `ConsumableType.NONFILLING` + `ConsumableQuality.NONE`
- Extended `ConsumableDatabase.load()` to parse `nonfilling.txt` (name, levelReq, optional notes; amount=0)
- Unified lookup API: `getConsumableByName`, `getLevelReqByName`, `getFullnessByName`, `getNonFilling`
- `GameDatabase` wrappers: `nonFilling()`, `levelReq()`, `fullness()`
- Batch marker: `GameRuntimeLibrary.AshP239Batch.kt`

## AshP240 — buffbots.txt registry loader

- Added `BuffBotEntry` (name, playerId, xmlUrl)
- Refactored `BuffBotDatabase` with `loadRegistry()` from bundled `buffbots.txt`
- Registry API: `isKnownBot`, `findBot`, `isOptedOut`, `allBots`
- `BuffBotManager.requestBuff` rejects opted-out bots
- Wired via `GameDatabase.load()` + `SharedModule` DI (`BuffBotDatabase.instance`)
- Batch marker: `GameRuntimeLibrary.AshP240Batch.kt`

## Tests

- `ConsumableDatabaseNonFillingTest` — loader, level req, zero fullness, notes
- `BuffBotDatabaseTest` — active bots, commented rows skipped, opt-out, case-insensitive lookup
- `BuffBotManagerTest` — opt-out failure + known bot PM still sends
- `GameRuntimeLibraryAshP240Test` — revision pin
- Bulk revision pins: `phase221` → `phase222`

## Deferred (Phase 223+)

- Bastille Battalion automation / optimal solver CLI
- Buffbot XML fetch + philanthropic `getOffering` meat substitution
- `ocean.txt`, `faxbots.txt`, `fambattle.txt`, `wereprofessor.txt`, TCRS files
- Item `$item[levelreq]` / `$item[fullness]` bracket fields
