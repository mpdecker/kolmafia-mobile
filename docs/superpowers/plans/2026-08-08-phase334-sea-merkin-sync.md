# Phase 334 — SeaMerkin visit sync (TCRS applyModifiers v75)

**Track:** TCRS `applyModifiers` v75  
**Revision:** `phase334`

## Goal

Port desktop `SeaMerkinRequest` temple empty + colosseum visit pref sync deferred from Phase 333: Sea-path alternate Mer-kin completion and revisit detection on `sea_merkin.php` / colosseum adventure visits.

## Delivered

- `SeaMerkinSync` — `parseTemple`/`parseColosseum` mirroring desktop `SeaMerkinRequest.parseResponse`/`parseColosseumResponse`
- `GameRuntimeLibrary.processVisitResponseHooks` — `sea_merkin.php?action=temple` + `adventure.php?snarfblat=210` wiring
- `SeaMerkinSyncTest` — 9 tests (temple sea-path left/right, non-sea empty, wrong action, colosseum admirers/high-priest, quest-done no-op, wrong snarfblat, hook routing)

## Deferred (Phase 335+)

- `DreadScrollManager.decorate()` choice-UI highlighting
- Adventurer of Leisure (5011) `UneffectRequest.reset()` tail from Phase 330
- `handleGuildQuests` meatcar/citadel/factory quest progress side effects (`lastDesertUnlock`, envelope consumption)
