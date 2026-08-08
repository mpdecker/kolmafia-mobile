# Phase 325 — JewelCreateRequest (CreateItemRequest v12)

**Track:** TCRS `applyModifiers` v66  
**Revision:** `phase325`

## Goal

Wire JEWEL/EJEWEL jewelry concoctions into the CreateItemRequest router via `craft.php` mode=combine (desktop `CraftingType.JEWELRY`), with `ConcoctionPermitted`/`CreateItemIngredients` preflight.

## Scope

- `JewelCreateRequest` — `craft.php` combine mode, 2-ingredient recipes
- `isJewelCraftable()` + `isCreateSupported()` extension
- `ConcoctionCreateRequest` v12 router branch
- `ConcoctionMeatPasteNeeded` JEWEL/EJEWEL paste parity
- DI + tests + docs/revision bump

## Delivered

- `JewelCreateRequest` — `craft.php` mode=combine for JEWEL/EJEWEL 2-ingredient recipes
- `ConcoctionData.isJewelCraftable()` + `isCreateSupported()` extension
- `ConcoctionCreateRequest` v12 router branch
- `ConcoctionMeatPasteNeeded` JEWEL/EJEWEL paste parity
- `SharedModule` DI with inventory-backed `accessibleCount` for pliers gate
- `JewelCreateRequestTest` + `ConcoctionCreateRequestTest.create_routesJewelToCraft`

## Deferred (Phase 326+)

- `BarrelCreateRequest` (BARREL shrine choice chain)
- `guild.php` visit-hook sync for manual `malussmash`
- `DreadScrollManager.decorate()` / choice 703 quest sync
- SINGLE_USE registry (not in bundled concoctions.txt)
