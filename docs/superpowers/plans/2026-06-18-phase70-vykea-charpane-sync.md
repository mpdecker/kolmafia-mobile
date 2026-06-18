# Phase 70: VYKEA charpane sync + live `my_vykea_companion`

**Date:** 2026-06-18  
**Track:** Subsystem sync (charpane)  
**REVISION:** `phase70`

## Goal

Close parity-audit VYKEA charpane gap: parse companion from charpane HTML, persist desktop-aligned prefs, wire live `my_vykea_companion` via `VykeaCompanionManager` on `visitKolPage("charpane.php")`.

## Deliverables

| Area | Change |
|------|--------|
| Parser | `VykeaCharpaneSync` — outer/inner regex + HTML entity decode |
| Data | `VykeaCompanionData.toAshString` / `toCatalogString` / public `typeFromString` |
| Manager | `VykeaCompanionManager` — `_currentVykea` + `_VYKEACompanion*` prefs |
| Runtime | `GameRuntimeLibrary` charpane hook; `my_vykea_companion` via manager |
| DI | `SharedModule` registers manager |
| Tests | `VykeaCharpaneSyncTest`, `VykeaCompanionManagerTest`, corpus `my_vykea_companion` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred (Phase 71+)

- Pasta thrall charpane sync
- MONSTER entity bracket fields
- `journeyman.txt` loader
- Full desktop Ed `servants` HTML table
