# Phase 323 — DreadScrollManager v1

**Track:** TCRS `applyModifiers` v64  
**Revision:** `phase323`

## Summary

Ports desktop `DreadScrollManager`: eight Mer-kin dreadscroll clue parsers, pref sync (`dreadScroll1`–`8`, `workteaClue`, `dreadScrollGuesses`), visit/use/skill wiring, and `dreadscroll` CLI.

## Delivered

- `DreadScrollManager.kt` — `ClueType`, `CLUE_DATA`, all handle* parsers, `setClue`/`getClues`/`getScrollText`/`recordFailure`/`applyFromResponse`
- `GameRuntimeLibrary.processVisitResponseHooks` — fight kill/heal scroll, choice 703 failure, choice 704 library, inv_use knucklebone, skills Deep Dark Visions
- `UseItemRequest` knucklebone post-use sync + DI for `Preferences`/`SessionLogger`/`GameEventBus`
- `SkillManager.cast` Deep Dark Visions (skill 90) post-cast sync + DI for `Preferences`/`SessionLogger`
- `SushiConsumptionSync.handleWorktea` delegates to `DreadScrollManager` (sets `dreadScroll7` + `workteaClue`)
- `dreadscroll` CLI via `GameRuntimeLibrary.DreadScroll.kt`
- Tests: `DreadScrollManagerTest`, `GameRuntimeLibraryAshP323Test`, updated `SushiConsumptionSyncTest`

## Deferred Phase 324+

- `DreadScrollManager.decorate()` choice-UI highlighting
- Choice 703 success path (`isMerkinHighPriest`, dreadscroll item consumption)
- lazy-load class/sign TCRS files (document as non-goal)
