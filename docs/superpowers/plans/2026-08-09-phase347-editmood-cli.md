# Phase 347 — editmood + save as mood CLI (TCRS applyModifiers v88)

## Context

Phase 346 (`phase346`, TCRS v87) ported MoodCommand subcommands. Remaining mood CLI gaps were desktop [`EditMoodCommand`](C:/Development/kolmafia/kolmafia/src/net/sourceforge/kolmafia/textui/command/EditMoodCommand.java) and [`SaveAsMoodCommand`](C:/Development/kolmafia/kolmafia/src/net/sourceforge/kolmafia/textui/command/SaveAsMoodCommand.java).

## Goal

Wire `editmood`/`trigger` removal-trigger editing and `save as mood` minimalSet persistence on mobile.

## Deliverables

1. **`MoodManager`** — `formatRemovalTriggerLine`, `activeRemovalTriggerLines`, `activeEditMoodLines`, `addActiveRemovalTrigger`, `clearAllActiveTriggers`
2. **`EditMoodCommandParser`** — desktop comma syntax `[type,] effect [, action]`
3. **`GameRuntimeLibrary` cliDispatch** — `editmood`/`trigger` handlers + `save as mood`
4. **Tests** — `MoodEditMoodCliTest`, `MoodManagerTest` helper coverage

## Deferred (Phase 348+)

| Item | Reason |
|------|--------|
| `{username}_moods.txt` import/export | Separate file persistence track |
| `Evaluator.cannotGainEffect` gate | Maximizer dependency |
| AT song two-pass ordering | Execution ordering polish |

## Verification

```powershell
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodEditMoodCliTest"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodAutofillTest"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodCliCommandTest"
```
