# Phase 194: AshP183 Pulverize.txt Loader + AshP184 Pulverize ASH v1

## Summary

Wired the bundled `pulverize.txt` static loader deferred from Phases 192–193, then added live ASH `pulverize(item)` v1 for direct item-id mappings plus validate corpus tests.

## Delivered

- **`PulverizeFlags.kt`** — `PULVERIZE_BITS` + `MALUS_UPGRADE` stub constant
- **`EquipmentDatabase.loadPulverizeFromText()`** — parse `name\tresult` rows from bundled `pulverize.txt` on `load()`; item name → result item id; `nosmash` → `-1`; numeric → `PULVERIZE_BITS | int`; skip `upgrade`/`*cluster` rows in v1
- **`GameRuntimeLibrary.AshP183Batch.kt`** — batch marker for loader wiring
- **`GameRuntimeLibrary.AshP184Batch.kt`** — live `pulverize(item)` / `pulverize(id)` returning `{resultItem → 1_000_000}` for positive direct mappings; empty aggregate for `-1`/bitmask
- **Tests** — `EquipmentDatabasePulverizeTest`, `GameRuntimeLibraryAshP183Test`, `GameRuntimeLibraryAshP184Test` (Chester's sunglasses → epic wad, nosmash, moss mace standard-reward overlay)
- **`REVISION`** — `phase194` (3,385 tests)

## Deferred (Phase 195+)

- Modifier-implication `derivePulverization(id)` general path + `initializePulverization()` prefetch
- Full bitmask pulverize ASH decode (elem/yield/powder/nugget/wad split)
- `upgrade` / `*cluster` pulverize.txt row handling
- Armory meat NPC row visit overlay; session-log `toData()` on armory visit learn
- AshP114 live `desc()` prefetch
