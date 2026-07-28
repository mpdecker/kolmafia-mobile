# Phase 154: AshP112 Closet + Storage Meat Sync

**Date:** 2026-07-27  
**Revision:** `phase154`  
**Ash batch:** AshP112

## Summary

Phase 154 closes the AshP12 meat-getter gap: closet and storage meat totals are parsed from visit HTML into `CharacterState`, making `my_closet_meat()` and `my_storage_meat()` live after the relevant page visits.

## Delivered

### ClosetMeatSync + StorageMeatSync

- **`ClosetMeatSync`** — parses `Your closet contains <b>X</b> meat.` from `closet.php` HTML
- **`StorageMeatSync`** — parses storage meat from `storage.php?which=5` (normal + fistcore patterns, zero-meat fallback)

### KoLCharacter

- Added `setClosetMeat(Long)` and `setStorageMeat(Long)`

### Visit hooks

- `processVisitResponseHooks`: sync on `closet.php` and `storage.php?which=5` visits
- AshP12 getters unchanged — already read from `CharacterState`

### Tests

- `ClosetMeatSyncTest`
- `StorageMeatSyncTest`
- `GameRuntimeLibraryAshP112Test`
- Corpus: `corpus_myClosetMeat_fromState`

## Deferred (unchanged)

- `desc(entity)` desc cache infrastructure
- `has_queued_commands` (no mobile CLI queue)
- `my_session_meat` increment wiring
- Storage pulls remaining (`pullsleft` span)
- Full campground dwelling/workshed/portal parse
- Crop counts / rock-plot harvest routing

## Verification

- `.\gradlew.bat :shared:jvmTest` — 2,796 tests pass
- `.\gradlew.bat :androidApp:assembleDebug` — OK
