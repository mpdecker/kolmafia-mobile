# Phase 173: AshP141 Craft Gates v4 + AshP142 Coinmaster Validate v8

**Date:** 2026-07-28  
**Revision:** `phase173`

## Summary

- **AshP141:** `SmithingGates`/`KnollAvailability` SMITH/SSMITH + CLIPART/JEWELRY method gates; `ConcoctionCreationCost` CLIPART priority; `ConcoctionPermitted` hammer via smithing gates
- **AshP142:** `DesertBeachAccessibility`/`ReplicaMrStoreAccessibility` + Crimbo20/shore/replica/pixel coinmaster accessibility and per-item purchase gates

## Key files

- `GameRuntimeLibrary.AshP141Batch.kt`
- `GameRuntimeLibrary.AshP142Batch.kt`
- `SmithingGates.kt`
- `KnollAvailability.kt`
- `ConcoctionMethodGates.kt` (SMITH/SSMITH/CLIPART/JEWEL/JEWELRY)
- `ConcoctionPermitted.kt`
- `ConcoctionCreationCost.kt` (CLIPART/JEWELRY/ROLLING_PIN/SAUSAGE_O_MATIC priority)
- `DesertBeachAccessibility.kt`
- `ReplicaMrStoreAccessibility.kt`
- `CoinmasterAccessibility.kt`
- `CoinmasterPurchaseAccessibility.kt`

## Deferred to Phase 174+

- Generalstore YouRobot-specific gates (Robocore track)
- Star Chart / Sugar Sheet / FiveD Printer coinmaster item rules
- Full Pixel psychosis-unlock item set
- `skillsRecalled` runtime sync for Bad Moon CLIPART edge case
