# Phase 314 — CreateItemRequest Router v8 (STAFF)

**Date:** 2026-08-08  
**Track:** TCRS applyModifiers v55  
**Revision:** `phase314`

## Summary

Wired STAFF chefstaff crafting (20 concoctions) into the CreateItemRequest router:

- `ConcoctionExtensions.isStaffCraftable()` + expanded `isCreateSupported()`
- `StaffCreateRequest` — retrieve all ingredients (including base staff), POST `guild.php` (`action=makestaff`, `whichstaff=<baseId>`) per unit
- `ConcoctionCreateRequest` STAFF dispatch + `SharedModule` DI
- Queue drain + `create` ASH pick up STAFF via `isCreateSupported()` (no runner changes)

## Deferred (Phase 315+)

- STAFF v2 (sub-craft missing ingredients + guild visit parse)
- SUSHI (135 concoctions)
- TINKER / GNOME_TINKER (21)
- PHINEAS v2
- SINGLE_USE v2

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
```
