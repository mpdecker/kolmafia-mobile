# Phase 145: AshP103 AccessibleItemCount Depth Parity

## Summary

Extended `AccessibleItemCount.physicalCount()` toward desktop `InventoryManager.getAccessibleCount` physical semantics for `available_amount` and retrieve check-only paths.

## Delivered

- **`PullableItems.kt`** — `pullableInLoL`, `pullableInSeaPath`, `storagePullAllowed` (Thrifty deferred)
- **`CharacterState`** — `inLegacyOfLoathing`, `inSeaPath`, `isThrifty`, `hatTrickHatIds`
- **`CharacterApiResponse.hats`** — Hat Trick extra hat ids from status API
- **`HermitRequest`** — worthless pseudo-item id 13 aggregate (trinket/gewgaw/knick-knack 43/44/45)
- **`EquippedItemCount.kt`** — worn + Hat Trick + inactive familiar equipment copies
- **`AccessCountContext`** — character, game database, familiar manager for depth counts
- **`FamiliarManager`** — optional `item` field from `api.php?what=familiars`; local equip update
- **`GameRuntimeLibrary.physicalAccessibleCount`** — passes full `AccessCountContext`
- **Tests** — `PullableItemsTest`, extended `AccessibleItemCountTest`, `HermitRequestWorthlessTest`, `GameRuntimeLibraryAshP103Test`
- **`REVISION`** — `phase145` (2,692 tests)

## Deferred (Phase 146+)

- Thrifty storage gate (`ThriftyRequest.isAllowed`)
- Freepulls in accessible count
- `ItemDatabase.isAllowed` hardcore/softcore bans
- Expand `AshCompatibilityCorpusTest` behavioral assertions
