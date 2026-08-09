# Phase 344 — Mood lose_effect trigger execution (TCRS applyModifiers v85)

## Context

Phase 343 completed mood autofill `minimalSet`/`maximalSet` (`phase343`, TCRS v84). The highest-impact deferral is desktop `MoodManager.execute()` second-pass removal trigger execution (`lose_effect`/`gain_effect`/`unconditional` CLI actions).

## Goal

Execute effective removal triggers from `executeActiveMood` with desktop `MoodTrigger.shouldExecute(0)` gating and cast/use/hottub/CLI action routing.

## Deliverables

1. **`MoodRemovalTriggerExecution`** — `shouldExecute`, `unstackableAction`, sorted execution order, cast/use/hottub/CLI dispatch
2. **`MoodManager.executeActiveMood`** — run removal triggers after buff maintenance; optional `cliExecutor` for non-cast actions
3. **`GameRuntimeLibrary`** — wire `moodManager.cliExecutor` → `dispatchCli`
4. **Tests** — `MoodRemovalTriggerExecutionTest`

## Deferred (Phase 345+)

| Item | Reason |
|------|--------|
| Desktop `username_moods.txt` import | Separate persistence track |
| `mood_execute` multiplicity > 0 lose_effect always-execute | ASH mood_execute pass |
| `Evaluator.cannotGainEffect` gate | Maximizer/effect gain parity |
| AT song two-pass isSkill ordering | v1 single pass with shared slot eviction |

## Verification

```powershell
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodRemovalTriggerExecutionTest"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodManagerTest"
```
