# Phase 125: AshP83 cargo pocket introspection + monster fight registration

**Date:** 2026-06-20  
**Revision:** `phase125`  
**Tests:** 2,488

## Goal

Complete remaining cargo-cult ASH introspection deferred from Phase 124, plus monster-pocket fight registration so choice 1420 combat picks mark pockets correctly.

## Delivered

### GameRuntimeLibrary.CargoPocketAsh.kt

Shared pocket-sort and content builders extracted from AshP82: `sortedMonsterPockets`/`sortedEffectPockets`/`sortedItemPockets`/`sortedStatPockets`, `buildPocketList`/`buildPocketSet`, `buildPocketEffects`/`buildPocketItems`/`buildPocketStats`, `buildIndexedText`, `pocketJokeText`.

### AshP83 ASH

- List helpers: `meat_pockets()`, `poem_pockets()`, `scrap_pockets()`, `joke_pockets()`, `restoration_pockets()`
- Content readers: `pocket_effects`, `pocket_items`, `pocket_stats`, `pocket_scrap`, `pocket_poem`, `pocket_meat`, `pocket_joke`
- `potential_pockets(monster|effect|item|stat)` — full sorted pocket list (all matches, not just first unpicked)

### Monster pocket fight registration

- `CargoPocketSync.registerPocketFight(url)` / `registerPocketFightFromPocket(pocket)` — marks pocket + daily pref
- `AdventureManager.syncCargoPocketFight()` on choice 1420 option 1 → combat
- `GameRuntimeLibrary.processVisitResponseHooks` — combat HTML + choice 1420 pocket param
- `extractMeatNote()` + `checkMeatNotePocket()` stub for meat-note blockquote parity

### Tests

- `GameRuntimeLibraryAshP83Test` — introspection APIs + revision
- Extended `CargoPocketSyncTest` — `registerPocketFight`, `extractMeatNote`
- `SummoningChamberManagerTest` Yeg regression unchanged

## Deferred (Phase 126+)

- DESC_ITEM consequence expressions (32+ rules in `consequences.txt`)
- Maximizer unified Evaluator
- PocketDatabase load completeness (~655/666 rows)
- Between-battle recovery before monster pocket fights

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```
