# Phase 308 — CreateItemRequest Router v2 (applyModifiers v49)

**Date:** 2026-08-07  
**Revision:** `phase308`

## Summary

Continues the CreateItemRequest router track by wiring CLIPART campground
summon and ROLL rolling-pin dough conversion into `ConcoctionCreateRequest`,
queue drain, and `create` ASH.

## Deliverables

- `ConcoctionData.param` + `ConcoctionDatabase` numeric-field parser for CLIPART rows
- `ConcoctionExtensions.kt` — `clipArtParams()`, `isClipArtCraftable()`, `isRollCraftable()`, expanded `isCreateSupported()`
- `ClipArtCreateRequest.kt` — `campground.php` combinecliparts + summon pref sync
- `RollingPinCreateRequest.kt` — DOUGH_DATA table + inv_use pin + ingredient retrieve
- `ConcoctionCreateRequest.kt` — CLIPART/ROLL dispatch
- `SharedModule.kt` — DI wiring

## Deferred (Phase 309+)

- TERMINAL extrude (`TerminalRequest`)
- SEWER chewing-gum automation
- VYKEA choice-chain assembly
- SINGLE_USE / MULTI_USE inv_use paths
- Mayo minder autostock during queue drain
- Mall auto-buy in rolling-pin create
