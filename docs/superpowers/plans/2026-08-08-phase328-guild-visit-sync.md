# Phase 328 — GuildVisitSync v1 (TCRS applyModifiers v69)

**Track:** TCRS `applyModifiers` v69  
**Revision:** `phase328`

## Goal

Close the guild.php visit-sync gap deferred from Phases 324 and 327: set `lastGuildStoreOpen` from live visits, post-response ingredient accounting for manual/automated `malussmash` and `makestaff`, and wire hooks so MALUS/STAFF gates and `guild_store_available` ASH behave correctly.

## Delivered

- `GuildVisitSync` — `syncStoreOpen` (`lastGuildStoreOpen` when HTML contains `shop.php`) + `parseFromVisit` dispatcher
- `GuildCreationSync` — `parseMalus` (5× powder per smash) + `parseStaff` (all recipe ingredients) + `registerStaffRequest` session-log line
- `ConcoctionDatabase.chefStaffByBaseItemId` / `malusByIngredientItemId` lazy indexes
- `GameRuntimeLibrary.processVisitResponseHooks` guild.php wiring
- `MalusCreateRequest` / `StaffCreateRequest` success tails via `GuildCreationSync` + `eventBus`/`sessionLogger` DI
- `GuildVisitSyncTest` (8 tests)

## Deferred (Phase 329+)

- `barrelprayer` CLI (da.php + choice 1100 automation)
- Guild `buyskill` meat/skill sync + concoction refresh
- Guild quest handlers (`handleGuildQuests` meatcar/citadel/etc.)
- `DreadScrollManager.decorate()` + choice 703 quest UI sync
