# Phase 146: AshP104 AccessibleItemCount Storage Gates Parity

## Summary

Closed Phase 145 deferrals for desktop `InventoryManager.getAccessibleCount` physical semantics: Thrifty storage gate, ronin freepull counts, and hardcore/softcore `ItemDatabase.isAllowed` early exit for `available_amount` and retrieve check-only.

## Delivered

- **`RestrictedItemType.kt`** — enum mirroring desktop restricted-item sections
- **`RestrictedItemsParse.kt`** — shared HTML parse for thrifty.php / standard.php
- **`ThriftyRequest.kt`** — Thrifty allowed-items cache + `refresh()` / `isAllowed`
- **`StandardRequest.kt`** — Standard restricted-items cache + `isAllowedInStandard`
- **`ItemRestriction.kt`** — `isAllowed()` for restricted-path physical counts
- **`StoragePullRules.kt`** — freepull/nopull classification + path-specific item ids
- **`StorageRequest.kt`** — `fetchClassifiedContents()`, freepull/storage split, `canUseStorage()`
- **`ModifierDatabase.hasBooleanModifier()`** — Free Pull / No Pull tag lookup
- **`PullableItems.storagePullAllowed`** — Thrifty gate via `ThriftyRequest.isAllowed`
- **`AccessibleItemCount.physicalCount`** — `ItemRestriction` early exit; freepull + gated storage counts
- **`GameRuntimeLibrary`** — `thriftyRequest`/`standardRequest` DI; `buildCheckContext` refresh hooks
- **`SharedModule.kt`** — DI wiring for Thrifty/Standard requests
- **Tests** — `ThriftyRequestTest`, `StandardRequestTest`, `StorageFreepullTest`, extended `AccessibleItemCountTest`, `GameRuntimeLibraryAshP104Test`, corpus `available_amount` LoL gate snippet
- **`REVISION`** — `phase146` (2,703 tests)

## Deferred (Phase 147+)

- Full `StandardRequest.isAllowed` path rules (Trendy, Thrifty item *use* via `LAST_AVAILABLE_DATE`, Quantum familiar exception)
- Expand `AshCompatibilityCorpusTest` behavioral assertions (bulk Tier 1 backlog)
- AshP8–P18 interactive/PvP stubs
