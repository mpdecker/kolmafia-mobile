# Phase 168: AshP131 Concoction Method Gates v2 + AshP132 NPC Shop Validate v3

**Date:** 2026-07-27  
**Revision:** `phase168`  
**Tests:** 3,043

## Summary

- **AshP131:** `ConcoctionMethodGates` v2 (TERMINAL/SPACEGATE/FANTASY_REALM/STILLSUIT/MAYAM/PHOTO_BOOTH/TAKERSPACE) + `CampgroundItemSync` + `ClanLoungeSync` photobooth
- **AshP132:** `NpcPurchaseAccessibility` v3 (generalstore/gnoll/gnomart/madeline/mayoclinic/meatsmith/nerve/sandpenny/whitecitadel/doc/hippy depth) + `is_npc_item(id, validate)` overload

## Key files

- `GameRuntimeLibrary.AshP131Batch.kt`
- `GameRuntimeLibrary.AshP132Batch.kt`
- `ConcoctionMethodGates.kt`, `CampgroundItemSync.kt`, `ClanLoungeSync.kt`
- `ConcoctionPermitted.kt`, `NpcPurchaseAccessibility.kt`

## Deferred to Phase 169+

- Remaining concoction methods: `GNOME_PART`, full fallout-shelter terminal detection
- Full generalstore switch (all item branches)
- Gift shop, vault/fallout shelter shops, tweedleporium effect gate
- Complete hippy post-war filthworm outfit detection
- Robocore YouRobot Bird Cage familiar gate
