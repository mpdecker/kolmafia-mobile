# Phase 333 — Mer-kin quest done sync (TCRS applyModifiers v74)

**Track:** TCRS `applyModifiers` v74  
**Revision:** `phase333`

## Goal

Close the deferred Mer-kin boss-victory choice sync from Phase 332: set `merkinQuestPath=done` and Shub/Yog-Urt defeat prefs on choices 709/713/717.

## Delivered

- `MerkinQuestSync` — `applyFromChoice`/`applyFromUrl` for choices 709/713/717
- `GameRuntimeLibrary.processVisitResponseHooks` — Mer-kin quest done hook via `applyFromUrl`
- `DreadScrollManager.applyFromResponse` — choice 709/713/717 routing
- `MerkinQuestSyncTest` — 6 tests (709/713/717 success, no-op, URL + applyFromResponse routing)

## Deferred (Phase 334+)

- `SeaMerkinRequest` temple empty + colosseum visit pref sync (Sea-path alternate completion)
- `DreadScrollManager.decorate()` choice-UI highlighting
- Adventurer of Leisure (5011) `UneffectRequest.reset()` tail from Phase 330
- `handleGuildQuests` meatcar/citadel/factory quest progress
