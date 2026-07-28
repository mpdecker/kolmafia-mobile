# Phase 167: AshP129 Concoction Method Gates + AshP130 NPC Shop Validate v2

**Date:** 2026-07-27  
**Revision:** `phase167`  
**Tests:** 3,021

## Summary

- **AshP129:** `ConcoctionMethodGates` (FLOUNDRY/BARREL/GNOME_TINKER/VYKEA) + `ClanLoungeSync` + `concoction_price(vykea)` overload
- **AshP130:** `NpcPurchaseAccessibility` v2 (hippy/fratboy/chinatown/cyber/fwshop + crimbo/fdkol stubs)

## Key files

- `GameRuntimeLibrary.AshP129Batch.kt`
- `GameRuntimeLibrary.AshP130Batch.kt`
- `ConcoctionMethodGates.kt`, `ClanLoungeSync.kt`
- `ConcoctionPermitted.kt`, `NpcPurchaseAccessibility.kt`
- `GameRuntimeLibrary.CraftAshHelpers.kt` (shared concoction pricing)

## Deferred to Phase 168+

- Full desktop `recalculatePermittedMethods` long tail (TERMINAL, SPACEGATE, FANTASY_REALM, etc.)
- Complete hippy store post-war outfit/filthworm logic
- Robocore YouRobot Bird Cage familiar gate
- Active Crimbo/FDKOL shop gates when content is live
