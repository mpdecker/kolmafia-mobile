# Phase 179: AshP153 Shop Visit Sync v4 + AshP154 Validate v14

## Summary

Phase 179 continues the shop sync alternation pattern from Phase 178:

- **AshP153** — replica Mr Store year sync (`currentReplicaStoreYear`) and black market MACGUFFIN unlock on visit
- **AshP154** — sync-driven validate v14 probes for mrreplica year gates and black market zeppelin ticket

## AshP153 changes

- `CoinmasterShopSync.apply(html, url, prefs, state)` — optional `CharacterState` for black market wu-tang/nuclear-autumn gates
- `syncReplicaMrStore` — parses `&mdash; <b>YYYY</b> &mdash;` year header
- `syncBlackMarket` — sets `MACGUFFIN` pref to `step1` when black market not yet available
- `GameRuntimeLibrary.processVisitResponseHooks` / `CoinmasterManager.visit` pass character state into `apply()`
- `GameRuntimeLibrary.REVISION` → `phase179`

## AshP154 changes

Validate logic already existed; AshP154 adds sync-driven accuracy verification:

| Probe | Sync fixture |
| --- | --- |
| `is_coinmaster_item(11325, true)` mrreplica | After visit sets `currentReplicaStoreYear=2023` + LoL path |
| `is_coinmaster_item(11190, true)` mrreplica | Blocked when visit sets year 2023 but item is 2004-only |
| `is_coinmaster_item(7185, true)` blackmarket | After visit sync sets MACGUFFIN `step1` from `unstarted` |

## Tests

- `CoinmasterShopSyncTest` — mrreplica year parse + blackmarket MACGUFFIN unlock
- `CoinmasterPurchaseAccessibilityTest` — replica wrong-year blocked after sync
- `GameRuntimeLibraryAshP153Test` / `GameRuntimeLibraryAshP154Test`
- `AshCompatibilityCorpusTest` — mrreplica year + blackmarket zeppelin sync-driven snippets

## Deferred to Phase 180+

- CRIMBO05–12 legacy craft methods
- STAR/SUGAR/PIXEL standalone method gates
- Remaining coinmaster visitShop long tail (crimbo shops, game shoppe, etc.)
- Full swagger dynamic inventory rebuild
