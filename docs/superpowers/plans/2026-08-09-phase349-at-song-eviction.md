# Phase 349 — AT song two-pass mood execution (TCRS applyModifiers v90)

## Context

Phase 348 deferred **AT song two-pass pre-pass eviction**. Mobile buff execution had per-cast eviction but no desktop pre-pass and removal cast actions did not share `locallyEvicted`/`locallyAdded` state.

## Goal

Desktop-parity AT song slot management during `executeActiveMood`:
1. Pre-pass shrugs orphan/extra AT songs before casts
2. Shared `AtSongSlotTracker` spans buff loop and removal cast actions
3. `AtSongEviction` helper centralizes pre-pass + per-cast eviction

## Deliverables

1. **`AtSongEviction.kt`** — `isThiefBuffTrigger`, `computePrePassEvictionIds`, `prePassEvict`, `evictBeforeCast`, `lowestPriorityActiveSong`
2. **`MoodManager.executeActiveMood`** — pre-pass before buff loop; shared tracker into removal execution
3. **`MoodRemovalTriggerExecution.executeCastAction`** — uses `AtSongEviction.evictBeforeCast` with shared tracker
4. **Tests** — `AtSongEvictionTest`, extended `MoodManagerAtSongTest`, `MoodRemovalTriggerExecutionTest` DB setup fix

## Deferred (Phase 350+)

| Item | Reason |
|------|--------|
| `Evaluator.cannotGainEffect` gate | Maximizer dependency |
| Explicit mood import/export CLI | Desktop has none |

## Verification

```powershell
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.AtSongEvictionTest"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodManagerAtSongTest"
.\gradlew.bat :shared:jvmTest
```
