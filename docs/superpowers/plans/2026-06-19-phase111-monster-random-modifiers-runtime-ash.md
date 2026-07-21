# Phase 111: AshP69 runtime `$monster[random_modifiers]`

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase111`

## Goal

Populate `$monster[random_modifiers]` from last-fight state via OCRS parsing and a lightweight MonsterStatusTracker.

## Deliverables

| Area | Change |
|------|--------|
| Combat | `RandomModifierParser` — OCRS + crazySummerModifiers map + MONSTERID |
| Combat | `MonsterStatusTracker` — last-fight instance with modifier list |
| Adventure | Pre-fight hook in `AdventureManager.resolveCombat()` |
| ASH | `MonsterAshRef` for `last_monster()` instance brackets; template `to_monster()` stays empty |
| REVISION | `phase111` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred

- Modifier stat mutations (`handleRandomModifiers` switch)
- Intergnat / mask / dinosaur / hat pipelines
- `translateLeetMonsterName`
- Full MonsterStatusTracker combat APIs
