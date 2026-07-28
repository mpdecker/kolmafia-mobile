# Phase 171: AshP137 Craft Gates v4 + AshP138 NPC/Coinmaster Validate v6

**Date:** 2026-07-27  
**Revision:** `phase171`  
**Tests:** (see jvmTest run)

## Summary

- **AshP137:** STAFF (mysticality + guild open) + PHINEAS (sledgehammer 4316) + kitchen fancy v2 (`FreeCraftingTurns` + Cocktail Magic 15008)
- **AshP138:** `NpcShopSync` (bartlebys ephemera + hippy filth clearance) + white Citadel gate fix + `CoinmasterPurchaseAccessibility` + `is_coinmaster_item(id[,validate])`

## Key files

- `GameRuntimeLibrary.AshP137Batch.kt`
- `GameRuntimeLibrary.AshP138Batch.kt`
- `ConcoctionMethodGates.kt` (STAFF/PHINEAS/KitchenEquipmentGates v2)
- `NpcShopSync.kt`
- `NpcBuyRequest.kt` (`visitStore`)
- `NpcPurchaseAccessibility.kt` (white Citadel)
- `CoinmasterPurchaseAccessibility.kt`
- `CoinmasterPurchaseProbe.kt`

## Deferred to Phase 172+

- Chef/bartender NPC auto-buy
- Full coinmaster per-item long tail
- Generalstore YouRobot-specific gates
