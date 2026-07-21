# Phase 118: AshP76 quest-log demon name sync

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase118`

## Goal

Wire quest-log page 3 accomplishments parsing from bundled `consequences.txt` to populate `demonName1`–`demonName14` prefs (and other simple QUEST_LOG rules), completing the summon name pipeline from Phases 115–117.

## Deliverables

| Area | Change |
|------|--------|
| `QuestLogConsequenceDatabase.kt` | Load simple QUEST_LOG rules from `consequences.txt` |
| `QuestLogConsequenceSync.kt` | `applyAccomplishments` on page 3 HTML |
| `QuestLogRequest.parsePage` | Hook when `which == 3` |
| REVISION | `phase118` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred (Phase 119+)

- Yeg `demonName13` scrap syllable sync
- `demonName14` combat segment tracking
- Modifier-expression QUEST_LOG rules (`royalty`, etc.)
- Inventory beeotch / Maximizer Evaluator unified port
