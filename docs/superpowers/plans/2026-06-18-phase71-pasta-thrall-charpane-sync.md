# Phase 71: Pasta thrall charpane sync + live `my_thrall`

**Date:** 2026-06-18  
**Track:** Subsystem sync (charpane)  
**REVISION:** `phase71`

## Goal

Close parity-audit pasta thrall charpane gap: parse active thrall from charpane HTML, update `pastaThrallN` prefs and `_currentThrall`, wire live `my_thrall` via `PastaThrallManager` on `visitKolPage("charpane.php")`.

## Deliverables

| Area | Change |
|------|--------|
| Parser | `PastaThrallCharpaneSync` — desktop regex + HTML entity decode |
| Data | `PastaThrall` — `typeIndex`, `canonicalType`, `writePref` |
| Manager | `PastaThrallManager` — Pastamancer gate, sync/clear `_currentThrall` |
| Runtime | `GameRuntimeLibrary` charpane hook; `my_thrall` via manager |
| DI | `SharedModule` registers manager |
| Tests | `PastaThrallCharpaneSyncTest`, `PastaThrallManagerTest`, corpus `my_thrall` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred (Phase 72+)

- MONSTER entity bracket fields
- Full desktop Ed `servants` HTML table
- `journeyman.txt` loader
- Bulk `AshCompatibilityCorpusTest` expansion
