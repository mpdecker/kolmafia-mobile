# Phase 315 — CreateItemRequest Router v9 (TINKER)

**Date:** 2026-08-08  
**Track:** TCRS applyModifiers v56  
**Revision:** `phase315`

## Summary

Wired TINKER supertinkering (21 clockwork concoctions) into the CreateItemRequest router:

- `ConcoctionExtensions.isTinkerCraftable()` + expanded `isCreateSupported()`
- `GnomeTinkerCreateRequest` — retrieve all 3 ingredients, POST `gnomes.php` (`place=tinker`, `action=tinksomething`, `item1`/`item2`/`item3`, `qty=1`) per unit
- `ConcoctionCreateRequest` TINKER dispatch + `SharedModule` DI
- Queue drain + `create` ASH pick up TINKER via `isCreateSupported()` (no runner changes)

## Deferred (Phase 316+)

- TINKER v2 (sub-craft missing ingredients + visit parse)
- SUSHI (135 concoctions)
- STAFF v2 / PHINEAS v2
- SINGLE_USE v2

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
```
