# Phase 169: AshP133 Concoction Method Gates v3 + AshP134 NPC Shop Validate v4

**Date:** 2026-07-27  
**Revision:** `phase169`  
**Tests:** 3,063

## Summary

- **AshP133:** `ConcoctionMethodGates` v3 (GNOME_PART/BURNING_LEAVES/WAX/NEWSPAPER/METEOROID/WOOL + fallout TERMINAL) + `CampgroundItemSync` burning leaves/source terminal + `FamiliarUsability` craft callback
- **AshP134:** `NpcPurchaseAccessibility` v4 (gift shop/unclep/tweedle/vault/generalstore trick-tot + effect/familiar validate hooks)

## Key files

- `GameRuntimeLibrary.AshP133Batch.kt`
- `GameRuntimeLibrary.AshP134Batch.kt`
- `ConcoctionMethodGates.kt`, `CampgroundItemSync.kt`
- `ConcoctionPermitted.kt`, `ConcoctionCreationCost.kt`, `GameRuntimeLibrary.CraftAshHelpers.kt`
- `NpcPurchaseAccessibility.kt`, `NpcStoreDatabase.kt`, `GameRuntimeLibrary.AshP132Batch.kt`, `GameRuntimeLibrary.AshP127Batch.kt`

## Deferred to Phase 170+

- Full gift shop ascension table (all ~30 item branches)
- Full generalstore switch completion
- Robocore YouRobot Bird Cage familiar gate
- Complete hippy post-war filthworm detection
- Coinmaster validate depth beyond probe wiring
- Remaining concoction long tail beyond GNOME_PART/BURNING_LEAVES
