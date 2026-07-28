# Phase 151: AshP109 Character Resources + Entity Introspection

## Summary

Replaced dead-pref AshP10/AshP11 character-resource getters with live `CharacterState` reads, fixed `my_path_id()` to return `AscensionPath.pathId`, and wired AshP12 `name(entity)` plus AshP8 extended `to_int(entity)` through a shared `EntityIntrospection.kt` helper.

## Delivered

- **`GameRuntimeLibrary.AshP10Batch.kt`** — `my_fury`/`my_soulsauce`/`my_pp`/`my_maxpp`/`my_thunder`/`my_rain`/`my_lightning` from `CharacterState`; `my_discomomentum`/`my_audience` prefer state with pref fallback; `my_path_id` uses `ascensionPath.pathId`
- **`GameRuntimeLibrary.AshP11Batch.kt`** — `my_robot_energy`/`my_robot_scraps` from `CharacterState`
- **`EntityIntrospection.kt`** — `entityName()` database/resolver lookups; `entityToInt()` for CLASS/STAT/SLOT/ELEMENT/PHYLUM/PATH/THRALL/SERVANT/VYKEA/BOUNTY/MODIFIER/COINMASTER/LOCATION
- **`GameRuntimeLibrary.AshP12Batch.kt`** — live `name()` overloads (desc remains `""`)
- **`GameRuntimeLibrary.AshP8Batch.kt`** — live extended-type `to_int()` (replaces hashCode stubs)
- **Tests** — `GameRuntimeLibraryAshP109Test`, corpus `corpus_characterResources_live` + `corpus_entityNameAndToInt_live`, updated `my_path_id` corpus assertion
- **`REVISION`** — `phase151`

## Deferred (Phase 152+)

- `has_queued_commands` (no mobile CLI queue subsystem)
- `desc(entity)` (requires desc.php cache)
- `my_garden_type` / `my_mask` / `my_paradoxicity` (charpane/campground sync)
- Charpane sync for disco/audience
- Bulk corpus expansion
- PvP / interactive ASH stubs
