# Phase 193: AshP181 Armory Refresh Wiring + AshP182 Validate v28

**Revision:** `phase193`  
**Follows:** Phase 192 (`REVISION = "phase192"`, AshP179/AshP180)

## Summary

Completed the standard-reward armory refresh pipeline deferred from Phase 192:

- **AshP181** — `StandardRewardRefresh.refreshArmoryRows()` calls `derivePulverization()` + `ArmoryAndLeggeryShopRows.rebuild()`; wired from `StandardRewardCurrencySync` (currency gain) and `ArmoryAndLeggerySync` (visit learn).
- **AshP182** — validate v28 corpus: visit-learn UNKNOWN row (11528), hard token gate (11512/11534), currency-acquire refresh derive (11504→11526).

## Key files

| File | Change |
|------|--------|
| `StandardRewardRefresh.kt` | derive + rebuild helper; optional pref reset |
| `StandardRewardCurrencySync.kt` | calls refresh on pulverized currency gain |
| `ArmoryAndLeggerySync.kt` | calls refresh when visit learn mutates data |
| `GameRuntimeLibrary.AshP181Batch.kt` / `AshP182Batch.kt` | batch markers |
| `GameRuntimeLibrary.kt` | `REVISION = "phase193"` |

## Tests

- `ArmoryAndLeggerySyncTest` — `getPulverization(11504)` after visit learn refresh
- `GameRuntimeLibraryAshP181Test` — revision pin + refresh wiring
- `GameRuntimeLibraryAshP182Test` — validate v28 matrix
- AshP revision pins bulk-updated to `phase193`

**Verification:** `.\gradlew.bat :shared:jvmTest` — 3,375 tests passing.

## Non-goals (carried forward)

- Full `pulverize.txt` bitmask loader + live ASH `pulverize(item)`
- Armory meat NPC row visit overlay
- Desktop session-log `StandardRewardDatabase.toData()` on armory visit learn
