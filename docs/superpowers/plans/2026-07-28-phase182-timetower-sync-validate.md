# Phase 182: AshP159 Time Tower Shop Sync + AshP160 Validate v17

**Revision:** `phase182`  
**Follows:** Phase 181 (`REVISION = "phase181"`, AshP157/AshP158 Drip Armory)

## AshP159 — Shop visit sync v7 (Time Tower)

- `TimeTowerSync.kt` — sets `timeTowerAvailable` from 8 Chroner coin shops + `place=twitch`
- `CoinmasterShopSync` — Chroner shop branches
- `GameRuntimeLibrary.processVisitResponseHooks` — `place=twitch` hook
- Swagger adjunct — `CoinmasterDatabase` override + `CoinmasterManager.visit()` peevpee path

## AshP160 — Coinmaster validate v17

- `TimeTowerAccessibility` — per-shop inaccessible messages when tower unavailable
- `StoragePullRules` — toolbelt (7566) free-pull gated on `timeTowerAvailable` pref
- Corpus: `corpus_alliedHqFlakShieldValidate_live` (item 11920, ROW1599)

## Deferred

- Storage list migration (moving toolbelt between storage/freepull buckets)
- MerchTable dynamic row refresh, trapper/lathe/september/junkmagazine sync
- SBB/vending/tinkering bench validate gates
