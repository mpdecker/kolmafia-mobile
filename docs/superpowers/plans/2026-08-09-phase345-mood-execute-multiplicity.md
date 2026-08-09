# Phase 345 — mood_execute multiplicity (TCRS applyModifiers v86)

## Context

Phase 344 (`phase344`, TCRS v85) added removal trigger execution in `MoodManager.executeActiveMood` via `MoodRemovalTriggerExecution`. Desktop `mood_execute(multiplicity)` and `mood repeat <n>` pass that value through to cast/use scaling and buff-pass gating.

## Goal

Thread desktop `multiplicity` through `checkpointedExecute`, `executeActiveMood`, removal trigger cast/use scaling, AshP119 `mood_execute`, and `mood repeat` CLI.

## Deliverables

1. **`MoodManager`** — `multiplicity` param on `checkpointedExecute`/`executeActiveMood`; when `multiplicity > 0`, buff pass attempts all effective triggers (not just missing)
2. **`MoodRemovalTriggerExecution`** — `scaledCount(base, multiplicity) = max(base, base * multiplicity)` for cast/use actions
3. **`MoodUneffectActionParser`** — `parseCastCount`/`parseUseCount` helpers
4. **ASH/CLI** — AshP119 `mood_execute(N)` forwards arg; `mood repeat N` CLI before generic `mood (.+)`
5. **Tests** — scaling tests in `MoodRemovalTriggerExecutionTest`, `GameRuntimeLibraryAshP119Test`, `MoodCliMultiplicityTest`

## Deferred (Phase 346+)

| Item | Reason |
|------|--------|
| Desktop `{username}_moods.txt` import/export | Separate persistence track |
| `editmood` / `saveasmood` / `mood list` / `mood clear` CLI | CLI long-tail batch |
| `Evaluator.cannotGainEffect` gate | Maximizer effect-gain parity |
| Full `skillToEffect` special-case switch tree | Autofill v1 reverse-map sufficient |
| `mood <name> <n>` set-then-repeat-then-restore flow | Secondary CLI parity |

## Verification

```powershell
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodRemovalTriggerExecutionTest"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryAshP119Test"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodCliMultiplicityTest"
```
