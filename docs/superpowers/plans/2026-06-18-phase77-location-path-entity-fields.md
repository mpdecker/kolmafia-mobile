# Phase 77: AshP35 LOCATION + PATH entity bracket fields

**Date:** 2026-06-18  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase77`

## Goal

Wire `$location[field]` and `$path[field]` bracket reads from bundled adventure/zone/combat data and AscensionPath metadata, completing the AshP32–AshP34 entity-bracket arc.

## Deliverables

| Area | Change |
|------|--------|
| Data | `AdventureZone.forceNoncombat` persisted from `adventures.txt` |
| Fields | `LocationEntityFields` — 22 location bracket fields (bundled data + session stubs) |
| Path | `AscensionPath` metadata (`pathId`, `pathImage`, `pointsPreference`, `avatarPath`, `allowsFamiliars`) + `PathEntityFields` |
| Wiring | `GameRuntimeLibrary.resolveEntityIndex` handles `AshType.LOCATION` and `AshType.PATH` |
| Tests | `GameRuntimeLibraryAshP35Test` + `corpus_locationEntityFields_live` + `corpus_pathEntityFields_live` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- Full runtime for `turns_spent`, `kisses`, `fire_level`, `water_level`, `poison`
- `numeric_modifier(monster, …)` — no monster modifier data
- Tier 3 `bastille.txt` loader/manager
