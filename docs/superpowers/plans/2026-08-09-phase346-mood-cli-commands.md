# Phase 346 — MoodCommand CLI parity (TCRS applyModifiers v87)

## Context

Phase 345 (`phase345`, TCRS v86) threaded `mood_execute`/`mood repeat` multiplicity. Remaining MoodCommand gaps from desktop [`MoodCommand.java`](C:/Development/kolmafia/kolmafia/src/net/sourceforge/kolmafia/textui/command/MoodCommand.java) were list/listall/clear and the `mood <name> [<n>]` temporary-set/repeat/restore flow.

## Goal

Port desktop MoodCommand subcommands and align `mood <name>` set-vs-execute behavior.

## Deliverables

1. **`MoodManager`** — `formatTriggerLine`, `activeTriggerLines`, `clearActiveTriggers`, `libraryDisplayNames`
2. **`GameRuntimeLibrary` cliDispatch** — `mood`/`mood list`, `mood listall`, `mood clear`; refactored `mood <name> [<n>]` with repeat-then-restore; `"Mood swing complete."` on execute/repeat
3. **Tests** — `MoodCliCommandTest`, `MoodManagerTest` helper coverage

## Behavior change

`mood <name>` without a numeric suffix is set-only (no implicit execute). Use `mood execute` or `mood repeat N` to run triggers.

## Deferred (Phase 347+)

| Item | Reason |
|------|--------|
| `editmood` CLI | Separate EditMoodCommand track |
| `save as mood` CLI | Desktop calls `minimalSet` |
| `{username}_moods.txt` import/export | Separate persistence track |
| `Evaluator.cannotGainEffect` gate | Maximizer dependency |

## Verification

```powershell
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodCliCommandTest"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodCliMultiplicityTest"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodManagerTest"
```
