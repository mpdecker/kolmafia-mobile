# Phase 122: AshP80 QUEST_LOG modifier-expression consequences

**Date:** 2026-06-20  
**Track:** Tier 1 ASH behavioral parity / quest sync  
**REVISION:** `phase122`

## Goal

Enable QUEST_LOG consequence actions with `[...]` modifier expressions, closing the last skipped rule in `consequences.txt` (`royalty=[$1]`).

## Deliverables

| Area | Change |
|------|--------|
| `ConsequenceActionResolver.kt` | Group `$N` substitution, bracket eval via `ModifierExpression`, numeric pref typing |
| `QuestLogConsequenceDatabase.kt` | `SetExpressionValue` action; no longer skip `[` actions |
| `QuestLogConsequenceSync.kt` | Wire expression actions through resolver |
| `ModifierExpression.kt` | `stripcommas()` function |
| `Preferences.kt` | `ROYALTY` constant |
| REVISION | `phase122` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred (Phase 123+)

- Cargo-cult automation (`cargo` CLI, `picked_scraps`/`picked_pockets` ASH)
- DESC_ITEM consequence expressions (broader `[...]` rules)
- Maximizer unified Evaluator (Tier 2)
