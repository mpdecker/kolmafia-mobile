# Phase 148: AshP106 Familiar Usability + Retrieve Restriction Parity

## Summary

Closed Phase 147 deferrals: desktop `KoLCharacter.isUsable`/`usableFamiliar` path gates for `have_familiar`, and desktop `InventoryManager.retrieveItem` early-exit when an item is path-restricted and not craftable.

## Delivered

- **`FamiliarUsability.kt`** — Zombie/Beecore/G-Lover/Zootomist/Pokefam gates + `StandardRequest.isAllowed(FAMILIARS)`
- **`FamiliarDefinition`** — `isUndead`, `isPokefamType`
- **`AscensionPath`** — `POKEFAM`, `GLOVER`
- **`CharacterState`** — `inZombiecore`, `inZootomist`, `inPokefam`, `inGLover`
- **`Beeosity.hasGs()`** — G-Lover race-name gate
- **`GameRuntimeLibrary.Familiar.kt`** — `have_familiar` uses `FamiliarUsability` + restriction list refresh
- **`RetrieveItemService`** — `ItemRestriction` early exit with craftable exception; DI `character`
- **Tests** — `FamiliarUsabilityTest`, `GameRuntimeLibraryAshP106Test`, `RetrieveItemServiceTest` extensions, corpus Beecore snippet
- **`REVISION`** — `phase148` (2,727 tests)

## Deferred (Phase 149+)

- Bulk `AshCompatibilityCorpusTest` expansion
- AshP8–P18 interactive/PvP stubs
- `in_terrarium` ASH
- Maximizer familiar filtering via `FamiliarUsability`
