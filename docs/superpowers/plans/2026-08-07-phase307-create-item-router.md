# Phase 307 — CreateItemRequest Router v1 (applyModifiers v48)

**Date:** 2026-08-07  
**Revision:** `phase307`

## Summary

Closes the next TCRS applyModifiers deferral by expanding craft creation beyond
station/SUSE auto-craft: a method router in `ConcoctionCreateRequest` now handles
STILL cocktail-shop buys and COINMASTER purchases, wired into queue drain and
`create` ASH.

## Deliverables

- `ConcoctionExtensions.kt` — `stillShopRow()`, `isStillCraftable()`,
  `isCoinmasterCraftable()`, `isCreateSupported()`
- `StillCreateRequest.kt` — ingredient retrieve + `ShopRequest.buy("still", row)`
  + `StillSync` still-count parse
- `ConcoctionCreateRequest.kt` — method router (SUSE/station/STILL/COINMASTER)
- `ConcoctionQueueRunner.kt` — `isCreateSupported()` guard for craft-only drain
- `GameRuntimeLibrary.ConcoctionQueue.kt` — `create` ASH uses expanded router
- `SharedModule.kt` — DI for `ShopRequest` + `CoinmasterManager`

## Deferred (Phase 308+)

- Remaining CreateItemRequest subclasses: CLIPART, TERMINAL, ROLLING_PIN, VYKEA,
  SINGLE_USE/MULTI_USE, SEWER, etc.
- Mayo minder autostock during queue drain
- Elemental helper Coldform/Hotform validation depth
- Queue push CLI/ASH (desktop UI-only)
