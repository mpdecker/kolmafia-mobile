# Phase 166: AshP127 Shop Validate Probes + AshP128 ConcoctionPermitted Depth

**Date:** 2026-07-27  
**Revision:** `phase166`  
**Tests:** 2,999

## Summary

- **AshP127:** `NpcPurchaseAccessibility` + `CoinmasterPurchaseProbe`; `NpcStoreDatabase`/`CoinmasterDatabase` validate=true; `ItemAvailability` depth; `npc_item_accessible`/`coinmaster_item_accessible` ASH helpers
- **AshP128:** `ConcoctionPermitted` COINMASTER + STILL/SUSHI/MALUS/SAUSAGE_O_MATIC gates; `CharacterCapacity` + `can_expand_stomach`/`can_expand_liver`/`is_craft_permitted` ASH

## Key files

- `GameRuntimeLibrary.AshP127Batch.kt`
- `GameRuntimeLibrary.AshP128Batch.kt`
- `NpcPurchaseAccessibility.kt`, `CoinmasterPurchaseProbe.kt`
- `CharacterCapacity.kt`
- `ConcoctionPermitted.kt`, `ItemAvailability.kt`, `NpcStoreDatabase.kt`, `CoinmasterDatabase.kt`

## Deferred to Phase 167+

- VYKEA `concoction_price` overload
- Full desktop `ConcoctionDatabase.recalculatePermittedMethods`
- Complete NPC shop gate port (crimbo/cyber/fdkol long tail)
- Robocore YouRobot Bird Cage familiar gate
