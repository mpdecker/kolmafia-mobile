# Phase 332 — DreadScroll gladiator use (TCRS applyModifiers v73)

**Track:** TCRS `applyModifiers` v73  
**Revision:** `phase332`

## Goal

Complete the deferred Mer-kin dreadscroll gladiator-champion use path (sigil burn) deferred from Phase 331.

## Delivered

- `DreadScrollManager` — `GLADIATOR_SIGIL_SUCCESS`, `handleGladiatorChampionSuccess()`, `parseDreadscrollUse()` router
- `UseItemRequest` + `GameRuntimeLibrary.processVisitResponseHooks` — inv_use dreadscroll → `parseDreadscrollUse` (choice 703 stays high-priest only)
- `DreadScrollManagerTest` — 5 new tests (gladiator success, skip when done, no-op, router, high-priest precedence)

## Deferred (Phase 333+)

- `DreadScrollManager.decorate()` choice-UI highlighting
- Adventurer of Leisure (5011) `UneffectRequest.reset()` tail from Phase 330
- `handleGuildQuests` meatcar/citadel/factory quest progress
- Mer-kin quest choices 709/713/717 (`merkinQuestPath=done`) sync
