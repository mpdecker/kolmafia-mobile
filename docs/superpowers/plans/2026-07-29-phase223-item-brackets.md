# Phase 223: AshP241 Item Entity Bracket Fields (Consumable v1)

**Delivered:** 2026-07-29  
**Revision:** `phase223`  
**Tests:** 3,666 (+20 from Phase 222)

## Context

Phase 222 wired `nonfilling.txt` into `ConsumableDatabase` but deferred item `$item[field]` bracket reads. Phase 223 closes that gap with consumable v1 fields via `ItemEntityFields`.

## AshP241 — item entity bracket fields

- Extended `ConsumableDatabase` with bracket lookup helpers: `getInebrietyByName`, `getSpleenByName`, `getQualityName`, `getAdventureRange`, `getMuscleRange`, `getMysticalityRange`, `getMoxieRange`, internal `formatRange`
- Added `ConsumableQuality.displayName()` for desktop-parity quality strings
- Created `ItemEntityFields.kt` resolving: `id`, `name`, `plural`, `descid`, `image`, `levelreq`, `quality`, `adventures`, `muscle`, `mysticality`, `moxie`, `fullness`, `inebriety`, `spleen`
- Wired `AshType.ITEM` in `GameRuntimeLibrary.resolveEntityIndex`
- Batch marker: `GameRuntimeLibrary.AshP241Batch.kt`

## Tests

- `ConsumableDatabaseBracketLookupTest` — range/quality/inebriety/spleen helpers
- `ItemEntityFieldsTest` — direct resolver coverage for food/drink/spleen/nonfilling
- `GameRuntimeLibraryAshP241Test` — revision pin + Ash interpreter bracket smoke
- `AshCompatibilityCorpusTest.corpus_itemEntityFields_live` — corpus bracket reads
- Bulk revision pins: `phase222` → `phase223`

## Deferred (Phase 224+)

- Item restore brackets (`minhp`/`maxhp`/`minmp`/`maxmp` via `RestoreDatabase`)
- Item flag brackets (`tradeable`, `giftable`, `discardable`, `usable`, equipment `power`/`hands`)
- Buffbot XML fetch + philanthropic `getOffering` meat substitution
- Bastille Battalion automation / optimal solver CLI
- Remaining unwired data files: `faxbots.txt`, `ocean.txt`, `fambattle.txt`, `wereprofessor.txt`
