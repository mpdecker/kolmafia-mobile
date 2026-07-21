# Phase 117: AshP75 Summoning Chamber demon summon HTTP

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase117`

## Goal

Port desktop Summoning Chamber demon summon HTTP and wire the `summon` CLI, completing the Intergnat demon workflow (`demonName12` sync from Phase 115).

## Deliverables

| Area | Change |
|------|--------|
| `DemonTypes.kt` | Desktop `DEMON_TYPES` table + item IDs |
| `SummoningChamberRequest.kt` | Visit chamber, POST choice 922, `parseResponse` |
| `SummoningChamberManager.kt` | Daily gate, name resolution, Intergnat swap, item consume |
| `summon` CLI | `GameRuntimeLibrary.Summon.kt` |
| REVISION | `phase117` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred (Phase 118+)

- Quest-log sync for `demonName1`–`demonName14`
- Yeg (`demonName13`) + demon-in-combat (`demonName14`) tracking
- Inventory beeotch / Maximizer Evaluator unified port
