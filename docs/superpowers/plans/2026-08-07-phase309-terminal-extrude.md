# Phase 309 — CreateItemRequest Router v3 (applyModifiers v50)

**Date:** 2026-08-07  
**Revision:** `phase309`

## Summary

Continues the CreateItemRequest router track by wiring TERMINAL Source Terminal
extrude (~10 concoctions) into `ConcoctionCreateRequest`, queue drain, and
`create` ASH.

## Deliverables

- `ConcoctionExtensions.kt` — `terminalExtrudeCommand()`, `isTerminalCraftable()`, expanded `isCreateSupported()`
- `TerminalRequest.kt` — campground/fallout terminal visit + choice 1191 POST
- `FalloutShelterRequest.kt` — minimal `place.php` vault_term visit for Nuclear Autumn
- `CampgroundRequest.visitTerminal()` — `campground.php?action=terminal`
- `TerminalExtrudeCreateRequest.kt` — essence retrieve + extrude loop + `_sourceTerminalExtrudes` pref sync
- `ConcoctionCreateRequest.kt` — TERMINAL dispatch
- `SharedModule.kt` — DI wiring

## Deferred (Phase 310+)

- SEWER chewing-gum automation
- VYKEA choice-chain assembly
- SINGLE_USE / MULTI_USE inv_use paths
- Mayo minder autostock during queue drain
- Full FalloutShelter HTML parse / shelter inventory sync
