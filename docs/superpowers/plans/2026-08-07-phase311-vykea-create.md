# Phase 311 — CreateItemRequest Router v5 (VYKEA)

**Date:** 2026-08-07  
**Track:** TCRS applyModifiers v52  
**Revision:** `phase311`

## Summary

Wired VYKEA companion assembly (~100 concoctions) into the CreateItemRequest router:

- `ConcoctionExtensions.isVykeaCraftable()` + expanded `isCreateSupported()`
- `VykeaChoiceMapper` — desktop choice 1120–1123 option table
- `VykeaCreateRequest` — hex key + starter retrieve + instructions use + choice loop (qty cap 1)
- `ConcoctionCreateRequest` VYKEA dispatch + `SharedModule` DI
- Queue drain + `create` ASH pick up VYKEA via `isCreateSupported()` (no runner changes)

## Deferred (Phase 312+)

- MUSE / MULTI_USE generic `multiuse.php` create paths
- SEWER v2 depth
- VYKEA v2: recursive makeIngredients, post-create companion sync, beecore/glover gates

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
```
