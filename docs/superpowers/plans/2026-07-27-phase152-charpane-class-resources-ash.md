# Phase 152: AshP110 Charpane + Combat Class-Resource Sync

**Date:** 2026-07-27  
**Revision:** `phase152`  
**Ash batch:** AshP110

## Summary

Phase 152 closes the Phase 151 deferred gap: class/path resources not present in the status API are now parsed from charpane and fight HTML into `CharacterState`, and the remaining pref-only AshP10/AshP18 getters read live state with pref fallback.

## Delivered

### CharacterState + KoLCharacter

- Added `currentMask`, `paradoxicity`, `telescopeLookedHigh` to `CharacterState`
- Added `isSneakyPete` / `inNoobcore` path helpers
- Extended `updateClassResource()` for mask/paradoxicity
- Extended `setCampground()` for `telescopeLookedHigh` (nullable partial updates)

### Sync parsers

- **`ClassResourceCharpaneSync`** — fury, soulsauce, audience (Love/Hate/Bored), paradoxicity, mask id→name map, absorbs (Gelatinous Noob)
- **`ClassResourceCombatSync`** — `discomo(\d).gif` → `discoMomentum`

### Visit hooks

- `processVisitResponseHooks`: charpane.php + fight.php sync
- `visitKolPage("charpane.php")`: charpane sync alongside Ed/VYKEA/pasta thrall
- **`TelescopeSync`**: optional `KoLCharacter?` mirrors `telescopeUpgrades` / `telescopeLookedHigh` into state

### ASH getters (AshP10 / AshP18)

| ASH | Source |
|---|---|
| `my_mask` | `CharacterState.currentMask`, pref fallback |
| `my_paradoxicity` | `CharacterState.paradoxicity`, pref fallback |
| `my_audience` / `my_discomomentum` | already state-first; charpane/fight sync makes them live |
| `telescope_upgrades` | `CharacterState.telescopeUpgrades`, pref fallback |
| `telescope_looked_high` | `CharacterState.telescopeLookedHigh` OR pref |

### Tests

- `ClassResourceCharpaneSyncTest`
- `ClassResourceCombatSyncTest`
- `GameRuntimeLibraryAshP110Test`
- Corpus: `corpus_charpaneClassResources_live`, `corpus_telescopeUpgrades_fromState`

## Deferred (unchanged)

- `my_garden_type` / campground crop parser
- `desc(entity)` desc cache infrastructure
- `has_queued_commands` (no mobile CLI queue)
- Bulk corpus expansion beyond 2 targeted snippets

## Verification

- `.\gradlew.bat :shared:jvmTest` — 2,767 tests pass
- `.\gradlew.bat :androidApp:assembleDebug` — OK
