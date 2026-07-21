# Phase 120: AshP78 demonName14 segment tracking + demons CLI

**Date:** 2026-06-20  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase120`

## Goal

Complete the demon summon name pipeline by tracking Allied Radio grey-text segments for demon 14, porting the graph solver, and wiring the `demons` / `demons solve14` CLI.

## Deliverables

| Area | Change |
|------|--------|
| `DemonInCombatNameSync.kt` | Segment merge, grey-text parse, prefs |
| `DemonName14Manager.kt` | Graph DFS solver port |
| `DemonNamesManager.kt` + `GameRuntimeLibrary.Demons.kt` | `demons` / `demons solve14` CLI |
| Hooks | `processVisitResponseHooks` + `AdventureManager` choice 1561/1563 |
| REVISION | `phase120` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred (Phase 121+)

- Full Allied Radio HTTP + `radio` CLI
- Modifier-expression QUEST_LOG rules (`royalty`, etc.)
- Full cargo-cult automation
- Maximizer unified Evaluator (Tier 2)
