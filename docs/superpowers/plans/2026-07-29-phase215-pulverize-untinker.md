# Phase 215: AshP225 Pulverize retrieve polish + AshP226 Untinker HTTP/CLI

**Delivered:** 2026-07-29  
**Revision:** `phase215`  
**Tests:** 3,576

## AshP225 — Pulverize retrieve polish

- `PulverizeRequest` now retrieves `TENDER_HAMMER` (338) and the smash target via `RetrieveItemService` before POSTing `craft.php?action=pulverize`.
- Returns `Result.success(0)` when hammer or item is still unavailable after retrieve (desktop parity).
- DI: `PulverizeRequest(client, inventory, retrieveItemService)`.
- Marker batch: `GameRuntimeLibrary.AshP225Batch.kt`.

## AshP226 — Untinker HTTP + CLI

- New `UntinkerRequest.kt`: POST `place.php` with `whichplace=forestvillage`, `action=fv_untinker`, `preaction=untinker`, `whichitem`, optional `untinkerall=on`.
- Eligibility: `ConcoctionDatabase.getByResult` with `isCombining` or `JEWELRY` method.
- `canUntinker()` GET probe with session cache; `parseResponse()` handles acquire text and `untinkerall=on` full-inventory consumption.
- CLI: `untinker item[, item]...` in `cliDispatch` via `runUntinkerCli`.
- DI: `UntinkerRequest(client, inventory, retrieveItemService, gameDatabase)`.
- Marker batch: `GameRuntimeLibrary.AshP226Batch.kt`.

## Tests

- Extended `PulverizeRequestTest` (retrieve path + hammer-missing graceful zero).
- New `UntinkerRequestTest` (eligibility, parseResponse, probe cache, HTTP + retrieve).
- New `GameRuntimeLibraryAshP226Test` (revision pin + CLI integration).
- Revision pins bulk-updated to `phase215`.

## Explicit deferrals (Phase 216+)

- `UntinkerRequest.completeQuest()` side-trip automation.
- Loathing Legion universal screwdriver path.
- Bare `untinker` with no args (quest completion).
- `JunkList` / `cleanup junk` CLI orchestration.
