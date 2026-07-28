# Phase 170: AshP135 Kitchen Craft Gates + AshP136 NPC Validate v5

**Date:** 2026-07-27  
**Revision:** `phase170`  
**Tests:** 3,083

## Summary

- **AshP135:** `KitchenEquipmentGates` COOK/MIX/COOK_FANCY/MIX_FANCY + `CampgroundItemSync.syncKitchenFromHtml` (hasOven/hasRange/hasShaker/hasCocktailKit/hasChef/hasBartender prefs)
- **AshP136:** `NpcPurchaseAccessibility` v5 (armory/bartender/bartlebys/vault3 + fishing hat/pole, RAT quest tavern gate, pirate ephemera, swashbuckling outfit, fallout level 7)

## Key files

- `GameRuntimeLibrary.AshP135Batch.kt`
- `GameRuntimeLibrary.AshP136Batch.kt`
- `ConcoctionMethodGates.kt` (`KitchenEquipmentGates`)
- `CampgroundItemSync.kt`
- `ConcoctionPermitted.kt`
- `NpcPurchaseAccessibility.kt`

## Deferred to Phase 171+

- Full gift shop ascension table
- Remaining generalstore switch completion
- Bartlebys ephemera HTML sync
- Chef/bartender NPC auto-buy
- STAFF/PHINEAS class/quest craft gates
- Coinmaster validate per-item depth
