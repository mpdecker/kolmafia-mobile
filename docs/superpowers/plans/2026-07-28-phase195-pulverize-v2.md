# Phase 195: AshP185 Upgrade/Cluster Loader + AshP186 Pulverize ASH v2

## Summary

Continued the pulverize track deferred from Phase 194: wired `upgrade`/`*cluster` pulverize.txt rows and implemented full desktop bitmask `pulverize(item)` decode.

## Delivered

- **`PulverizeFlags.kt`** — full yield/element bitmask constants (`YIELD_*`, `ELEM_*`, `MALUS_UPGRADE`)
- **`EquipmentDatabase.deriveUpgrade()` / `deriveCluster()`** — desktop parity for Malus upgrade powders/nuggets and cluster equipment rows
- **`EquipmentDatabase.loadPulverizeFromText()`** — no longer skips upgrade/cluster specs (~80 rows)
- **`PulverizeAggregate.kt`** — shared bitmask decode (elem/yield/powder/nugget/wad/gem/cluster splits at ×1,000,000)
- **`GameRuntimeLibrary.AshP185Batch.kt`** — batch marker for loader wiring
- **`GameRuntimeLibrary.AshP186Batch.kt`** — live `pulverize(item)` / `pulverize(id)` v2; AshP184 reduced to marker-only
- **Tests** — `EquipmentDatabasePulverizeTest`, `PulverizeAggregateTest`, `GameRuntimeLibraryAshP185Test`, `GameRuntimeLibraryAshP186Test`
- **`REVISION`** — `phase195` (3,399 tests)

## Deferred (Phase 196+)

- Lazy `derivePulverization(id)` for unlisted equipment (modifier implications + power tiers)
- `initializePulverization()` prefetch on login
- Armory meat NPC row visit overlay; session-log `toData()` on armory visit learn
- AshP114 live `desc()` prefetch
