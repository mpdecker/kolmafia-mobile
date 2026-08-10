# Phase 335 — Guild quest side effects (TCRS applyModifiers v76)

**Track:** TCRS `applyModifiers` v76  
**Revision:** `phase335`

## Goal

Close deferred guild quest side effects from Phase 334: wire `lastDesertUnlock` on MEATCAR completion and consume the factory envelope on paco turn-in.

## Delivered

- `DesertBeachUnlockSync` — desktop `KoLCharacter.setDesertBeachAvailable()` → `lastDesertUnlock=ascension`
- `GuildQuestSync` — paco visit `"South of the Border"` desert unlock + factory envelope turn-in via shared `QuestLogSync.applyFactoryTurnIn`
- `QuestLogSync` — `consumeItem` callback on `QuestSyncContext`; internal `applyFactoryTurnIn`; MEATCAR/MACGUFFIN derived desert unlock in `applyDerivedQuestPrefs`
- `GuildVisitSync`/`GameRuntimeLibrary.buildQuestSyncContext` — guild.php hook + inventory consume wiring
- `GuildQuestSyncTest` (5 tests) + `QuestLogSyncTest` extensions (2 tests)

## Deferred (Phase 336+)

- `DreadScrollManager.decorate()` choice-UI highlighting
- Adventurer of Leisure (5011) `UneffectRequest.reset()` tail from Phase 330
- EGO key / guild challenge item consumption on guild visits
