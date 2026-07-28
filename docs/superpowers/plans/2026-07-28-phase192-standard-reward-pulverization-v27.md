# Phase 192: AshP179 Standard Reward Pulverization Derive + AshP180 Pulverized Currency Hooks v27

**Revision:** `phase192`  
**Follows:** Phase 191 (`REVISION = "phase191"`, AshP177/AshP178)

## Summary

Closed the standard-reward pulverization gap deferred since Phase 190:

- **AshP179** — `StandardRewardDatabase.derivePulverization()` maps each standard-reward equipment item to next-year pulverized currency via `EquipmentDatabase.addPulverization()`; wired on `CoinmasterDatabase.load()`.
- **AshP180** — `StandardRewardCurrencySync.onInventoryDelta()` bumps `_armoryAndLeggeryCurrencyRefresh` when pulverized standard-reward currency quantity increases; hooked from `InventoryManager.fetchInventory()`.

## Key files

| File | Change |
|------|--------|
| `StandardRewardDatabase.kt` | `derivePulverization()` |
| `EquipmentDatabase.kt` | `pulverizeByItemId`, `addPulverization`, `getPulverization` |
| `CoinmasterDatabase.kt` | call derive after load; reset EquipmentDatabase in tests |
| `StandardRewardCurrencySync.kt` | pulverized currency acquire pref bump |
| `InventoryManager.kt` | inventory diff → currency sync |
| `GameRuntimeLibrary.AshP179Batch.kt` / `AshP180Batch.kt` | batch markers |
| `GameRuntimeLibrary.kt` | `REVISION = "phase192"` |

## Tests

- `StandardRewardDatabaseTest.derivePulverization_populatesEquipmentDatabase` (11504 → 11526)
- `GameRuntimeLibraryAshP179Test` — revision pin + derive wiring
- `GameRuntimeLibraryAshP180Test` — currency sync pref + validate v27 matrix
- AshP revision pins bulk-updated to `phase192`

**Verification:** `.\gradlew.bat :shared:jvmTest` — 3,368 tests passing.

## Non-goals (carried forward)

- Full `pulverize.txt` bitmask loader
- Armory meat NPC row visit overlay
- Desktop session-log `StandardRewardDatabase.toData()` on armory visit learn
