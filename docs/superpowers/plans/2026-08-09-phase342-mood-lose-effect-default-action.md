# Phase 342 — Mood lose_effect default-action routing (TCRS applyModifiers v83)

## Context

Phase 341 completed uneffect item-acquisition probes (`phase341`, TCRS v82). The highest-impact remaining mood deferral from Phases 340–341 is desktop `MoodManager.getDefaultAction("lose_effect", …)` fallbacks via statuseffects default actions and mood-library known sources.

## Delivered

1. **`MoodRemovalKnownSources`** — desktop `MoodTrigger.knownSources` registry (`register`/`getKnownSources`/`clear`/`rebuildFromLibrary`)
2. **`MoodManager.getDefaultAction`** — `lose_effect` branch: active mood removal trigger → `EffectDefinitionProxy.getDefaultAction` → known sources pipe-join
3. **Registry wiring** — `addMoodToLibrary`/`loadMoodLibrary` populate known sources from library `lose_effect` removal triggers
4. **Tests** — `MoodRemovalKnownSourcesTest`, extended `MoodManagerDefaultActionTest`

## Deferred (Phase 343+)

| Item | Reason |
|------|--------|
| `minimalSet` / `maximalSet` mood autofill | Separate mood-library automation |
| `lose_effect` trigger execution in `executeActiveMood` | Desktop second-pass non-skill execution; no full effect stack |
| Desktop `username_moods.txt` file import | Separate persistence track |
| CharPane / `extend` CLI wiring | Desktop UI consumers of lose_effect default action |

## Verification

```powershell
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodManagerDefaultActionTest"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodRemovalKnownSourcesTest"
```
