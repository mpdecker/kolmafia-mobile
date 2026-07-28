# Phase 155: AshP112 Repair + AshP113 Session Meat Tracking

**Date:** 2026-07-27  
**Revision:** `phase155`  
**Ash batch:** AshP112 (repair) + AshP113

## Summary

Phase 155 completes the missing Phase 154 AshP112 deliverables and wires live `my_session_meat()` tracking from adventure and visit HTML.

## Delivered

### Part A — AshP112 repair

- **`ClosetMeatSync`** — parses closet meat from `closet.php` HTML
- **`StorageMeatSync`** — parses storage meat from `storage.php?which=5` (normal + fistcore)
- Tests: `ClosetMeatSyncTest`, `StorageMeatSyncTest`, `GameRuntimeLibraryAshP112Test`, corpus `corpus_myClosetMeat_fromState`

### Part B — AshP113 session meat

- **`SessionMeatSync`** — parses `You gain X Meat` from HTML; calls `addSessionMeat`
- **Adventure loop hooks** — combat fight HTML, non-combat text, choice response HTML in `AdventureManager`
- **Visit hook** — `processVisitResponseHooks` for CLI/visit parity
- **`updateFromApiResponse`** preserves `sessionMeat`, `closetMeat`, `gardenType`, `telescopeLookedHigh`, `currentMask`, `paradoxicity` across status refreshes

### Tests

- `SessionMeatSyncTest`
- `GameRuntimeLibraryAshP113Test`
- `KoLCharacterTest.updateFromApiResponse_preservesRuntimeMeatCounters`
- Corpus: `corpus_mySessionMeat_fromState`

## Deferred (unchanged)

- `desc(entity)` description cache
- `has_queued_commands` (non-goal)
- Storage `pullsleft` / crop yield counts
- Full campground dwelling/workshed parse

## Verification

- `.\gradlew.bat :shared:jvmTest` — 2,821 tests pass
- `.\gradlew.bat :androidApp:assembleDebug` — OK
