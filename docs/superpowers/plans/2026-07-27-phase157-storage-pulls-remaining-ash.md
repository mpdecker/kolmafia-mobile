# Phase 157: AshP115 Storage Pulls Remaining Sync

**Date:** 2026-07-27  
**Revision:** `phase157`  
**Ash batch:** AshP115

## Summary

Phase 157 closes the deferred storage `pullsleft` gap: parse pulls remaining from `storage.php?which=5` HTML into `ConcoctionDatabase` and wire live `pulls_remaining()` ASH.

## Delivered

### ConcoctionDatabase pulls state

- **`getPullsRemaining()` / `setPullsRemaining()`** — runtime singleton (default `-1` = unknown)
- **`resetForTest()`** clears pulls alongside recipe data

### StorageMeatSync extension

- Parses `<span class="pullsleft">N</span>` on the same `storage.php?which=5` hook as storage meat
- Fallback when span absent: `0` if hardcore/ronin (`isHardcore || isRestricted`); else `-1`

### AshP115 wiring

- **`GameRuntimeLibrary.AshP115Batch.kt`** — registers `pulls_remaining()` → `ConcoctionDatabase.getPullsRemaining()`
- Wired in `GameRuntimeLibrary.registerAll`

### Tests

- Extended `StorageMeatSyncTest` — pulls parse + apply
- `ConcoctionDatabasePullsTest` — get/set/reset
- `GameRuntimeLibraryAshP115Test` — ASH getter + visit hook
- Corpus: `corpus_pullsRemaining_fromStorageHook`

## Deferred (unchanged)

- Garden crop yield / mushroom plot parse
- `desc(entity)` on-demand HTTP prefetch
- Storage/closet item counts
- Full campground dwelling/workshed parse

## Verification

- `.\gradlew.bat :shared:jvmTest` — 2,866 tests pass
- `.\gradlew.bat :androidApp:assembleDebug` — OK
