# Phase 196: AshP187 Lazy Pulverize Derive + AshP188 Login Prefetch

## Summary

Closed the pulverize track gap from Phase 195: unlisted equipment now lazy-derives pulverization bitmasks (modifier implications + power tiers + gift/NPC gates), and login prefetches the cache for all pulverizable items.

## Delivered

- **`PulverizeImplications.kt`** — desktop `IMPLICATIONS` map (resistance/damage/spell-damage → element flags)
- **`EquipmentDatabase`** — `isPulverizable`, `derivePulverization`, lazy-cache `getPulverization`, `initializePulverization`, gift-only (`'g'` without `'t'`) + NPC-store useless powder gates
- **`GameDatabase.load()`** — calls `EquipmentDatabase.initializePulverization()` after modifiers/coinmasters
- **`GameRuntimeLibrary.AshP187Batch.kt`** / **`AshP188Batch.kt`** — batch markers
- **`StandardRewardDatabase.findPulverization`** — desktop-aligned year lookup (removed erroneous `year + 1`)
- **Tests** — `PulverizeImplicationsTest`, `EquipmentDatabaseDerivePulverizationTest`, `GameDatabaseLoadTest`, `GameRuntimeLibraryAshP187Test`, `GameRuntimeLibraryAshP188Test`, corpus derived `pulverize()` snippet; standard-reward fixture alignment (moss mace → moss mulch)
- **`REVISION`** — `phase196` (3,415 tests)

## Validate corpus IDs

- `11911` bishop's mitre — derived multi-element `YIELD_1P` bitmask
- `4682` pottery hat — Cold Resistance + Hot Damage → `YIELD_2P` + hot powder aggregate
- `11504` moss mace — explicit StandardReward map → `11510` moss mulch (not lazy derive)

## Deferred (Phase 197+)

- Armory meat NPC row visit overlay + session-log `toData()` on armory visit learn
- AshP114 live `desc()` HTTP prefetch
