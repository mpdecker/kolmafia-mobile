# Phase 30: CLI Depth + Quest Guild Sync + Maximizer Storage Pulls

*Completed: 2026-06-11*

## Summary

Three-track batch closing script-compat echoes, guild/NPC quest sync, and Maximizer collection pulls.

## Track A: CLI Echo Cleanup

- Fixed `set location` shadowed by generic `set` pref handler
- Added aliases: `acquire`/`find`, `wear`/`wield`, `make`/`bake`/…, `pull`/`hagnk`, `restore`/`check`, `shrug`/`remedy`, `remove`, `camp`, `inventory`, `breakfast`, `searchmall`, `reprice`, `checkpoint`
- Guild NPC shortcuts: `guild paco|ocg|scg|challenge`
- `equip familiar` slot token maps to `familiarequip`
- Wired `BreakfastManager` into `GameRuntimeLibrary` DI

## Track B: Quest NPC-Visit Sync

- `visit_url` GET/POST now calls `QuestLogSync.processResponse`
- Expanded `QuestAdvanceRules` with guild quest signals (MEATCAR, CITADEL, EGO, NEMESIS partial, MYST/MOXIE/MUSCLE)
- Corrected misc NPC signals (SEA_OLD_GUY, PIRATEREALM, SEA_MONKEES)
- Expanded `SYNC_SIGNALS` for guild/NPC dialogue

## Track C: Maximizer Step 2

- `MaximizerManager` scores closet + storage via `fetchContents`
- Pulls via `ClosetRequest.takeOut` / `StorageRequest.withdraw` before equip
- `OutfitCheckpoint` snapshot + restore on failure
- DI: explicit `MaximizerManager` factory with closet/storage

## Verification

- `:shared:jvmTest` — 1,505 tests passing
- `:androidApp:assembleDebug` — success
