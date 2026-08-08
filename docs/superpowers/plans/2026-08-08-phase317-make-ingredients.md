# Phase 317 — CreateItemRequest v2 (makeIngredients v1)

**Date:** 2026-08-08  
**Track:** TCRS applyModifiers v58  
**Revision:** `phase317`

## Summary

Ported desktop `CreateItemRequest.makeIngredients()` v1:

- `CreateItemIngredients` — two-pass retrieve, multiplier/yield math, creatable sort
- `RetrieveItemService.craftMissing` specialty sub-craft via lazy `ConcoctionCreateRequest` provider
- TINKER/STAFF/PHINEAS/SUSHI handlers wired to `makeIngredients()` instead of flat retrieve loops

## Deferred (Phase 318+)

- SUSHI v2 visit parse / fullness pref sync
- Fancy doily / worktea response handling
- Meat-paste preflight for COMBINE/JEWELRY in `makeIngredients`
- Muse/Vykea/Terminal/Sewer makeIngredients wiring

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
```
