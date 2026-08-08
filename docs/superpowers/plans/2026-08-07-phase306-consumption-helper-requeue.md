# Phase 306 — Consumption-Helper Re-queue (applyModifiers v47)

**Date:** 2026-08-07  
**Revision:** `phase306`

## Summary

Closes the consumption-helper deferral from Phases 302–305 by porting desktop
`ConcoctionDatabase.handleQueue` failure re-queue behavior: helper snapshot/restore,
partial `lastUnconsumed` quantity restore, and eat/drink request depth for helpers
and multi-iterate consumption.

## Deliverables

- `ConsumptionHelperState.kt` — food/drink helper slots, consumed counters, utensil ids
- `ConsumptionRequestOutcome.kt` — structured eat/drink outcomes (`Completed`/`Aborted`)
- `EatFoodRequest.kt` / `DrinkBoozeRequest.kt` — abort parsing, utensil param, multi-iterate, helper queue
- `ConcoctionQueueRunner.kt` — FOOD_HELPER/DRINK_HELPER routing, helper-first + partial re-queue

## Deferred (Phase 307+)

- Full `allowFoodConsumption` / elemental helper validation depth
- Mayo minder autostock during queue drain
- Full `CreateItemRequest` subclass tree
- Queue push CLI/ASH (desktop UI-only)
