# Phase 121: AshP79 Allied Radio HTTP + alliedradio CLI

**Date:** 2026-06-20  
**Track:** Tier 1 ASH behavioral parity / CLI long-tail  
**REVISION:** `phase121`

## Goal

Complete the demon-14 Allied Radio workflow by porting full HTTP automation and the `alliedradio` CLI (Phase 120 only wired passive grey-text segment parsing on choices 1561/1563).

## Deliverables

| Area | Change |
|------|--------|
| `AlliedRadioRequest.kt` | Backpack drop, handheld use, choice POST, battery/postChoice parsers |
| `AlliedRadioManager.kt` | Uses remaining, subcommand routing, orchestration |
| `GameRuntimeLibrary.AlliedRadio.kt` | `alliedradio` CLI dispatch |
| Hooks | Battery sync on choice 1561 visit (AdventureManager + processVisitResponseHooks) |
| REVISION | `phase121` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred (Phase 122+)

- Modifier-expression QUEST_LOG rules (`royalty=[$1]`)
- Full cargo-cult automation (`cargo` CLI, `picked_scraps` ASH)
- Maximizer unified Evaluator (Tier 2)
