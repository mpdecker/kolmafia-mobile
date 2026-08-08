# Phase 305 — Live Floundry HTML Parse (applyModifiers v46)

## Goal

Port desktop `ClanLoungeRequest.parseFloundry()` so mobile tracks live fish stock, virtual FLOUNDRY concoction counts, per-item availability gates, lounge visit hook, and `get_fishing_locations` ASH.

## Deliverables

- `FloundryAvailability` + extended `FloundryDatabase` fish→item mapping
- `ClanLoungeSync.syncFloundryFromHtml` + location pref parse + `apply()` hooks
- `ClanLoungeRequest.visitFloundry`
- `ConcoctionDatabase` virtual FLOUNDRY concoctions + `applyFloundryRefreshTail`
- `ConcoctionMethodGates` per-item availability + `CreatableAmount` FLOUNDRY branch
- AshP305 `get_fishing_locations`

## Deferred (Phase 306+)

- Consumption-helper re-queue + partial `lastUnconsumed` re-queue on eat/drink failure
