# Phase 312 — CreateItemRequest Router v6 (MUSE)

**Date:** 2026-08-08  
**Track:** TCRS applyModifiers v53  
**Revision:** `phase312`

## Summary

Wired MUSE multi-use create (~100 concoctions) into the CreateItemRequest router:

- `ConcoctionExtensions.isMuseCraftable()` + expanded `isCreateSupported()`
- `UseItemRequest.multiUse()` — desktop `MultiUseRequest` multiuse.php POST
- `MuseCreateRequest` — retrieve all ingredients, use first ingredient via inv_use/multiuse
- `ConcoctionCreateRequest` MUSE dispatch + `SharedModule` DI
- Queue drain + `create` ASH pick up MUSE via `isCreateSupported()` (no runner changes)

## Deferred (Phase 313+)

- SINGLE_USE generic create paths
- Specialty create handlers (GNOME_TINKER, PHINEAS, etc.)
- SEWER v2 / MUSE v2 (beecore/glover gates, sub-craft)

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
```
