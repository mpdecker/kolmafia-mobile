# Phase 313 — CreateItemRequest Router v7 (PHINEAS)

**Date:** 2026-08-08  
**Track:** TCRS applyModifiers v54  
**Revision:** `phase313`

## Summary

Wired PHINEAS sealhide crafting (~11 concoctions) into the CreateItemRequest router:

- `ConcoctionExtensions.isPhineasCraftable()` + expanded `isCreateSupported()`
- `PhineasCreateRequest` — retrieve all ingredients, POST `volcanoisland.php` (`action=npc`, `subaction=make`, `makewhich`, `quantity=1`) per unit
- `ConcoctionCreateRequest` PHINEAS dispatch + `SharedModule` DI
- Queue drain + `create` ASH pick up PHINEAS via `isCreateSupported()` (no runner changes)

## Deferred (Phase 314+)

- PHINEAS v2 (sub-craft white pixels + visit parse)
- SUSHI (~100 concoctions)
- STAFF (~20)
- GNOME_TINKER / TINKER (~21)
- SINGLE_USE v2 yield/consumption-type/beecore parity

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
```
