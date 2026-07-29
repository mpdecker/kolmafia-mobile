# Phase 216: AshP227 JunkList persistence + AshP228 cleanup junk CLI

**Delivered:** 2026-07-29  
**REVISION:** `phase216`  
**Tests:** 3,583

## AshP227 — JunkList persistence

- Bundled desktop `COMMON_JUNK` defaults in `common_junk.txt` (107 item names)
- `JunkListManager` loads/seeds `junkList` pref (pipe-delimited names) on login via `SessionManager`
- Resolves names → item IDs via `GameDatabase`; skips unknown names at load
- DI: `SharedModule` + `GameRuntimeLibrary.AshP227Batch.kt` marker

## AshP228 — Cleanup junk orchestration + CLI

- `CleanupJunkRunner.cleanup()` ports desktop `CleanupJunkRequest.cleanup()`:
  1. Singleton closet stash — deferred (Phase 217)
  2. COMBINE untinker loop + auto-use box IDs
  3. Pulverize pass (skill 1016, power/NPC-store/antique gates)
  4. Autosell remainder (ronin keep-one, skip meat paste)
- CLI: `cleanup`, `cleanup junk`, `junk` in `GameRuntimeLibrary.cliDispatch`
- DI: `CleanupJunkRunner` + `GameRuntimeLibrary.AshP228Batch.kt` marker

## Tests

- `JunkListManagerTest` — default seed, pref round-trip, unknown skip
- `CleanupJunkRunnerTest` — untinker-before-autosell, pulverize power gate, ronin keep-one
- `GameRuntimeLibraryAshP228Test` — revision pin + `cleanup junk` CLI smoke

## Deferrals (Phase 217+)

- Full `FlaggedItems` parity (`singletonList`, `mementoList`, `profitableList`, `itemflags.txt`)
- `UntinkerRequest.completeQuest()` + Loathing Legion screwdriver + bare `untinker`
- Pulverize auto-add to junkList on ronin smash
- `bastille.txt` manager
- `QuarkCommand` junk-list MP paste
