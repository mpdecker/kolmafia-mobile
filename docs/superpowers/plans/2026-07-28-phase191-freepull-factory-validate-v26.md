# Phase 191: AshP177 Freepull Bucket Migration + AshP178 Crimbo23 Factory Validate v26

**Revision:** `phase191`  
**Follows:** Phase 190 (`REVISION = "phase190"`, AshP175/AshP176)

## AshP177 — Freepull bucket migration

| Component | Behavior |
|-----------|----------|
| `StorageBucketMigration` | Move toolbelt (`7566`) between `CACHED_STORAGE` and `CACHED_FREEPULLS` on tower toggle |
| `TimeTowerSync` | Call migration instead of blind cache wipe |

## AshP178 — Coinmaster validate v26

| Component | Behavior |
|-----------|----------|
| Crimbo23 factory validate | Existing `Crimbo23ShopAccessibility` + `CoinmasterSyncedTokenCount` gates |
| Validate probes | trick coin (11480) / prank Crimbo card (11487) |

**Batch files:** `GameRuntimeLibrary.AshP177Batch.kt`, `GameRuntimeLibrary.AshP178Batch.kt`

## Key probe IDs

| Item | ID | Shop | Currency |
|------|-----|------|----------|
| trick coin | 11480 | `crimbo23_elf_factory` | Elf Guard MPC (11408) |
| prank Crimbo card | 11487 | `crimbo23_pirate_factory` | piece of 12 (11409) |

## Deferred (non-goals)

- Armory meat NPC row visit overlay
- `EquipmentDatabase.derivePulverization` / `ResultProcessor.isPulverizedStandardReward`
- Desktop session-log `toData()` output on armory visit learn
