# Phase 153: AshP111 Campground Garden Sync

**Date:** 2026-07-27  
**Revision:** `phase153`  
**Ash batch:** AshP111

## Summary

Phase 153 closes the Phase 151/152 deferred `my_garden_type` gap: garden crop type is parsed from campground HTML into `CharacterState`, and the AshP10 getter reads live state with desktop-compatible `"none"` fallback.

## Delivered

### CropType + GardenSync

- **`CropType`** enum — nine desktop crop types with lowercase `toString()`
- **`GardenSync`** — gif-family pattern detection (`pumpkinpatch_`, `mushgarden.gif`, `/rockgarden/`, etc.)
- Updates `CharacterState.gardenType` + mirrors `myGardenType` pref

### CharacterState + KoLCharacter

- Added `gardenType: String` under campground section
- Extended `setCampground(gardenType: String? = null)`

### Visit hooks + ASH

- `processVisitResponseHooks`: sync on `campground.php` visits
- **`my_garden_type`**: state → pref → `"none"` (desktop parity)

### Tests

- `GardenSyncTest`
- `GameRuntimeLibraryAshP111Test`
- Corpus: `corpus_myGardenType_fromState`

## Deferred (unchanged)

- `desc(entity)` desc cache infrastructure
- `has_queued_commands` (no mobile CLI queue)
- Full `parseCampground` (dwelling, workshed, portal)
- Crop counts / rock-plot harvest routing

## Verification

- `.\gradlew.bat :shared:jvmTest` — 2,779 tests pass
- `.\gradlew.bat :androidApp:assembleDebug` — OK
