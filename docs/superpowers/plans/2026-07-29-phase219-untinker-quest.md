# Phase 219: AshP233 untinker quest side-trip + AshP234 LL screwdriver untinker

**Delivered:** 2026-07-29  
**Revision:** `phase219`  
**Tests:** 3,605

## AshP233 — completeQuest + Plains side-trip

- Extended `UntinkerRequest.completeQuest()` — POST screwquest, Knoll Innabox when `knollAvailable`, else Degrassi Knoll Garage (snarfblat=354) side-trip via `GoalManager.runSideTripForItem`
- Added `GoalManager.captureSnapshot`/`restoreSnapshot` + `runSideTripForItem` (desktop `makeSideTrip` parity)
- Quest sync via `UntinkerRequest.syncQuestFromResponse` → `Quest.UNTINKER` started/finished
- `untinker()` auto-retries after `completeQuest()` when probe lacks `<select>`
- CLI `untinker` (no args) now calls `completeQuest()` instead of `completeQuestKnoll()` only
- DI: `UntinkerRequest` injects `AdventureManager`, `GoalManager`, `QuestDatabase`

## AshP234 — Loathing Legion universal screwdriver

- `UntinkerRequest.untinkerViaLegionScrewdriver` — `inv_use.php?ajax=1&whichitem=4926&action=screw&dowhichitem=…`
- `runUntinkerCli` routes to legion path when item 4926 is physically accessible (`AccessibleItemCount`)

## Tests

- `UntinkerRequestTest` — knoll quest, side-trip helper, LL screwdriver HTTP, auto-retry, quest sync
- `GameRuntimeLibraryAshP234Test` — revision pin, `cli_execute("untinker")`, legion routing

## Deferred (Phase 220+)

- `bastille.txt` / `BastilleBattalionManager`
- Desktop `managestore.php` vs mobile `backoffice.php` live audit
- Untinker HTML decorate links (UI-only)
