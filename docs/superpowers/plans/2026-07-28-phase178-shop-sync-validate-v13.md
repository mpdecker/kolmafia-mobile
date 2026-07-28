# Phase 178: AshP151 Shop Visit Sync v3 + AshP152 Validate v13

## Summary

Phase 178 continues the shop sync alternation pattern from Phase 177:

- **AshP151** — swagger visit sync (`peevpee.php?place=shop`), hiddentavern NPC unlock sync (`store.php`), jarl cosmic six-pack purchase pref hook, and visit hook wiring in `processVisitResponseHooks`
- **AshP152** — sync-driven validate v13 probes for hiddentavern NPC item 175, swagger item 7732, and jarl cosmic six-pack item 6237

## AshP151 changes

- `CoinmasterShopSync.applySwaggerVisit` — season item `*Available`/`*Cost` prefs, `currentPVPSeason`, season swagger prefs
- `CoinmasterShopSync.applyPurchasedItem` — jarl item 6237 → `_cosmicSixPackConjured=true`
- `NpcShopSync.syncHiddenTavern` — `hiddenTavernUnlock = ascensionNumber`
- `GameRuntimeLibrary.processVisitResponseHooks` — `store.php` NPC sync + `peevpee.php` swagger sync
- `GameRuntimeLibrary.REVISION` → `phase178`

## AshP152 changes

Validate logic already existed; AshP152 adds sync-driven accuracy verification:

| Probe | Sync fixture |
| --- | --- |
| `is_npc_item(175, true)` hiddentavern | `syncHiddenTavern` with matching ascension |
| `is_coinmaster_item(7732, true)` swagger | swagger visit HTML sets `blackBartsBootyAvailable=true` |
| `is_coinmaster_item(6237, true)` jarl | blocked when `_cosmicSixPackConjured=true` after purchase hook |

## Tests

- `CoinmasterShopSyncTest` — swagger season parse + jarl purchase hook
- `NpcShopSyncTest` — hiddentavern unlock pref + store.php routing
- `CoinmasterPurchaseAccessibilityTest` / `NpcPurchaseAccessibilityTest` — post-sync gate checks
- `GameRuntimeLibraryAshP151Test` / `GameRuntimeLibraryAshP152Test` — revision pin + visit hook smoke tests
- `AshCompatibilityCorpusTest` — hiddentavern/swagger/jarl sync-driven validate snippets

## Deferred to Phase 179+

- CRIMBO05–12 legacy craft methods
- STAR/SUGAR/PIXEL standalone method gates
- Remaining coinmaster visitShop long tail (replica Mr Store, black market, crimbo shops, etc.)
- Full swagger dynamic inventory rebuild (mobile uses static `coinmasters.txt`; season *Available prefs are the critical parity slice)
