# Phase 164: AshP123 Shop Probes + AshP124 Craft/Consumption Depth ASH

**Date:** 2026-07-27  
**Revision:** `phase164`  
**Tests:** 2,963

## Summary

- **AshP123:** `is_npc_item`, `is_coinmaster_item`, `concoction_price`
- **AshP124:** `ConcoctionPermitted` gating for `get_ingredients`/`creatable_amount`, `creatable_turns` (1-arg + 2-arg v1), path-base modifier-effective `fullness_limit`/`inebriety_limit`

## Key files

- `GameRuntimeLibrary.AshP123Batch.kt`
- `GameRuntimeLibrary.AshP124Batch.kt`
- `ConcoctionPermitted.kt`, `ConcoctionCreationCost.kt`, `CreatableTurns.kt`
- `NpcStoreDatabase.containsItem`, `CoinmasterDatabase.containsBuyItem`
- `ConsumptionEligibility` + `AscensionPath` capacity fields

## Deferred to Phase 165+

- `creatable_turns(item, count, freeCrafting)` + recursive ingredient adventure tree
- Shop/coinmaster `validate=true` accessibility probes
- Full `CraftingRequirements` (holiday, grimacite, coinmaster method)
- Robocore Bird Cage familiar gate
- VYKEA `concoction_price` overload
