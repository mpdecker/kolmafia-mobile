# Phase 149: AshP107 in_terrarium + Maximizer FamiliarUsability + Retrieve List Refresh

## Summary

Closed Phase 148 familiar-track deferrals: ownership-only `in_terrarium` ASH, Maximizer `switch` familiar filtering via `FamiliarUsability`, and lazy HC/thrifty/trendy list refresh before retrieve restriction checks.

## Delivered

- **`in_terrarium(fam)`** — registered in `GameRuntimeLibrary.Familiar.kt`; checks terrarium ownership only (no path-usability gate)
- **`RestrictionListRefresh.kt`** — shared `ensureInitialized` helper for Standard/Thrifty/Trendy lists
- **`MaximizerManager.resolveFamiliarSwitch`** — uses `FamiliarUsability.usableByRace` + list refresh before switch selection
- **`RetrieveItemService`** — calls `RestrictionListRefresh` before `ItemRestriction.isAllowed`; wired in `SharedModule`
- **Tests** — `GameRuntimeLibraryFamiliarTest`, `GameRuntimeLibraryAshP107Test`, `MaximizerManagerTest`, `RetrieveItemServiceTest`, corpus snippets (`corpus_inTerrarium_ownedVsUsable`, `corpus_retrieveItem_restrictedEarlyExit`)
- **`REVISION`** — `phase149` (2,737 tests)

## Deferred (Phase 150+)

- Bulk `AshCompatibilityCorpusTest` expansion
- AshP8–P18 interactive/PvP stubs
- Maximizer enthroned/bjorn familiar usability filtering
- `use_familiar` path-usability gate
