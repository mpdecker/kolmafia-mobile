# Phase 327 — BarrelShrineSync (TCRS applyModifiers v68)

**Track:** TCRS `applyModifiers` v68  
**Revision:** `phase327`

## Goal

Wire choice 1100 / da.php visit-hook pref sync so `_barrelPrayer`, `prayedFor*`, and `barrelShrineUnlocked` stay accurate after manual shrine visits, complementing Phase 326 `BarrelCreateRequest`.

## Scope

- `BarrelShrineSync` — `syncFromVisit`, `syncPostChoice`, `syncUnlockFromHtml`
- Visit hooks in `GameRuntimeLibrary.processVisitResponseHooks`
- Choice-loop hooks in `AdventureManager` + `GameRuntimeLibrary.cliChoice`
- `BarrelCreateRequest` concoction refresh tail on success
- Tests + docs/revision bump

## Delivered

- `BarrelShrineSync` — desktop ChoiceControl case 1100 visit/post-choice + dungeon unlock
- `GameRuntimeLibrary.processVisitResponseHooks` — barrelshrine URL/HTML + choice 1100 detection
- `AdventureManager` + `cliChoice` — visit sync before dispatch, post-choice sync after choose
- `BarrelCreateRequest` — `ConcoctionDatabase.refreshConcoctionsNowFromLastContext()` on success
- `BarrelShrineSyncTest` (6 tests)

## Deferred (Phase 328+)

- `barrelprayer` CLI (option 4 buff path)
- `guild.php` visit-hook sync for manual `malussmash`
- `DreadScrollManager.decorate()` / choice 703 quest sync
