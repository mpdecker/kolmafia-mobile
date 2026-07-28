# Phase 158: AshP116 Stills + Mushroom Plot ASH

**Date:** 2026-07-27  
**Revision:** `phase158`  
**Ash batch:** AshP116

## Summary

Phase 158 wires live `stills_available()` and `have_mushroom_plot()` ASH with desktop gate logic, visit-hook HTML sync for the still shop and mushroom plot pages, and corpus coverage.

## Delivered

### Stills availability

- **`StillsAvailability.kt`** — desktop gate logic (SUPER_COCKTAIL/MIXOLOGIST skill, moxie class, guild store or Sneaky Pete); unknown stills (`-1`) → 0
- **`StillSync.kt`** — parses `with N bright` from `shop.php?whichshop=still`; updates `CharacterState.stillsAvailable`
- **`KoLCharacter.setStillsAvailable()`** — partial update helper

### Mushroom plot ownership

- **`MushroomPlotSync.kt`** — detects `<b>Your Mushroom Plot:</b>` on `knoll_mushrooms.php`; sets `lastMushroomPlot` pref to current ascension
- **`have_mushroom_plot()`** — `ascensionNumber == lastMushroomPlot`

### AshP116 wiring

- **`GameRuntimeLibrary.AshP116Batch.kt`** — registers `stills_available()` and `have_mushroom_plot()`
- Visit hooks in `GameRuntimeLibrary.processVisitResponseHooks` for still shop and knoll mushroom pages

### Tests

- `StillSyncTest`, `MushroomPlotSyncTest`, `GameRuntimeLibraryAshP116Test`
- Corpus: `corpus_stillsAvailable_fromStillHook`, `corpus_haveMushroomPlot_fromKnollHook`

## Deferred (unchanged)

- Full mushroom plot square parsing / planting automation
- Garden crop yield / `mushroomGardenCropLevel`
- HTTP prefetch when stills unknown (`-1`)
- `craft_type()` (Phase 159 candidate)
- `desc(entity)` on-demand HTTP prefetch

## Verification

- `.\gradlew.bat :shared:jvmTest` — 2,851 tests pass
- `.\gradlew.bat :androidApp:assembleDebug` — OK
