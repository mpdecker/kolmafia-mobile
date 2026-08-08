# Phase 316 — CreateItemRequest Router v10 (SUSHI)

**Date:** 2026-08-08  
**Track:** TCRS applyModifiers v57  
**Revision:** `phase316`

## Summary

Wired all 135 SUSHI concoctions into the CreateItemRequest router:

- `SushiChoiceMapper` — desktop name→form field mapping (whichsushi/topping/filling/veggie/dipping)
- `ConcoctionExtensions.isSushiCraftable()` / `isCreateAndConsume()` + expanded `isCreateSupported()`
- `SushiCreateRequest` — retrieve ingredients, POST `sushi.php` create-and-consume per unit
- `ConcoctionCreateRequest` SUSHI dispatch + `SharedModule` DI
- `ConcoctionQueueRunner` skips post-create eat for create-and-consume SUSHI

## Deferred (Phase 317+)

- SUSHI v2 (sub-craft missing ingredients + visit parse)
- STAFF/PHINEAS/TINKER v2 sub-craft polish
- Fancy doily / worktea / fullness sync from response

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
```
