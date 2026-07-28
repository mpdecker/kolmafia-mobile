# Phase 172: AshP139 Kitchen Auto-Buy v3 + AshP140 Coinmaster Validate v7

**Date:** 2026-07-28  
**Revision:** `phase172`

## Summary

- **AshP139:** `KitchenAutoBuy` willBuyTool/willBuyServant + `BoxServantAvailability` + `KitchenEquipmentGates` COOK/MIX_FANCY v3; `limitMode` threaded through `ConcoctionMethodGates`/`ConcoctionPermitted`
- **AshP140:** `CoinmasterAccessibility` jarl/swagger shop gates + `CoinmasterPurchaseAccessibility` Jarlsberg skill gates, Swagger season prefs, Crimbo17 class gates + `is_npc_item(id)` 1-arg ASH

## Key files

- `GameRuntimeLibrary.AshP139Batch.kt`
- `GameRuntimeLibrary.AshP140Batch.kt`
- `KitchenAutoBuy.kt`
- `BoxServantAvailability.kt`
- `ConcoctionMethodGates.kt` (`KitchenEquipmentGates` v3)
- `CoinmasterAccessibility.kt`
- `CoinmasterPurchaseAccessibility.kt`
- `CoinmasterPurchaseProbe.kt`

## Deferred to Phase 173+

- Generalstore YouRobot-specific gates (Robocore track)
- Full Crimbo20 + remaining coinmaster long tail
- Remaining craft methods still `else -> true` in `ConcoctionMethodGates`
