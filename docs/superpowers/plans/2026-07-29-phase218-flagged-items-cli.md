# Phase 218 (delivered): AshP231 profitableList/itemflags + AshP232 automall/quark CLI

**Date:** 2026-07-29  
**Revision:** `phase218`  
**Tests:** 3,603 (`:shared:jvmTest` green; `:androidApp:assembleDebug` OK)

## AshP231 — profitableList + itemflags.txt

- Extended `JunkListManager` with `profitableList` pref (empty default), `profitableIds()` / `isProfitable()`, `importItemFlags()` / `exportItemFlags()`, and `itemFlagsImported` pref for post-import load path
- New `ItemFlagsParser.kt` — desktop `itemflags.txt` section parse/export (`> junk` / `> singleton` / `> mementos` / `> profitable`), unknown names skipped, singleton names merged into junk on import
- Marker batch: `GameRuntimeLibrary.AshP231Batch.kt`

## AshP232 — automall + quark CLI

- New `AutoMallRunner.kt` — iterate `profitableIds()`, skip mementos/meat paste (25)/meat stack (88)/dense stack (258), singleton when `!canInteract`, mall via `ManageStoreRequest.addItem()` at autosell price
- New `QuarkRunner.kt` — unstable quark (3743), meat-paste gluon retrieve for non-Knoll/non-Zombiecore, junk-list or explicit item pool, COMBINE/JEWELRY pasteable filter, highest autosell price selection, `CraftRequest.craft("combine", 1, 3743, itemId)`
- CLI wired in `GameRuntimeLibrary.kt`: `automall`, `quark [items]`
- DI in `SharedModule.kt`; marker batch `GameRuntimeLibrary.AshP232Batch.kt`

## Tests

- Extended `JunkListManagerTest.kt` (profitable empty default, import/export round-trip)
- New `ItemFlagsParserTest.kt`, `AutoMallRunnerTest.kt`, `QuarkRunnerTest.kt`, `GameRuntimeLibraryAshP232Test.kt`
- Revision pins bulk-updated to `phase218`

## Deferred to Phase 219+

- `UntinkerRequest.completeQuest()` full Plains side-trip
- Loathing Legion universal screwdriver untinker path (item 4926)
- `bastille.txt` manager (Tier 3 #7)
- Desktop `managestore.php` vs mobile `backoffice.php` divergence audit if automall hits store API mismatches in live testing
