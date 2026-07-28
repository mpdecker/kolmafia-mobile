# Phase 197: AshP189 Armory Visit Learn v2 + AshP190 desc() HTTP Prefetch

## Summary

Closed Phase 196 deferrals: armory visit now logs desktop-style `toData()` lines and registers meat NPC rows at runtime; `desc()` prefetches item/effect/skill descriptions over HTTP on cache miss.

## Delivered

- **`StandardRewardDatabase.toData()`** — reward + pulverized tab-separated export lines
- **`SessionLogger.appendRawLine()`** — session-log spading output parity
- **`NpcStoreVisitOverlay`** — runtime armory meat row overlay wired into `NpcStoreDatabase`
- **`ArmoryAndLeggerySync`** — session-log emission + meat overlay on visit; `CoinmasterShopSync` passes `SessionLogger`
- **`EntityIntrospection.entityDesc`** — cache-miss HTTP fetch via internal `visitKolPage` / `fetchDescription`
- **`GameRuntimeLibrary.AshP189Batch.kt`** / **`AshP190Batch.kt`** — batch markers; `REVISION = phase197`

## Tests

- `StandardRewardDatabaseTest`, `NpcStoreVisitOverlayTest`, extended `ArmoryAndLeggerySyncTest`
- `EntityIntrospectionTest`, `GameRuntimeLibraryAshP189Test`, `GameRuntimeLibraryAshP190Test`
- Corpus: `corpus_descItem_prefetchOnMiss`, `corpus_armoryMeatNpc_afterVisitOverlay`
- **3,428 tests** (was 3,415 at Phase 196)

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
```
