# Phase 185: AshP165 Free-Pull Collections + AshP166 Validate v20

**Revision:** `phase185`  
**Follows:** Phase 184 (`REVISION = "phase184"`, AshP163/AshP164)

## AshP165 — Collection ASH v1 (free pulls)

| Component | Behavior |
|-----------|----------|
| `get_free_pulls()` | Live fetch via `StorageRequest.fetchClassifiedContents().freepulls` |
| `get_cached_free_pulls()` | Read `Preferences.CACHED_FREEPULLS` snapshot |
| `TimeTowerSync` | Clear `_cachedFreepulls` when `timeTowerAvailable` toggles |

## AshP166 — Coinmaster validate v20

| Component | Validate behavior |
|-----------|-------------------|
| `storage_amount(item)` | Sum cached storage + cached freepull counts (desktop parity) |
| `TinkeringBenchPurchasedItem` | On tinker buy, `OutfitCheckpoint.forgetEquipment` for ingredient items |
| `flowertradein` gate | Requires rose/tulip accessible count ≥ 1 |

**Batch files:** `GameRuntimeLibrary.AshP165Batch.kt`, `GameRuntimeLibrary.AshP166Batch.kt`

## Key probe IDs

| Item | ID |
|------|-----|
| rose | 8668 |
| time-twitching toolbelt | 7566 |
| biphasic molecular oculus | 11550 |
| smashed scientific equipment | 11549 |

## Deferred (non-goals)

- Flower tradein dynamic row refresh (`visitShopRows`)
- MerchTable HTML token balance prefs (inventory counts suffice)
- Full storage/freepull bucket migration on tower toggle
