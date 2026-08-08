# Phase 322 — RollingPin makeDough depth

**Track:** TCRS `applyModifiers` v63  
**Revision:** `phase322`

## Summary

Ports desktop `CreateItemRequest.makeDough()` into mobile `RollingPinCreateRequest`: accessible-count retrieve, NPC wad-of-dough purchase fallback (avoiding flat-dough retrieve recursion), wad-output early exit, tool-vs-hand batch semantics, and the >10-without-tool guard.

## Delivered

- `RollingPinCreateRequest.makeDoughBatch()` — desktop-shaped flow with outer quantity loop
- `buyWadsFromNpc()` via `NpcBuyRequest` + `NpcStoreDatabase`/`gameDatabase.npcStoreFor("wad of dough")`
- `accessibleCount()` via `AccessibleItemCount.physicalCount` + optional test override lambda
- DI: `NpcBuyRequest`, `Preferences`, `InventoryManager`, `KoLCharacter` in `SharedModule`
- Tests: NPC buy path, wad early exit, >10 without pin failure, existing pin/unroll tests preserved

## Deferred Phase 323+

- Full `DreadScrollManager`
- lazy-load class/sign TCRS files (document as non-goal)
