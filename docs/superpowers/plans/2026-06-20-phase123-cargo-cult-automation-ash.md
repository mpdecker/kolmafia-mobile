# Phase 123: AshP81 Cargo-cult automation MVP

**Date:** 2026-06-20  
**Track:** Tier 1 ASH behavioral parity / CLI long-tail  
**REVISION:** `phase123`

## Goal

Port cargo-cult picked-pocket tracking, inspect sync, basic `cargo` CLI, and `picked_pockets`/`picked_scraps` ASH on top of Phase 119 Yeg scrap sync.

## Deliverables

| Area | Change |
|------|--------|
| `CargoPocketSync.kt` | `cargoPocketsEmptied` ROA load/save, inspect inverse-mapping, pick recording |
| `YegDemonNameSync.kt` | Scrap-only; public `checkScrapPocket` |
| `CargoCultistShortsRequest.kt` | HTTP inspect + pick via choice 1420 |
| `CargoCultManager.kt` | `cargo` CLI: empty list, inspect, pick, demon |
| `GameRuntimeLibrary.AshP81Batch.kt` | `picked_pockets()`, `picked_scraps()` |
| REVISION | `phase123` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred (Phase 124+)

- Full `PocketDatabase` port + remaining cargo ASH (`pick_pocket`, `pocket_*`, etc.)
- Monster pocket fight registration
- DESC_ITEM consequence expressions
- Maximizer unified Evaluator (Tier 2)
