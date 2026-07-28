# Phase 140: AshP98 itemAvailable accessibility gates

**Date:** 2026-07-22  
**Revision:** `phase140`  
**Tests:** 2,649

## Goal

Port desktop `InventoryManager.itemAvailable` and its `canUse*` pref/limit-mode gates so dynamic modifier desc visits (starting with `checkRing`) treat mall-buyable / NPC-sold / coinmaster-sold items as accessible when prefs allow.

## Delivered

### ItemAvailability + LimitModeGates

- [`LimitModeGates.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/inventory/LimitModeGates.kt) — limitMode string → mall/NPC/coinmaster/storage/clan/campground limits (spelunky/batman/ed)
- [`ItemAvailability.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/inventory/ItemAvailability.kt) — `itemAvailable` OR-chain + `canUseMall/NPC/Coinmaster/Storage/Closet/Stash`

### DynamicItemModifierSync + GameRuntimeLibrary

- Extended [`CheckContext`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/DynamicItemModifierSync.kt) with `storageItemIds`, `stashItemIds`, `limitMode`, `canInteract`, `hasClan`
- [`checkRing`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/DynamicItemModifierSync.kt) uses `ItemAvailability.itemAvailable` instead of `isAccessible`
- [`buildCheckContext()`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt) populates limitMode/hasClan/canInteract from character state

### Tests

- [`ItemAvailabilityTest.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/inventory/ItemAvailabilityTest.kt) — inventory, mall/NPC/coinmaster prefs, spelunky limit, closet pref + ed limit
- Extended [`DynamicItemModifierSyncTest.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/quest/DynamicItemModifierSyncTest.kt) — ring mall-available + closet pref cases
- [`GameRuntimeLibraryAshP98Test.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryAshP98Test.kt) — `revision_phase140`

## Deferred (Phase 141+)

- Post-bird `skillManager.fetchSkills()` / explicit `learnSkill`
- Codpiece equip HTTP automation (`inventory.php?action=docodpiece`)
- Rewire all `available_amount` retrieve semantics
- Full async storage/stash fetch in every `buildCheckContext()` call
- Eleven-leaf clover coinmaster special case (Hermit clover count)

## Notes

- `isAccessible` unchanged for owned-item desc checks (closet-inclusive, no pref gates)
- Closet ring desc now requires `autoSatisfyWithCloset=true` (desktop parity via `canUseCloset`)
