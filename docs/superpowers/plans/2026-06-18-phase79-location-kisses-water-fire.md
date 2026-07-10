# Phase 79: AshP37 Location Kisses/Water/Fire Bracket Fields

**Date:** 2026-06-18  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase79`

## Goal

Complete Phase 78 deferral: live `$location["kisses"]`, `$location["water_level"]`, and `$location["fire_level"]` bracket fields, with stretch `volcano slime` CLI.

## Deliverables

| Area | Change |
|------|--------|
| Water level | `AdventureZone.waterLevel` + `Level:` parse + env/stat heuristics in `AdventureDatabase` |
| Path gates | `CharacterState.isRaincore` / `isFirecore` gate `water_level` / `fire_level` reads |
| Kisses | `DreadKissesTracker` + fight HTML kiss parse in `AdventureManager.resolveCombat` |
| Fire level | `WildfireCampManager` + captain HTML parse on wildfire visit hooks |
| Brackets | `LocationEntityFields` live `kisses`/`water_level`/`fire_level` + DI wiring |
| Stretch | `volcano slime` CLI → `volcanoisland.php?action=npc&subaction=getslime` |
| Tests | `GameRuntimeLibraryAshP37Test`, `DreadKissesTrackerTest`, `WildfireCampManagerTest`, corpus + `AdventureManagerTest` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- `bastille.txt` loader/manager (Tier 3)
- `numeric_modifier(monster, …)` — no bundled monster modifier data
- Full Wildfire choice automation (`choice 1451`) — only captain parse needed for reads
