# Phase 119: AshP77 Yeg demonName13 scrap syllable sync

**Date:** 2026-06-20  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase119`

## Goal

Port desktop cargo-cult scrap syllable tracking to populate `demonName13` (Yeg), completing the demon-name pipeline deferred from Phase 118.

## Deliverables

| Area | Change |
|------|--------|
| `CultShortsDatabase.kt` | Load scrap pocket order from `cultshorts.txt` |
| `YegDemonNameSync.kt` | `parsePocketPick`, pref round-trip, `updateYegName` |
| `ChoiceRequest.kt` | Optional extra form fields (e.g. `pocket`) |
| Hooks | `processVisitResponseHooks` + `AdventureManager` choice 1420 |
| REVISION | `phase119` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred (Phase 120+)

- `demonName14` combat segment tracking + `demons solve14` CLI
- Modifier-expression QUEST_LOG rules (`royalty`, etc.)
- Full cargo-cult automation (`cargo` CLI, `picked_scraps` ASH)
- Maximizer unified Evaluator (Tier 2)
