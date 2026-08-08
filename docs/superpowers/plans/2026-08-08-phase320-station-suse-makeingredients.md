# Phase 320 — CreateItemRequest v4 (station/SUSE makeIngredients)

**Track:** TCRS `applyModifiers` v61  
**Revision:** `phase320`

## Summary

Closes the Phase 319 deferral: desktop `CreateItemRequest.run()` always calls `makeIngredients()` before station combine/cook/mix/smith HTTP and SUSE single-use auto-craft. Mobile now routes COMBINE/COOK/MIX/SMITH station crafts, SUSE crafts, STILL shop buys, and `RetrieveItemService.craftMissing` station/SUSE auto-craft through the shared `CreateItemIngredients.makeIngredients()` helper (including meat-paste preflight and recursive sub-craft).

## Delivered

- `ConcoctionCreateRequest` — inject `CreateItemIngredients`; `createSuse`/`createStation` call `makeIngredients(concoction, qty, state)` before `use`/`craft`
- `RetrieveItemService` — lazy `createItemIngredientsProvider`; `craftSuse`/`craftAtStation` use `makeIngredients` with `character.state`
- `StillCreateRequest` — inject `CreateItemIngredients`; single `makeIngredients` before still shop buy
- `SharedModule` DI — `ConcoctionCreateRequest`, `RetrieveItemService`, `CreateItemIngredients` wiring
- Tests: extended `ConcoctionCreateRequestTest`, `RetrieveItemServiceTest`, `StillCreateRequestTest`, queue/corpus constructor updates

## Deferred Phase 321+

- Full `DreadScrollManager`
- Manual browser `sushi.php` visit hook
- lazy-load class/sign TCRS files
- `RollingPinCreateRequest` flat retrieve (ROLL method)
