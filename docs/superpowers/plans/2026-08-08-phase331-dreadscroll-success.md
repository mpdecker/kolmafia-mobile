# Phase 331 — DreadScroll choice 703 success (TCRS applyModifiers v72)

**Track:** TCRS `applyModifiers` v72  
**Revision:** `phase331`

## Goal

Close the deferred Mer-kin dreadscroll choice 703 success path from Phase 323: High Priest pref sync, dreadscroll consumption, and visit/use hook wiring.

## Delivered

- `DreadScrollManager` — `DREADSCROLL_ID`, `HIGH_PRIEST_SUCCESS`, `handleHighPriestSuccess()`
- `GameRuntimeLibrary.processVisitResponseHooks` — choice 703 success/failure split + inv_use dreadscroll hook
- `UseItemRequest` — post-use dreadscroll success tail
- `DreadScrollManagerTest` — 4 new tests (success, no-op, failure, applyFromResponse routing)

## Deferred (Phase 332+)

- Dreadscroll gladiator champion use path (`The sigil burned into your forehead`)
- `DreadScrollManager.decorate()` choice-UI highlighting
- Adventurer of Leisure (5011) `UneffectRequest.reset()` tail from Phase 330
- `handleGuildQuests` meatcar/citadel/factory quest progress
