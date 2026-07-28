# Phase 174: AshP143 ConcoctionPermitted v3 + AshP144 Coinmaster Validate v9

**Date:** 2026-07-28  
**Revision:** `phase174`

## Summary

- **AshP143:** `TorsoAwareness` helper (TORSO skill 12 or BEST_DRESSED 15022); TIKI → skill 186 gate; explicit always-permit craft methods (ROLL/ROLLING_PIN/SEWER/MUSE/SUSE/MULTI_USE/SINGLE_USE); `METHOD_PRIORITY` MULTI_USE/SINGLE_USE
- **AshP144:** `FiveDPrinterAccessibility` shop (printer item 7750) + `unknownRecipe*` item gates; starchart/sugarsheets torso-aware star/sugar shirt purchase gates

## Key files

- `GameRuntimeLibrary.AshP143Batch.kt`
- `GameRuntimeLibrary.AshP144Batch.kt`
- `TorsoAwareness.kt`
- `FiveDPrinterAccessibility.kt`
- `ConcoctionPermitted.kt` (TIKI/TORSO)
- `ConcoctionMethodGates.kt` (explicit permit methods)
- `ConcoctionCreationCost.kt` (METHOD_PRIORITY)
- `CoinmasterAccessibility.kt` (5dprinter shop gate + accessibleCount param)
- `CoinmasterPurchaseAccessibility.kt` (starchart/sugarsheets/5dprinter items)
- `CoinmasterPurchaseProbe.kt` (pass accessibleCount to shop accessibility)

## Deferred to Phase 175+

- Pixel psychosis-unlock item set (desktop also stubbed)
- `FiveDPrinterRequest.visitShop` recipe-discovery sync
- `skillsRecalled` runtime for Bad Moon CLIPART edge case
- YouRobot/Robocore equipment shop
- Remaining coinmaster long tail (MemeShop, Fixodent, KiwiKwikiMart, etc.)
