# Phase 310 — CreateItemRequest Router v4 (SEWER)

**Date:** 2026-08-07  
**Track:** TCRS applyModifiers v51  
**Revision:** `phase310`

## Summary

Wired SEWER chewing-gum retrieval (~17 concoctions) into the CreateItemRequest router:

- `ConcoctionExtensions.isSewerCraftable()` + expanded `isCreateSupported()`
- `SewerCreateRequest` — gum retrieve, closet goal shuffle, `inv_use` gum loop, worthless-item alias
- `ConcoctionCreateRequest` SEWER dispatch + `SharedModule` DI with `inventoryCountById`
- Queue drain + `create` ASH pick up SEWER via `isCreateSupported()` (no runner changes)

## Deferred (Phase 311+)

- VYKEA choice-chain assembly (~100 concoctions)
- SINGLE_USE / MULTI_USE generic inv_use create paths
- SEWER v2: hermit-scroll worthless path, starter-item probability optimization, mall auto-buy for gum

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
```
