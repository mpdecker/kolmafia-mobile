# Phase 348 — `{username}_moods.txt` persistence (TCRS applyModifiers v89)

## Context

Phase 347 (`phase347`, TCRS v88) wired `editmood`/`trigger` and `save as mood`. Mobile stored moods only in Preferences; desktop uses `{username}_moods.txt` auto-loaded on login.

## Goal

Add desktop-compatible mood file persistence for cross-install library round-trips.

## Deliverables

1. **`UserDataFileIO`** — `readUserDataText` / `writeUserDataText` expect/actual (jvm/android/ios)
2. **`MoodSettingsFile`** — desktop format parse/serialize with buff+removal import mapping
3. **`MoodManager`** — `loadSettings` / `saveSettings` / `updateFromPreferences`; file write on `saveMoodLibrary`
4. **`SessionManager`** — login calls `updateFromPreferences`
5. **`mood autofill` CLI** — persists after `maximalSet` (desktop parity)

## Deferred (Phase 349+)

| Item | Reason |
|------|--------|
| `Evaluator.cannotGainEffect` gate | Maximizer dependency |
| AT song two-pass pre-pass eviction | Execution ordering polish |
| Explicit `mood import`/`export` CLI | Desktop has none |

## Verification

```powershell
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodSettingsFileTest"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodManagerSettingsTest"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.platform.UserDataFileAppenderTest"
```
