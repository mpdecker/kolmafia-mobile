# Phase 319 — CreateItemRequest v3 (makeIngredients v2 + meat-paste preflight)

**Track:** TCRS `applyModifiers` v60  
**Revision:** `phase319`

## Summary

Closes Phase 317–318 deferrals on the specialty create path: desktop meat-paste COMBINE/JEWELRY preflight in `CreateItemIngredients`, plus Muse/Vykea/Terminal/Sewer handler wiring through the shared two-pass `makeIngredients()` helper.

## Delivered

- `ConcoctionMeatPasteNeeded` — `needsPaste` + recursive `getMeatPasteNeeded` (COMBINE/ACOMBINE/JEWELRY; knoll/zombie gate)
- `CreateItemIngredients.makeIngredients` — optional `state`/`initialCount`; meat paste id 25 preflight retrieve before two-pass ingredient loops
- `MuseCreateRequest` / `TerminalExtrudeCreateRequest` / `SewerCreateRequest` / `VykeaCreateRequest` — inject `CreateItemIngredients`; concoction ingredient loop replaced with `makeIngredients(concoction, 1, state)` (Vykea keeps hex key + 5× plank/rail/bracket retrieves)
- TINKER/STAFF/PHINEAS/SUSHI handlers — pass live `state` into `makeIngredients`
- `SharedModule` DI wiring for all specialty handlers
- Tests: `ConcoctionMeatPasteNeededTest`, extended `CreateItemIngredientsTest`, handler/queue/corpus constructor updates

## Deferred Phase 320+

- `ConcoctionCreateRequest` station/SUSE auto-craft path (`craftAtStation` / `craftSuse`) through `makeIngredients`
- Full `DreadScrollManager`
- Manual browser `sushi.php` visit hook
- lazy-load class/sign TCRS files
