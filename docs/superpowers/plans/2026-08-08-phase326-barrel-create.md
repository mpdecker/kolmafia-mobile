# Phase 326 — BarrelCreateRequest (CreateItemRequest v13)

**Track:** TCRS `applyModifiers` v67  
**Revision:** `phase326`

## Goal

Wire BARREL shrine concoctions (barrel lid / barrel hoop earring / bankruptcy barrel) into the CreateItemRequest router via `da.php?barrelshrine=1` + choice 1100, with daily prayer pref gates.

## Scope

- `BarrelChoiceMapper` — option mapping + `availableBarrelItem` + success pref write-back
- `BarrelCreateRequest` — shrine visit + choice 1100 + quantity cap 1
- `isBarrelCraftable()` + `isCreateSupported()` extension
- `ConcoctionCreateRequest` v13 router branch
- DI + tests + docs/revision bump

## Delivered

- `BarrelChoiceMapper` — choice 1100 option 1–3 mapping + `_barrelPrayer`/`prayedFor*` gates
- `BarrelCreateRequest` — `da.php?barrelshrine=1` visit + choice 1100 POST + KoE block + pref write-back on success
- `ConcoctionData.isBarrelCraftable()` + `isCreateSupported()` extension
- `ConcoctionCreateRequest` v13 router branch (no `makeIngredients` — zero-ingredient recipes)
- `SharedModule` DI wiring
- `BarrelCreateRequestTest` + `ConcoctionCreateRequestTest.create_routesBarrelToShrine`

## Deferred (Phase 327+)

- Choice 1100 visit-hook pref sync (`ChoiceControl` unavailable reward options)
- `barrelprayer` CLI (desktop option 4 buff path)
- `guild.php` visit-hook sync for manual `malussmash`
- `DreadScrollManager.decorate()` / choice 703 quest sync
