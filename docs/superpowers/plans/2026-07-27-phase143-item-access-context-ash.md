# Phase 143: AshP101 Item Access Context Parity

**Date:** 2026-07-27  
**Revision:** `phase143`  
**Tests:** 2,672 (full suite)

## Goal

Complete AshP98 ItemAvailability parity: populate storage/stash in `buildCheckContext`, Hermit eleven-leaf clover coinmaster gate, and centralize physical accessible-count logic for `available_amount` / `retrieve_item` check-only.

## Delivered

### AccessibleItemCount

- [`AccessibleItemCount.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/inventory/AccessibleItemCount.kt) — desktop-style physical count (inventory + closet + storage + display + stash + equipped)
- [`OutfitManager.accessibleCount`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/equipment/OutfitManager.kt) delegates to helper

### ASH rewire

- [`GameRuntimeLibrary.physicalAccessibleCount`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt) — shared count for `available_amount`, AshP8 int overload, `retrieve_item(..., false)`
- [`buildCheckContext()`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt) — populates `storageItemIds`, `stashItemIds`, `hermitCloverCount`

### Hermit clover + ItemAvailability

- [`HermitRequest.fetchCloverCount`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/HermitRequest.kt) — parse hermit.php stock; zombie path via `_zombieClover0`
- [`CheckContext.hermitCloverCount`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/DynamicItemModifierSync.kt)
- [`ItemAvailability.canUseCoinmasters`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/inventory/ItemAvailability.kt) — blocks 11-leaf clover when Hermit stock is 0

### Tests

- [`AccessibleItemCountTest.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/inventory/AccessibleItemCountTest.kt)
- Extended [`ItemAvailabilityTest.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/inventory/ItemAvailabilityTest.kt) — storage/stash/clover gates
- Extended [`DynamicItemModifierSyncTest.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/quest/DynamicItemModifierSyncTest.kt) — ring from storage
- [`HermitRequestCloverTest.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/HermitRequestCloverTest.kt)
- [`GameRuntimeLibraryAshP101Test.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryAshP101Test.kt)

## Deferred (Phase 144+)

- General battle `learnSkillFromResponse`
- Deeper `getAccessibleCount` parity (familiar-equipped, hat trick, path gates)
- Expand `AshCompatibilityCorpusTest` assertions
