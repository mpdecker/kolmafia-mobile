# Phase 217: AshP229 FlaggedItems lists + AshP230 cleanup/untinker polish

**Delivered:** 2026-07-29  
**REVISION:** `phase217`  
**Tests:** 3,591

## AshP229 — FlaggedItems singleton + memento lists

- Bundled desktop defaults: `common_singleton.txt` (28 names), `common_memento.txt` (56 names)
- Extended `JunkListManager` with `singletonList`/`mementoList` prefs (pipe-delimited, same pattern as `junkList`)
- Singleton IDs merged into junk list on load (desktop `FlaggedItems.initializeList` behavior)
- New API: `singletonIds()`, `mementoIds()`, `isSingleton()`, `isMemento()`, `addToJunkList(itemId)`
- DI unchanged (class name kept); `GameRuntimeLibrary.AshP229Batch.kt` marker

## AshP230 — Cleanup polish + Pulverize auto-add + bare untinker

- `CleanupJunkRunner`: singleton closet `putIn` before untinker loop; memento skips in pulverize + autosell passes
- `PulverizeRequest`: after successful smash in ronin/HC (`!StoragePullRules.canInteract`), auto-add item to junk list via `JunkListManager.addToJunkList`
- `UntinkerRequest.completeQuestKnoll()` v1: Knoll-sign gate + POST `fv_untinker_quest` + GET `dk_innabox`, then re-probe `canUntinker()`
- Bare `untinker` CLI (no args) in `GameRuntimeLibrary.cliDispatch` → `runUntinkerQuestCli()`
- `CharacterState.knollAvailable` (Mongoose/Wallaby/Vole signs)
- DI: `PulverizeRequest` + `UntinkerRequest` character/junkListManager wiring in `SharedModule`
- `GameRuntimeLibrary.AshP230Batch.kt` marker

## Tests

- `JunkListManagerTest` — singleton/memento seed, singleton→junk merge, `addToJunkList` pref persist
- `CleanupJunkRunnerTest` — singleton closet stash, memento autosell skip
- `PulverizeRequestTest` — ronin smash adds item to junk list
- `GameRuntimeLibraryAshP230Test` — revision pin + bare `untinker` CLI smoke

## Deferrals (Phase 218+)

- `profitableList` + `AutoMallCommand` / QuarkCommand junk-list MP paste
- Full `itemflags.txt` section import/export + `saveFlaggedItemList`
- Untinker full Plains side-trip quest + Loathing Legion screwdriver `inv_use.php?action=screw` path
- `bastille.txt` manager
